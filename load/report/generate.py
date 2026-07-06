import json
import argparse
import sys

def parse_k6_summary(filepath):
    try:
        with open(filepath, 'r') as f:
            data = json.load(f)
        
        metrics = data.get('metrics', {})
        
        # Helper to safely extract values
        def get_metric(name, field, default="N/A"):
            if name in metrics and field in metrics[name].get('values', {}):
                val = metrics[name]['values'][field]
                return f"{val:.2f}" if isinstance(val, (int, float)) else val
            return default

        http_duration = metrics.get('http_req_duration', {}).get('values', {})
        p50 = f"{http_duration.get('med', 0):.2f} ms" if 'med' in http_duration else "N/A"
        p95 = f"{http_duration.get('p(95)', 0):.2f} ms" if 'p(95)' in http_duration else "N/A"
        p99 = f"{http_duration.get('p(99)', 0):.2f} ms" if 'p(99)' in http_duration else "N/A"
        
        rps = get_metric('http_reqs', 'rate') + " req/s"
        
        error_rate_val = metrics.get('http_req_failed', {}).get('values', {}).get('rate', 0)
        error_rate = f"{error_rate_val * 100:.2f}%"
        
        checkout_success = get_metric('checkout_success_count', 'count', "0")
        
        return {
            'p50 latency': p50,
            'p95 latency': p95,
            'p99 latency': p99,
            'RPS': rps,
            'Error rate': error_rate,
            'Checkout success count': checkout_success
        }
    except Exception as e:
        print(f"Error parsing {filepath}: {e}", file=sys.stderr)
        # Return fallback empty data
        return {k: "Error" for k in ['p50 latency', 'p95 latency', 'p99 latency', 'RPS', 'Error rate', 'Checkout success count']}

def calculate_delta(before, after):
    try:
        # Simple extraction for numerical delta if possible
        b_val = float(before.replace(' ms', '').replace(' req/s', '').replace('%', ''))
        a_val = float(after.replace(' ms', '').replace(' req/s', '').replace('%', ''))
        diff = a_val - b_val
        
        if diff > 0:
            return f"+{diff:.2f}"
        return f"{diff:.2f}"
    except:
        return "N/A"

def generate_markdown(before_stats, after_stats, template_path, output_path, label):
    metrics_list = ['p50 latency', 'p95 latency', 'p99 latency', 'RPS', 'Error rate', 'Checkout success count']
    
    table = "| Metric | Before | After | Delta |\n"
    table += "| ------ | ------ | ----- | ----- |\n"
    
    for m in metrics_list:
        b = before_stats.get(m, 'N/A')
        a = after_stats.get(m, 'N/A')
        delta = calculate_delta(b, a)
        table += f"| {m} | {b} | {a} | {delta} |\n"
        
    try:
        with open(template_path, 'r') as f:
            template = f.read()
    except Exception as e:
        print(f"Error reading template: {e}", file=sys.stderr)
        template = "# Performance Report\n\n{label}\n\n{table}"
        
    final_md = template.replace('{{LABEL}}', label).replace('{{TABLE}}', table)
    
    with open(output_path, 'w') as f:
        f.write(final_md)
        
    print(f"Report generated successfully at {output_path}")

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generate k6 Markdown Report')
    parser.add_argument('--before', required=True, help='Path to before summary JSON')
    parser.add_argument('--after', required=True, help='Path to after summary JSON')
    parser.add_argument('--output', required=True, help='Path for output Markdown file')
    parser.add_argument('--label', default='Performance Comparison', help='Label/Title for the report')
    
    args = parser.parse_args()
    
    before_data = parse_k6_summary(args.before)
    after_data = parse_k6_summary(args.after)
    
    # We assume template.md is in the same directory as this script
    import os
    template_dir = os.path.dirname(os.path.abspath(__file__))
    template_path = os.path.join(template_dir, 'template.md')
    
    generate_markdown(before_data, after_data, template_path, args.output, args.label)
