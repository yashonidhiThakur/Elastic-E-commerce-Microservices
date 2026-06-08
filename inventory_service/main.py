from fastapi import FastAPI
from . import db

app = FastAPI(title="Inventory Service")

@app.get("/inventory")
def get_inventory():
    conn = db.get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT item, stock, price, reserved FROM inventory")
    rows = cursor.fetchall()
    conn.close()
    return {row['item']: row['stock'] for row in rows}

@app.get("/inventory/stock")
def get_inventory_stock():
    conn = db.get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT item, (stock - reserved) as available, price FROM inventory")
    rows = cursor.fetchall()
    conn.close()
    return {
        row['item']: {
            "available": max(0, row['available']),
            "price": row['price']
        } for row in rows
    }

@app.get("/health")
def health():
    return {"status": "ok"}
