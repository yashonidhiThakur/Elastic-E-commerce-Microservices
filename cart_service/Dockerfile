FROM python:3.10-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY cart_service/ /app/cart_service/

CMD ["uvicorn", "cart_service.main:app", "--host", "0.0.0.0", "--port", "8004"]
