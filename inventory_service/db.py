import sqlite3
import os

DB_FILE = os.path.join(os.path.dirname(__file__), "inventory.db")

def init_db():
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS inventory (
        item TEXT PRIMARY KEY,
        stock INTEGER,
        price REAL,
        reserved INTEGER DEFAULT 0
    )
    ''')
    
    # Insert default data if empty
    cursor.execute('SELECT COUNT(*) FROM inventory')
    if cursor.fetchone()[0] == 0:
        cursor.executemany('''
        INSERT INTO inventory (item, stock, price, reserved) 
        VALUES (?, ?, ?, ?)
        ''', [
            ("laptop", 100, 1000.0, 0),
            ("mouse", 100, 25.0, 0),
            ("keyboard", 100, 75.0, 0)
        ])
    conn.commit()
    conn.close()

def get_db():
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn

init_db()
