import os

files_to_edit = [
    ('k8s/gateway/deployment.yml', True),
    ('k8s/gateway/service.yml', False),
    ('k8s/auth/deployment.yml', True),
    ('k8s/auth/service.yml', False),
    ('k8s/cart/deployment.yml', True),
    ('k8s/cart/service.yml', False),
    ('k8s/payment/deployment.yml', True),
    ('k8s/payment/service.yml', False),
    ('k8s/inventory/deployment.yml', True),
    ('k8s/inventory/service.yml', False),
    ('k8s/consumer/deployment.yml', True),
]

for fpath, is_deploy in files_to_edit:
    if not os.path.exists(fpath):
        print(f"Missing {fpath}")
        continue
    
    with open(fpath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    out_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        out_lines.append(line)
        
        # Look for app: ... under selector or labels
        if line.strip().startswith('app: '):
            # Check the next line to see if it's already slot: blue
            if i + 1 < len(lines) and 'slot: blue' in lines[i+1]:
                # already there, do nothing
                pass
            else:
                # Need to insert slot: blue with exactly same indentation
                indent = line[:len(line) - len(line.lstrip())]
                out_lines.append(f"{indent}slot: blue\n")
        i += 1
        
    with open(fpath, 'w', encoding='utf-8') as f:
        f.writelines(out_lines)
    print(f"Updated {fpath}")
