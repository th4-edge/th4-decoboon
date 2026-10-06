#!/usr/bin/env python3
"""Parse nginx timed log (combined + rt/urt/uct/uht) -> P50/P95/P99/max per route."""
import re, sys
from collections import defaultdict

PAT = re.compile(r'^(\S+) - (\S+) \[([^\]]+)\] "(\S+) (\S+) \S+" (\d+) (\d+) rt=([0-9.]+) urt=([0-9.\-]+) uct=([0-9.\-]+) uht=([0-9.\-]+)')

def pct(vals, p):
    vals = sorted(v for v in vals if v is not None)
    if not vals:
        return 0.0
    return vals[min(len(vals) - 1, int(round((len(vals) - 1) * p)))]

def main():
    path = sys.argv[1]
    rows = []
    for line in open(path, encoding='utf-8', errors='ignore'):
        m = PAT.search(line)
        if not m:
            continue
        ip, user, ts, method, uri, status, size, rt, urt, uct, uht = m.groups()
        def f(v):
            try: return float(v)
            except Exception: return None
        rows.append(dict(ts=ts, method=method, route=method + " " + uri.split("?")[0], status=int(status),
                         rt=f(rt), urt=(f(urt) if urt not in ("-", "") else f(rt)), size=int(size)))
    by = defaultdict(list)
    for r in rows:
        by[r['route']].append(r)
    print(f'valid_requests={len(rows)}')
    print(f'{"route":46} {"n":>3} {"P50":>7} {"P95":>7} {"P99":>7} {"max":>7} {"maxrt":>7} {"avgKB":>6} status')
    for route, items in sorted(by.items(), key=lambda kv: -max(x['urt'] for x in kv[1])):
        u = [x['urt'] for x in items]; r = [x['rt'] for x in items]
        avgkb = sum(x['size'] for x in items) / len(items) / 1024.0
        st = sorted(set(x['status'] for x in items))
        print(f'{route[:46]:46} {len(items):3d} {pct(u,0.5):7.3f} {pct(u,0.95):7.3f} {pct(u,0.99):7.3f} {max(u):7.3f} {max(r):7.3f} {avgkb:6.1f} {st}')

if __name__ == '__main__':
    main()
