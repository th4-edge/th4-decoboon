#!/usr/bin/env python3
import json
import re
import http.client
from collections import Counter
from pathlib import Path
from openpyxl import load_workbook

SOURCE = Path('/Users/zhangwei/Desktop/麦鸿装饰报价9.5.xlsx')
TOKEN_FILE = Path('/private/tmp/maihong-token.txt')
RUN_DIR = Path(__file__).resolve().parent
TOKEN = TOKEN_FILE.read_text().strip()

SHEETS = {
    '综合项目报价清单': 'General',
    '施工报价清单': 'Construction',
    '主材报价清单': 'Material',
    '全屋定制、橱柜报价清单': 'Custom',
}
UNIT_MAP = {
    '平米': 'Area', '㎡': 'Area', 'm²': 'Area', '平方米': 'Area',
    '米': 'Metre', 'm': 'Metre', 'M': 'Metre',
    '个': 'Count', '只': 'Count',
    '项': 'Item', '车': 'Item',
    '处': 'Place', '套': 'Set', '户': 'House', '步': 'Step', '樘': 'Plate',
    '块': 'Block', '根': 'Root', '扇': 'Hole', '台': 'Pcs', '支': 'Stick',
    '格': 'Grid', '片': 'Sheet', '厘米': 'Centimeter', 'cm': 'Centimeter',
}

def number(value):
    if value is None:
        return 0.0
    text = str(value).strip()
    if text in {'', '/', '//', '-', '—', '#VALUE!'}:
        return 0.0
    try:
        return float(text)
    except ValueError:
        return 0.0

def clean(value, limit=None):
    if value is None:
        return None
    text = re.sub(r'\s+', ' ', str(value)).strip()
    if not text or text in {'/', '//', '#VALUE!'}:
        return None
    return text[:limit] if limit else text

def item_rows():
    wb = load_workbook(SOURCE, data_only=True)
    summary = Counter()
    anomalies = []
    for sheet_name, item_type in SHEETS.items():
        ws = wb[sheet_name]
        section = ''
        for row in range(4, ws.max_row + 1):
            seq = ws.cell(row, 1).value
            raw_name = ws.cell(row, 2).value
            if seq is not None and str(seq).strip() and (raw_name is None or not str(raw_name).strip()):
                section = str(seq).strip()
                continue
            if seq is None or raw_name is None:
                continue
            if not str(seq).strip().replace('.', '', 1).isdigit():
                continue
            raw_name_text = clean(raw_name, 256)
            if not raw_name_text:
                continue
            series = None
            name = raw_name_text
            if '--' in raw_name_text:
                series, name = raw_name_text.split('--', 1)
                series = clean(series, 128)
                name = clean(name, 256) or raw_name_text
            material = number(ws.cell(row, 5).value)
            manual = number(ws.cell(row, 6).value)
            total = number(ws.cell(row, 7).value)
            if material <= 0 and manual <= 0:
                summary['skipped_zero_price'] += 1
                anomalies.append({'sheet': sheet_name, 'row': row, 'reason': '零价格/备注行', 'name': name})
                continue
            unit_raw = clean(ws.cell(row, 3).value) or '项'
            unit = UNIT_MAP.get(unit_raw)
            if not unit:
                unit = 'Item'
                anomalies.append({'sheet': sheet_name, 'row': row, 'reason': f'单位“{unit_raw}”已映射为项', 'name': name})
            category = clean(ws.cell(row, 9).value, 64) or clean(section, 64) or '未分类'
            if manual > 0 and material > 0:
                pricing = 'MaterialAndManual'
            elif manual > 0:
                pricing = 'ManualOnly'
            else:
                pricing = 'MaterialOnly'
            brand = None
            if item_type == 'Material' and section and section != category and len(section) <= 32:
                brand = section
            elif item_type == 'Custom':
                brand = '志邦' if '志邦' in section else '麦鸿定制'
            craft = clean(ws.cell(row, 8).value)
            description = clean(ws.cell(row, 10).value)
            if description in {'#VALUE!'}:
                description = None
            if description and len(description) > 1000:
                description = description[:1000]
            summary[f'candidate_{item_type}'] += 1
            yield {
                'type': item_type,
                'category': category,
                'brand': brand,
                'series': series,
                'spec': None,
                'name': name,
                'unit': unit,
                'pricingMode': pricing,
                'materialPrice': round(material, 2),
                'manualPrice': round(manual, 2),
                'costPrice': 0,
                'craft': craft,
                'criterion': None,
                'description': description,
                'images': [],
                'sort': row,
                '_sheet': sheet_name,
                '_row': row,
                '_source_total': total,
            }
    RUN_DIR.joinpath('import-anomalies.json').write_text(json.dumps(anomalies, ensure_ascii=False, indent=2))
    RUN_DIR.joinpath('import-precheck.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2))
    print(json.dumps({'precheck': summary, 'anomalies': len(anomalies)}, ensure_ascii=False))

def post(payload):
    connection = http.client.HTTPConnection('127.0.0.1', 8090, timeout=20)
    try:
        body = json.dumps({k: v for k, v in payload.items() if not k.startswith('_')}, ensure_ascii=False).encode()
        connection.request('POST', '/api/catalog/item/save', body, {
            'content-type': 'application/json',
            'token': TOKEN,
        })
        response = connection.getresponse()
        text = response.read().decode(errors='replace')
        result = json.loads(text)
        if response.status >= 400:
            return {'success': False, 'httpStatus': response.status, 'body': result}
        return result
    except Exception as error:
        return {'success': False, 'error': str(error)}
    finally:
        connection.close()

def main():
    rows = list(item_rows())
    results = []
    for index, payload in enumerate(rows, 1):
        result = post(payload)
        results.append({
            'index': index,
            'sheet': payload['_sheet'],
            'row': payload['_row'],
            'name': payload['name'],
            'success': bool(result.get('success')),
            'error': None if result.get('success') else result,
        })
        if index % 25 == 0:
            print(json.dumps({'processed': index, 'total': len(rows)}, ensure_ascii=False), flush=True)
    RUN_DIR.joinpath('import-results.json').write_text(json.dumps(results, ensure_ascii=False, indent=2))
    ok = sum(1 for item in results if item['success'])
    fail = len(results) - ok
    print(json.dumps({'imported': ok, 'failed': fail, 'total': len(results)}, ensure_ascii=False))
    if fail:
        raise SystemExit(1)

if __name__ == '__main__':
    main()
