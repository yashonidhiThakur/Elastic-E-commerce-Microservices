from fastapi import FastAPI, HTTPException, Header
from pydantic import BaseModel
import redis
import json
import os
import httpx

app = FastAPI(title="Cart Service")

REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")
INVENTORY_URL = os.getenv("INVENTORY_URL", "http://localhost:8002")

redis_client = redis.from_url(REDIS_URL, decode_responses=True)

class AddItemRequest(BaseModel):
    item: str
    quantity: int

class RemoveItemRequest(BaseModel):
    item: str

def get_cart(user_id: str):
    cart_data = redis_client.get(f"cart:{user_id}")
    if not cart_data:
        return {}
    return json.loads(cart_data)

def save_cart(user_id: str, cart: dict):
    redis_client.set(f"cart:{user_id}", json.dumps(cart))

@app.post("/cart/add")
def add_item(req: AddItemRequest, x_user_id: str = Header(None)):
    if not x_user_id:
        raise HTTPException(status_code=401, detail="Missing user_id")
    
    cart = get_cart(x_user_id)
    if req.item in cart:
        cart[req.item] += req.quantity
    else:
        cart[req.item] = req.quantity
        
    save_cart(x_user_id, cart)
    return {"success": True, "cart": cart}

@app.post("/cart/remove")
def remove_item(req: RemoveItemRequest, x_user_id: str = Header(None)):
    if not x_user_id:
        raise HTTPException(status_code=401, detail="Missing user_id")
        
    cart = get_cart(x_user_id)
    if req.item in cart:
        del cart[req.item]
        save_cart(x_user_id, cart)
        
    return {"success": True, "cart": cart}

@app.delete("/cart/clear")
def clear_cart(x_user_id: str = Header(None)):
    if not x_user_id:
        raise HTTPException(status_code=401, detail="Missing user_id")
        
    redis_client.delete(f"cart:{x_user_id}")
    return {"success": True}

@app.get("/cart")
async def view_cart(x_user_id: str = Header(None)):
    if not x_user_id:
        raise HTTPException(status_code=401, detail="Missing user_id")
        
    cart = get_cart(x_user_id)
    enriched_cart = []
    
    async with httpx.AsyncClient() as client:
        try:
            res = await client.get(f"{INVENTORY_URL}/inventory/stock")
            inventory_data = res.json()
        except Exception as e:
            raise HTTPException(status_code=500, detail="Inventory service unavailable")
            
    total_cost = 0.0
    for item, qty in cart.items():
        inv_item = inventory_data.get(item, {})
        available_qty = inv_item.get("available", 0)
        price = inv_item.get("price", 0.0)
        
        is_available = available_qty >= qty
        
        enriched_cart.append({
            "item": item,
            "quantity": qty,
            "price": price,
            "available": is_available
        })
        
        if is_available:
            total_cost += price * qty
            
    return {
        "items": enriched_cart,
        "total": total_cost
    }

@app.get("/health")
def health():
    return {"status": "ok"}
