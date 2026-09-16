#!/usr/bin/env python3
import fcntl
import hashlib
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

RAW_PATH = Path(sys.argv[1])
OUT_DIR = Path(sys.argv[2])
OUT_DIR.mkdir(parents=True, exist_ok=True)

EXPECTED_PATTERNS = [
    'tokenIsUndefined',
    '缺少授权标识',
    '手机号或密码错误',
    '首次登录请授权手机号',
    '当前用户已有在职企业',
    '微信支付回调签名缺失',
    '微信支付回调原始报文不存在',
    '找不到对应的处理服务:.env',
]
CODE_BUG_PATTERNS = [
    'LazyInitializationException',
    'Could not initialize proxy',
    'NullPointerException',
    'ClassCastException',
    'IndexOutOfBoundsException',
    'NoSuchMethodError',
    'AbstractMethodError',
    'IllegalStateException',
    'DataIntegrityViolationException',
    'TransactionSystemException',
    'Cannot invoke',
]
INFRA_PATTERNS = [
    'OutOfMemoryError',
    'No space left',
    'Connection refused',
    'Communications link failure',
    'RedisConnectionFailureException',
    'UnknownHostException',
    'SocketTimeoutException',
    'TimeoutException',
    'Too many connections',
    'Deadlock',
    'Read timed out',
]
EXTERNAL_PATTERNS = [
    'Aliyun',
    'OSS',
    'Wechat',
    'WeChat',
    '微信支付',
    'payment',
]

ERROR_START = re.compile(r'(\[ERROR\]|\bERROR\b|\bException\b|Caused by:)')
TIMESTAMP_PREFIX = re.compile(r'^\s*(?:\[?\d{4}-\d{2}-\d{2}[T\s]|[A-Z][a-z]{2}\s+\d{1,2}\s)')
STACK_LINE = re.compile(r'^\s*(at\s+|Caused by:|\.{3}\s+\d+\s+more|Suppressed:|org\.|com\.|java\.|jakarta\.)')


def read_text(path: Path) -> str:
    return path.read_text(encoding='utf-8', errors='replace')


def parse_sections(text: str):
    sections = {}
    current = None
    buffer = []
    for line in text.splitlines():
        match = re.match(r'^== ([A-Z_]+) ==$', line.strip())
        if match:
            if current is not None:
                sections[current] = '\n'.join(buffer)
            current = match.group(1)
            buffer = []
        elif current is not None:
            buffer.append(line)
    if current is not None:
        sections[current] = '\n'.join(buffer)
    return sections


def extract_events(text: str):
    blocks = []
    current = []
    for line in text.splitlines():
        if ERROR_START.search(line):
            if current:
                blocks.append('\n'.join(current))
            current = [line]
        elif current and (STACK_LINE.search(line) or line.startswith((' ', '\t'))):
            current.append(line)
        elif current:
            blocks.append('\n'.join(current))
            current = []
    if current:
        blocks.append('\n'.join(current))
    return blocks


def message_of(block: str) -> str:
    first = block.splitlines()[0].strip()
    if '处理失败:' in first:
        first = first.split('处理失败:', 1)[1].strip()
    elif re.search(r'\]\s+-\s+', first):
        first = re.split(r'\]\s+-\s+', first, maxsplit=1)[1].strip()
    elif re.search(r'ApiHttpProcessor\s*:\s*', first):
        first = re.split(r'ApiHttpProcessor\s*:\s*', first, maxsplit=1)[1].strip()
    if 'Could not initialize proxy' in first or 'Could not initialize proxy' in block:
        return 'LazyInitializationException: Could not initialize proxy - no session'
    exception = re.search(r'([A-Za-z0-9_.]+(?:Exception|Error))', block)
    if exception and exception.group(1) not in first:
        first = f'{exception.group(1)}: {first}'
    return first[:500]


def normalize(message: str) -> str:
    value = message
    value = re.sub(r'https?://\S+', '<url>', value)
    value = re.sub(r'\b[0-9a-fA-F]{16,}\b', '<hex>', value)
    value = re.sub(r'\b\d{4,}\b', '<id>', value)
    value = re.sub(r'([A-Za-z0-9_.$-]+\.java:)\d+', r'\1<line>', value)
    value = re.sub(r'\bXNIO-\S+', '<thread>', value)
    value = re.sub(r'\s+', ' ', value).strip()
    return value[:240]


def classify(signature: str) -> str:
    lowered = signature.lower()
    if any(pattern.lower() in lowered for pattern in EXPECTED_PATTERNS):
        return 'expected'
    if any(pattern.lower() in lowered for pattern in CODE_BUG_PATTERNS):
        return 'code_bug'
    if any(pattern.lower() in lowered for pattern in INFRA_PATTERNS):
        return 'infrastructure'
    if any(pattern.lower() in lowered for pattern in EXTERNAL_PATTERNS):
        return 'external_dependency'
    return 'unknown'


def severity_of(classification: str) -> str:
    return {
        'code_bug': 'critical',
        'infrastructure': 'high',
        'external_dependency': 'high',
        'unknown': 'medium',
        'expected': 'info',
    }.get(classification, 'medium')


def parse_time_from_block(block: str):
    match = re.search(r'(\d{4}-\d{2}-\d{2}[T ][0-9:.+-]+)', block)
    if match:
        return match.group(1)
    return None


def make_incident(signature: str, classification: str):
    digest = hashlib.sha1(signature.encode('utf-8')).hexdigest()[:10]
    return {
        'incidentId': f'INC-{datetime.now().strftime("%Y%m%d")}-{digest}',
        'signature': signature,
        'signatureHash': digest,
        'classification': classification,
        'severity': severity_of(classification),
        'status': 'open' if classification != 'expected' else 'observed',
        'actionable': classification != 'expected',
        'count': 0,
        'firstSeen': None,
        'lastSeen': None,
        'samples': [],
        'reportPaths': [],
    }


def load_registry():
    incidents_path = OUT_DIR.parents[1] / 'registry.json'
    if not incidents_path.exists():
        return {'schema': 'th4-decoboon.incidents.v1', 'incidents': []}
    try:
        return json.loads(incidents_path.read_text(encoding='utf-8'))
    except json.JSONDecodeError:
        return {'schema': 'th4-decoboon.incidents.v1', 'incidents': []}


def comparable_time(value):
    if not value:
        return ''
    return value.replace('T', ' ')[:19]


def apply_fixed_baseline(incidents, existing_registry):
    existing = {}
    for item in existing_registry.get('incidents', []):
        item = canonicalize_existing(item)
        existing[item.get('signature')] = item
    for incident in incidents:
        if incident['classification'] == 'expected':
            incident['actionable'] = False
            incident['status'] = 'observed'
            continue
        previous = existing.get(incident['signature'])
        fixed_at = previous.get('fixedAt') if previous else None
        if previous and fixed_at and incident.get('lastSeen') and comparable_time(incident['lastSeen']) <= comparable_time(fixed_at):
            incident['actionable'] = False
            incident['status'] = 'fixed'
            incident['fixedAt'] = fixed_at
            incident['fixedRelease'] = previous.get('fixedRelease')
        else:
            incident['actionable'] = True
            incident['status'] = 'reopened' if fixed_at else 'open'
    return incidents


def canonicalize_existing(item):
    signature = item.get('signature', '')
    if 'Could not initialize proxy' in signature or signature == 'no session':
        canonical = 'LazyInitializationException: Could not initialize proxy - no session'
        item['signature'] = canonical
        item['signatureHash'] = hashlib.sha1(canonical.encode('utf-8')).hexdigest()[:10]
        item['incidentId'] = f'INC-{datetime.now().strftime("%Y%m%d")}-{item["signatureHash"]}'
        item['classification'] = 'code_bug'
        item['severity'] = 'critical'
        item['status'] = 'open'
    return item


def merge_incident(target, source):
    target['count'] = int(target.get('count', 0)) + int(source.get('count', 0))
    if source.get('firstSeen') and (not target.get('firstSeen') or source['firstSeen'] < target['firstSeen']):
        target['firstSeen'] = source['firstSeen']
    if source.get('lastSeen') and (not target.get('lastSeen') or source['lastSeen'] > target['lastSeen']):
        target['lastSeen'] = source['lastSeen']
    target['samples'] = (list(target.get('samples', [])) + list(source.get('samples', [])))[:5]
    paths = list(target.get('reportPaths', []))
    for path in source.get('reportPaths', []):
        if path not in paths:
            paths.append(path)
    target['reportPaths'] = paths[-20:]
    return target


def update_registry(incidents, report_rel: str):
    incident_root = OUT_DIR.parents[1]
    incidents_path = incident_root / 'registry.json'
    lock_path = incident_root / '.registry.lock'
    incidents_path.parent.mkdir(parents=True, exist_ok=True)
    with lock_path.open('a+', encoding='utf-8') as lock:
        fcntl.flock(lock.fileno(), fcntl.LOCK_EX)
        if incidents_path.exists():
            try:
                registry = json.loads(incidents_path.read_text(encoding='utf-8'))
            except json.JSONDecodeError:
                registry = {'schema': 'th4-decoboon.incidents.v1', 'incidents': []}
        else:
            registry = {'schema': 'th4-decoboon.incidents.v1', 'incidents': []}
        reconciled = {}
        for item in registry.get('incidents', []):
            item = canonicalize_existing(item)
            signature = item.get('signature')
            if signature in reconciled:
                merge_incident(reconciled[signature], item)
            else:
                reconciled[signature] = item
        registry['incidents'] = list(reconciled.values())
        by_signature = reconciled
        for incident in incidents:
            if incident['classification'] == 'expected':
                continue
            existing = by_signature.get(incident['signature'])
            if existing:
                existing['classification'] = incident['classification']
                existing['severity'] = incident['severity']
                if incident.get('actionable'):
                    existing['status'] = 'reopened' if existing.get('fixedAt') else 'open'
                elif existing.get('status') != 'fixed':
                    existing['status'] = incident['status']
                existing['actionable'] = incident.get('actionable', existing.get('actionable', True))
                existing['count'] = int(existing.get('count', 0)) + incident['count']
                existing['lastSeen'] = incident['lastSeen'] or existing.get('lastSeen')
                if incident['firstSeen'] and not existing.get('firstSeen'):
                    existing['firstSeen'] = incident['firstSeen']
                samples = list(existing.get('samples', [])) + incident['samples']
                existing['samples'] = samples[:5]
                paths = list(existing.get('reportPaths', []))
                if report_rel not in paths:
                    paths.append(report_rel)
                existing['reportPaths'] = paths[-20:]
            else:
                incident['reportPaths'] = [report_rel]
                registry.setdefault('incidents', []).append(incident)
                by_signature[incident['signature']] = incident
        registry['updatedAt'] = datetime.now(timezone.utc).isoformat()
        incidents_path.write_text(json.dumps(registry, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        fcntl.flock(lock.fileno(), fcntl.LOCK_UN)


def main():
    raw = read_text(RAW_PATH)
    sections = parse_sections(raw)
    events = []
    for section_name in ('LOG_ERROR', 'STDOUT_ERRORS', 'JOURNAL'):
        for block in extract_events(sections.get(section_name, '')):
            events.append((section_name, block))

    grouped = {}
    for source, block in events:
        message = message_of(block)
        signature = normalize(message)
        if not signature:
            continue
        classification = classify(signature)
        item = grouped.setdefault(signature, make_incident(signature, classification))
        item['count'] += 1
        seen = parse_time_from_block(block)
        if seen:
            item['firstSeen'] = min(filter(None, [item['firstSeen'], seen]))
            item['lastSeen'] = max(filter(None, [item['lastSeen'], seen]))
        if len(item['samples']) < 5:
            item['samples'].append(block[:1800])

    incidents = sorted(grouped.values(), key=lambda item: (item['classification'] != 'code_bug', item['classification'] != 'infrastructure', item['signature']))
    incidents = apply_fixed_baseline(incidents, load_registry())
    report_rel = str(OUT_DIR.relative_to(OUT_DIR.parents[1]))
    update_registry(incidents, report_rel)

    counts = {}
    for incident in incidents:
        counts[incident['classification']] = counts.get(incident['classification'], 0) + incident['count']
    actionable_count = sum(item['count'] for item in incidents if item.get('actionable'))
    fixed_count = sum(item['count'] for item in incidents if item.get('status') == 'fixed')

    analysis = {
        'collectedAt': datetime.now(timezone.utc).isoformat(),
        'reportDir': str(OUT_DIR),
        'counts': counts,
        'incidents': incidents,
    }
    (OUT_DIR / 'analysis.json').write_text(json.dumps(analysis, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

    service = sections.get('SERVICE', '').strip()
    health = sections.get('HEALTH', '').strip()
    resources = sections.get('RESOURCES', '').strip()
    lines = [
        '# 生产后端监控摘要',
        '',
        f'- 采集时间：{datetime.now().astimezone().isoformat(timespec="seconds")}',
        f'- 原始快照：`{RAW_PATH}`',
        f'- 可处理异常：{actionable_count}',
        f'- 已修复历史异常：{fixed_count}',
        f'- 预期业务噪音：{counts.get("expected", 0)}',
        '',
        '## 服务状态',
        '',
        '```text',
        service or '未采集到服务状态',
        '```',
        '',
        '## 健康检查',
        '',
        '```text',
        health or '未采集到健康检查',
        '```',
        '',
        '## 资源状态',
        '',
        '```text',
        resources,
        '```',
        '',
        '## 异常分组',
        '',
        '| 分类 | 状态 | 次数 | 严重度 | 指纹 |',
        '| --- | --- | ---: | --- | --- |',
    ]
    for incident in incidents:
        lines.append(f"| {incident['classification']} | {incident.get('status', 'open')} | {incident['count']} | {incident['severity']} | `{incident['signature']}` |")
    if not incidents:
        lines.append('| 无 | observed | 0 | info | 未发现 ERROR 事件 |')

    actionable = [item for item in incidents if item.get('actionable')]
    if actionable:
        lines.extend(['', '## 证据', ''])
        for incident in actionable[:10]:
            lines.extend([
                f"### {incident['incidentId']} · {incident['classification']}",
                '',
                f"- 指纹：`{incident['signature']}`",
                f"- 次数：{incident['count']}",
                f"- 首次：{incident['firstSeen'] or '未知'}",
                f"- 最近：{incident['lastSeen'] or '未知'}",
                '',
                '```text',
                incident['samples'][0][:1800] if incident['samples'] else '无样例',
                '```',
                '',
            ])

    (OUT_DIR / 'summary.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')

    actionable_incidents = [item for item in incidents if item.get('actionable')]
    code_bugs = sum(item['count'] for item in actionable_incidents if item['classification'] == 'code_bug')
    infra = sum(item['count'] for item in actionable_incidents if item['classification'] in ('infrastructure', 'external_dependency'))
    unknown = sum(item['count'] for item in actionable_incidents if item['classification'] == 'unknown')
    if code_bugs:
        return 10
    if infra or unknown:
        return 20
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
