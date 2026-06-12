import sqlite3
import os

def init_db():
    os.makedirs('db', exist_ok=True)
    conn = sqlite3.connect('db/app.db')
    cursor = conn.cursor()
    
    # Create users table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY,
        username TEXT UNIQUE,
        password TEXT,
        wallet_balance REAL
    )
    ''')
    
    # Create sessions table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS sessions (
        token TEXT PRIMARY KEY,
        user_id INTEGER,
        created_at TEXT
    )
    ''')
    
    # Insert initial users
    users = [
        (1, 'nir', 'password123', 20000.0),
        (2, 'yashonidhi', 'password456', 20000.0),
        (3, 'random', 'password789', 10000.0)
    ]
    for i in range(1, 201):
        users.append((3 + i, f'user_{i}', f'pass_{i}', 20000.0))
    
    cursor.executemany('INSERT OR IGNORE INTO users VALUES (?, ?, ?, ?)', users)
    
    conn.commit()
    conn.close()
    print("Database initialized successfully.")

if __name__ == '__main__':
    init_db()
