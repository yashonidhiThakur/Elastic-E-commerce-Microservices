FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY inventory_service /app/inventory_service
CMD ["uvicorn", "inventory_service.main:app", "--host", "0.0.0.0", "--port", "8002"]
