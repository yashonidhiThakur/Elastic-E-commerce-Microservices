import os
import json
import pika
from dotenv import load_dotenv
from . import db

load_dotenv()

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")

def publish_depleted(channel, item):
    event = json.dumps({"item": item})
    channel.basic_publish(exchange='inventory_events', routing_key='', body=event)
    print(f" [x] Published inventory.depleted for {item}")

def callback(ch, method, properties, body):
    order = json.loads(body)
    print(f" [x] Received order.paid event: {order}")
    
    item = order.get("item")
    quantity = order.get("quantity", 0)
    
    conn = db.get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT stock, reserved FROM inventory WHERE item = ?", (item,))
    row = cursor.fetchone()
    
    if row:
        current_stock = row['stock']
        print(f" [✓] Acknowledged {item} payment. Current stock: {current_stock}")
        
        # Check if depleted
        if current_stock - row['reserved'] <= 0:
            ch.exchange_declare(exchange='inventory_events', exchange_type='fanout')
            publish_depleted(ch, item)
    else:
        print(f" [!] Item '{item}' not found in inventory.")
        
    conn.close()

def start_consumer():
    connection = pika.BlockingConnection(pika.ConnectionParameters(host=RABBITMQ_HOST))
    channel = connection.channel()

    channel.exchange_declare(exchange='orders', exchange_type='fanout')
    
    result = channel.queue_declare(queue='', exclusive=True)
    queue_name = result.method.queue

    channel.queue_bind(exchange='orders', queue=queue_name)

    print(' [*] Waiting for order.paid events. To exit press CTRL+C')

    channel.basic_consume(
        queue=queue_name, on_message_callback=callback, auto_ack=True)

    channel.start_consuming()

if __name__ == '__main__':
    try:
        start_consumer()
    except KeyboardInterrupt:
        print("Interrupted")
        try:
            import sys
            sys.exit(0)
        except SystemExit:
            os._exit(0)
