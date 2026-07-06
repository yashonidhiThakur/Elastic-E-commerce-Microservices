# k6 Load Test Harness

This directory contains the load testing harness to quantitatively measure the performance of the microservices-ecommerce-platform.

## Prerequisites
- [k6](https://k6.io/docs/getting-started/installation/) installed on your machine.
- Python 3.9+ (for generating reports).
- The application stack running locally.

## Bringing up the Stack
To start the application stack under test, run from the root of the project:
```bash
docker compose up -d
```

## Running Scenarios

**IMPORTANT WARNING**: Do not run the heavy k6 load tests (e.g. `flash_sale.js` or `baseline.js`) from inside a container on the same host as your application stack. Doing so shifts the bottleneck to your test rig and artificially limits your metrics. Run k6 locally, or ideally, on a completely separate physical machine.

To run a scenario and export the metrics summary for reporting:
```bash
k6 run --summary-export=summary.json scenarios/flash_sale.js
```

### Scenario Descriptions
- **checkout_smoke.js**: A sanity check (1 VU, 10s) to verify the stack and checkout flow work. Run this first!
- **baseline.js**: Simulates typical day-to-day traffic across products and carts.
- **flash_sale.js**: The headline scenario! Hammering a single product with thousands of concurrent checkouts to test concurrency and sharded counters.
- **soak.js**: A 30-minute steady load to identify memory leaks or consumer lag.

## Comparing Runs (Reporting)

The report generator script consumes two k6 summary JSON files to produce a readable Markdown table of the delta.

**Workflow Example**:
1. Run baseline on original architecture:
   ```bash
   k6 run --summary-export=before.json scenarios/flash_sale.js
   ```
2. Implement your architectural optimization (e.g., Sharded Counters).
3. Re-run on new architecture:
   ```bash
   k6 run --summary-export=after.json scenarios/flash_sale.js
   ```
4. Generate the comparison report:
   ```bash
   python report/generate.py \
     --before before.json \
     --after after.json \
     --output report.md \
     --label "Sharded counters optimization"
   ```

## Live Dashboards (Optional)
If you want to view live metrics in Grafana while k6 runs:
1. Start the monitoring stack:
   ```bash
   docker-compose -f docker/docker-compose.k6.yml up -d
   ```
2. Run k6 streaming to InfluxDB:
   ```bash
   k6 run --out influxdb=http://localhost:8086/k6 scenarios/baseline.js
   ```
3. Open Grafana at `http://localhost:3000` and configure an InfluxDB data source pointing to `http://influxdb:8086` database `k6`.
