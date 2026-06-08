#!/bin/bash
echo "🚀 Starting HPA Load Test on API Gateway..."
echo "Generating massive traffic. This will take about 30-60 seconds to trigger the auto-scaler."
echo "Keep your other terminals open to watch 'kubectl get hpa -w' and 'kubectl get pods -w'!"

# Run a temporary busybox pod that spams the gateway with 5 parallel endless loops of requests
kubectl run -i --tty load-generator --rm --image=busybox --restart=Never -- /bin/sh -c '
i=1
while [ $i -le 10 ]; do
  while true; do wget -q -O- http://gateway-service:8000 > /dev/null; done &
  i=$((i + 1))
done
wait
'
