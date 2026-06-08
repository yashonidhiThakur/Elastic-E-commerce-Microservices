FROM python:3.10-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY payment_service/ /app/payment_service/

CMD ["uvicorn", "payment_service.main:app", "--host", "0.0.0.0", "--port", "8005"]
