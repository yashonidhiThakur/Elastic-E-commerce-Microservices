#!/bin/bash

# Exit on any error
set -e

echo "🚀 Applying updated Kubernetes Deployments (with JVM optimizations) and HPAs..."

# Apply the configmap and individual service specs
kubectl apply -f k8s/configmap.yml

# Apply auth, cart, inventory, payment, consumer, gateway manifests
kubectl apply -f k8s/auth/
kubectl apply -f k8s/cart/
kubectl apply -f k8s/inventory/
kubectl apply -f k8s/payment/
kubectl apply -f k8s/consumer/
kubectl apply -f k8s/gateway/

echo "------------------------------------------------------------------"
echo "✅ All Kubernetes configurations and HPAs applied successfully!"
echo "------------------------------------------------------------------"
echo ""
echo "💡 IMPORTANT STEPS FOR THE FLASH SALE LOAD TEST:"
echo ""
echo "1. Ensure Metrics Server is enabled in your Kubernetes cluster:"
echo "   - If using Minikube:  minikube addons enable metrics-server"
echo "   - If using Docker Desktop: Deploy the metrics-server YAML and patch it if needed."
echo ""
echo "2. Open a separate terminal window and start watching the HPA auto-scaling:"
echo "   kubectl get hpa -w"
echo ""
echo "3. Open another terminal window to watch the pods scaling up in real-time:"
echo "   kubectl get pods -w"
echo ""
echo "4. Expose the API Gateway port to your host machine (if not already done):"
echo "   kubectl port-forward svc/gateway-service 8000:8000"
echo ""
echo "5. Run the k6 load test script:"
echo "   - If you have k6 installed locally:"
echo "     k6 run flash_sale_load_test.js"
echo "   - Or run it via Docker:"
echo "     docker run --rm -i -e GATEWAY_URL=http://host.docker.internal:8000 grafana/k6 run - <flash_sale_load_test.js"
echo ""
echo "6. Verify database consistency after the test completes (no negative stock!):"
echo "   - Run: sqlite3 inventory_service/inventory.db \"SELECT * FROM inventory;\""
echo "------------------------------------------------------------------"
