from fastapi import FastAPI, HTTPException, Header
from pydantic import BaseModel
from typing import List, Dict, Any
import sqlite3
import os
import pika
import json
import httpx

app = FastAPI(title="Payment Service")

AUTH_DB_PATH = os.getenv("AUTH_DB_PATH", "db/app.db")
INVENTORY_DB_PATH = os.getenv("INVENTORY_DB_PATH", "inventory_service/inventory.db")
RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
CART_URL = os.getenv("CART_URL", "http://localhost:8004")

class CartItem(BaseModel):
    item: str
    quantity: int

class CheckoutRequest(BaseModel):
    cart_items: List[CartItem]

def get_auth_db():
    conn = sqlite3.connect(AUTH_DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def get_inventory_db():
    conn = sqlite3.connect(INVENTORY_DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

@app.get("/payment/balance")
def get_balance(x_user_id: str = Header(None)):
    if not x_user_id:
        raise HTTPException(status_code=401, detail="Missing user_id")
        
    conn = get_auth_db()
    cursor = conn.cursor()
    cursor.execute("SELECT wallet_balance FROM users WHERE id = ?", (x_user_id,))
    row = cursor.fetchone()
    conn.close()
    
    if not row:
        raise HTTPException(status_code=404, detail="User not found")
        
    return {"balance": row['wallet_balance']}

@app.post("/payment/checkout")
async def checkout(req: CheckoutRequest, x_user_id: str = Header(None)):
    if not x_user_id:
        raise HTTPException(status_code=401, detail="Missing user_id")
        
    inv_conn = get_inventory_db()
    inv_cursor = inv_conn.cursor()
    
    auth_conn = get_auth_db()
    auth_cursor = auth_conn.cursor()
    
    reserved_items = []
    total_cost = 0.0
    
    try:
        # STEP 1: Atomic inventory reservation
        for cart_item in req.cart_items:
            # We need to get the price to calculate total cost
            inv_cursor.execute("SELECT price FROM inventory WHERE item = ?", (cart_item.item,))
            row = inv_cursor.fetchone()
            if not row:
                raise Exception(f"Item {cart_item.item} not found")
                
            price = row['price']
            total_cost += price * cart_item.quantity
            
            inv_cursor.execute('''
                UPDATE inventory 
                SET reserved = reserved + ? 
                WHERE item = ? AND (stock - reserved) >= ?
            ''', (cart_item.quantity, cart_item.item, cart_item.quantity))
            
            if inv_cursor.rowcount == 0:
                raise Exception(f"Item {cart_item.item} is out of stock")
                
            reserved_items.append(cart_item)
            
        # STEP 2: Atomic wallet deduction
        auth_cursor.execute('''
            UPDATE users 
            SET wallet_balance = wallet_balance - ? 
            WHERE id = ? AND wallet_balance >= ?
        ''', (total_cost, x_user_id, total_cost))
        
        if auth_cursor.rowcount == 0:
            raise Exception("Insufficient wallet balance")
            
        # STEP 3: Both succeeded - commit everything
        # Actually commit the wallet deduction
        auth_conn.commit()
        
        # Now we execute the final update on inventory:
        for cart_item in req.cart_items:
            inv_cursor.execute('''
                UPDATE inventory 
                SET stock = stock - ?, reserved = reserved - ? 
                WHERE item = ?
            ''', (cart_item.quantity, cart_item.quantity, cart_item.item))
        inv_conn.commit()
        
        # Publish order.paid to RabbitMQ
        connection = pika.BlockingConnection(pika.ConnectionParameters(host=RABBITMQ_HOST))
        channel = connection.channel()
        channel.exchange_declare(exchange='orders', exchange_type='fanout')
        
        for cart_item in req.cart_items:
            event = json.dumps({"item": cart_item.item, "quantity": cart_item.quantity, "user_id": x_user_id})
            channel.basic_publish(exchange='orders', routing_key='', body=event)
            
        connection.close()
        
        # Call cart-service DELETE /cart/clear
        async with httpx.AsyncClient() as client:
            await client.delete(f"{CART_URL}/cart/clear", headers={"x-user-id": x_user_id})
            
        # Fetch new balance
        auth_cursor.execute("SELECT wallet_balance FROM users WHERE id = ?", (x_user_id,))
        new_balance = auth_cursor.fetchone()['wallet_balance']
        
        return {"success": True, "new_balance": new_balance}
        
    except Exception as e:
        # Rollback wallet
        auth_conn.rollback()
        # Rollback inventory: release reservations
        for cart_item in reserved_items:
            inv_cursor.execute('''
                UPDATE inventory 
                SET reserved = reserved - ? 
                WHERE item = ?
            ''', (cart_item.quantity, cart_item.item))
        inv_conn.commit()
        
        return {"success": False, "error": str(e)}
        
    finally:
        inv_conn.close()
        auth_conn.close()

@app.get("/health")
def health():
    return {"status": "ok"}
