#!/usr/bin/env python3
"""Generate the public WeChat mini-program code used by the A4 product sheet.

The app secret is read from the repository's local production environment file and
is never printed or written to the output metadata.
"""
from __future__ import annotations

import json
import re
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
OUT = Path(__file__).resolve().parent
ENV_FILE = ROOT / '.env.prod.local'
APP_ID_FALLBACK = 'wxe5cc5fec9e50a9a9'


def read_env(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for line in path.read_text(encoding='utf-8').splitlines():
        match = re.match(r'export\s+([A-Z0-9_]+)=(.*)$', line.strip())
        if match:
            values[match.group(1)] = match.group(2).strip().strip('"').strip("'")
    return values


def fetch_json(url: str) -> dict:
    with urllib.request.urlopen(url, timeout=15) as response:
        return json.load(response)


env = read_env(ENV_FILE)
app_id = env.get('WECHAT_MINI_APP_ID') or APP_ID_FALLBACK
app_secret = env.get('WECHAT_MINI_APP_SECRET', '')
if not app_secret:
    raise SystemExit(f'WECHAT_MINI_APP_SECRET not found in {ENV_FILE}')

token_data = fetch_json(
    'https://api.weixin.qq.com/cgi-bin/token?'
    + urllib.parse.urlencode({
        'grant_type': 'client_credential',
        'appid': app_id,
        'secret': app_secret,
    })
)
if token_data.get('errcode'):
    raise SystemExit(f"access token failed: {token_data.get('errcode')} {token_data.get('errmsg', '')}")

payload = json.dumps({
    'scene': 'product-a4',
    'page': 'pages/index/index',
    'check_path': False,
    'width': 430,
    'is_hyaline': False,
    'env_version': 'release',
}, ensure_ascii=False).encode('utf-8')

request = urllib.request.Request(
    'https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token='
    + urllib.parse.quote(token_data['access_token']),
    data=payload,
    headers={'Content-Type': 'application/json'},
    method='POST',
)
with urllib.request.urlopen(request, timeout=20) as response:
    body = response.read()
    content_type = response.headers.get('Content-Type', '')

if body.startswith(b'{'):
    try:
        error = json.loads(body)
    except json.JSONDecodeError:
        error = {'raw': body[:300].decode('utf-8', 'replace')}
    raise SystemExit(f'mini-program code failed: {error}')

assets = OUT / 'assets'
assets.mkdir(parents=True, exist_ok=True)
qr_path = assets / 'miniapp-qrcode-release.jpg'
qr_path.write_bytes(body)
metadata = {
    'source': 'WeChat getwxacodeunlimit',
    'appid': app_id,
    'page': 'pages/index/index',
    'scene': 'product-a4',
    'env_version': 'release',
    'width': 430,
    'content_type': content_type,
    'bytes': len(body),
}
(assets / 'miniapp-qrcode-release.json').write_text(
    json.dumps(metadata, ensure_ascii=False, indent=2) + '\n',
    encoding='utf-8',
)
print(json.dumps(metadata, ensure_ascii=False, indent=2))
