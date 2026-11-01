import json, re
from pathlib import Path
from datetime import datetime, timezone

run = Path('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261031-dq-x8-x11-independent-acceptance')
raw = run / 'raw'
api = json.loads((raw / 'api-acceptance-results.json').read_text())
web_range = json.loads((raw / 'web-dom-network-check.json').read_text())
web_contract = json.loads((raw / 'web-contract-page-check.json').read_text())
native = json.loads((raw / 'app-native-range-check.json').read_text())
integrity = json.loads((raw / 'integrity-comparison.json').read_text())
cleanup_text = (raw / 'process-cleanup.txt').read_text()
batch = (raw / 'batch-snapshot-contract.txt').read_text()
full_log = (raw / 'backend-full-tests.log').read_text()
target_log = (raw / 'backend-targeted-tests.log').read_text()

web_range_checks = {
    'initial_source_rows_7': web_range['initial']['rows'] == 7,
    'material_source_rows_3': web_range['material']['rows'] == 3,
    'initial_request_current_size': any(json.loads(x['postData']) == {'contractId':'241939522005237760','current':1,'size':20} for x in web_range['rangeRequests'] if x.get('postData')),
    'material_request_current_size_itemType': any((lambda b: b.get('current') == 1 and b.get('size') == 20 and b.get('itemType') == 'Material')(json.loads(x['postData'])) for x in web_range['rangeRequests'] if x.get('postData')),
    'range_responses_200': bool(web_range['rangeResponses']) and all(x.get('status') == 200 for x in web_range['rangeResponses']),
    'console_errors_0': len(web_range['consoleErrors']) == 0,
    'exceptions_0': len(web_range['exceptions']) == 0,
    'no_overflow_1440': next(x for x in web_range['viewports'] if x['width'] == 1440)['docHorizontalOverflow'] is False,
    'no_overflow_1280': next(x for x in web_range['viewports'] if x['width'] == 1280)['docHorizontalOverflow'] is False,
    'no_overflow_1024': next(x for x in web_range['viewports'] if x['width'] == 1024)['docHorizontalOverflow'] is False,
}
web_contract_checks = {
    'line_request_current_size': any((lambda b: b.get('current') == 1 and b.get('size') == 20)(json.loads(x['postData'])) for x in web_contract['requests'] if x['url'].endswith('/project/contract-line/page')),
    'line_rows_6': web_contract['lineRows'] == 6,
    'range_header_6_977_80': '6 项' in web_contract['rangeHeader'] and '¥977.80' in web_contract['rangeHeader'],
    'change_1_minus_168': '合同变更' in web_contract['text'] and '1 项 · ¥-168.00' in web_contract['text'],
    'network_responses_200': bool(web_contract['responses']) and all(x.get('status') == 200 for x in web_contract['responses']),
    'console_errors_0': len(web_contract['consoleErrors']) == 0,
    'exceptions_0': len(web_contract['exceptions']) == 0,
    'no_overflow_all_viewports': all(x['docHorizontalOverflow'] is False for x in web_contract['viewports']),
}
native_checks = {x['id']: bool(x['pass']) for x in native.get('checks', [])}
batch_checks = {
    'single_detail_load_call': batch.count('findByChange_Contract_ContractIdOrderByChangeItemIdAsc') >= 2,
    'shared_list_for_contract_sources': 'source(contract, line, null, changeItems)' in batch,
    'shared_list_for_effective_sources': 'source(contract, null, item, changeItems)' in batch,
    'repository_free_batch_test': 'verifyNoInteractions(repository)' in batch,
    'loaded_once_test': 'verify(itemRepository, times(1))' in batch,
    'targeted_tests_green': 'Tests run: 11, Failures: 0, Errors: 0, Skipped: 0' in batch,
}
targeted_checks = {'all_targeted_tests': 'Tests run: 11, Failures: 0, Errors: 0, Skipped: 0' in target_log and 'BUILD SUCCESS' in target_log}
full_checks = {'full_backend_suite': 'Tests run: 1186, Failures: 0, Errors: 0, Skipped: 0' in full_log and 'BUILD SUCCESS' in full_log}
command_checks = {
    'provider_response_check': 'findings=0' in (raw/'provider-response-audit.log').read_text(),
    'provider_response_self_test': 'self-test: PASS' in (raw/'provider-response-self-test.log').read_text(),
    'frontend_provider_contract': 'unmatched=0' in (raw/'frontend-provider-contract-check.log').read_text() and 'PASS' in (raw/'frontend-provider-contract-check.log').read_text(),
    'web_build': '✓ built in' in (raw/'web-build.log').read_text(),
    'app_type_check': (raw/'app-type-check.log').exists(),
    'app_ui_check': 'UI 统一化规范检查通过' in (raw/'app-ui-check.log').read_text(),
    'app_pagination_check': '通过：没有新增大页固定值' in (raw/'app-pagination-check.log').read_text(),
    'app_contract_line_gate': '合同行分页契约门禁通过' in (raw/'app-contract-line-gate.log').read_text(),
    'app_contract_line_gate_self_test': 'PASS（5/5' in (raw/'app-contract-line-gate-self-test.log').read_text(),
    'app_range_gate': '合同来源分页门禁通过' in (raw/'app-range-gate.log').read_text(),
    'app_range_gate_self_test': 'PASS（4/4' in (raw/'app-range-gate-self-test.log').read_text(),
    'app_build_mp_weixin': 'DONE  Build complete.' in (raw/'app-mp-build.log').read_text(),
    'app_subpackages': '分包规范检查通过' in (raw/'app-subpackages-check.log').read_text(),
    'app_build_mp_weixin_8092_dev_mode': 'DONE  Build complete.' in (raw/'app-mp-build-8092-dev-mode.log').read_text() and 'http://127.0.0.1:8092/api/' in (Path('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/dist/build/mp-weixin/config/index.js')).read_text(),
}
integrity_checks = {
    'db_counts_checksums_fingerprints_unchanged': integrity['dbUnchanged'],
    'tracked_worktree_status_unchanged': integrity['trackedWorktreeStatusUnchanged'],
}
cleanup_checks = {
    'existing_8090_preserved': 'java    34129' in cleanup_text and '*:8090' in cleanup_text,
    'own_8092_stopped': '*:8092' not in cleanup_text,
    'own_8081_stopped': '127.0.0.1:8081' not in cleanup_text,
    'own_9430_stopped': '127.0.0.1:9430' not in cleanup_text,
}

categories = []
def category(cid, label, mapping, detail=None):
    passed = sum(1 for v in mapping.values() if v)
    failed = len(mapping) - passed
    categories.append({'id':cid,'label':label,'passed':passed,'failed':failed,'total':len(mapping),'checks':mapping,'detail':detail})

category('API_BLACKBOX','DQ-X8/X10/X11 API black-box assertions',{x['id']:bool(x['pass']) for x in api['results']},'Detailed results: raw/api-acceptance-results.json')
category('WEB_RANGE','Web real-page change source flow',web_range_checks)
category('WEB_CONTRACT','Web contract archive regression',web_contract_checks)
category('NATIVE_RANGE','WeChat native change source flow',native_checks)
category('BATCH_SNAPSHOT_CONTRACT','Batch snapshot code contract',batch_checks)
category('BACKEND_TARGETED','Range targeted tests',targeted_checks)
category('GATES_AND_BUILDS','Provider/Web/App gates and builds',command_checks)
category('BACKEND_FULL','Backend full test suite',full_checks)
category('DATA_INTEGRITY','Read-only data and source integrity',integrity_checks)
category('PROCESS_CLEANUP','Owned process cleanup and existing-process preservation',cleanup_checks)

total = sum(c['total'] for c in categories)
passed = sum(c['passed'] for c in categories)
failed = sum(c['failed'] for c in categories)
outcome = 'PASS' if failed == 0 else 'FAIL'

defect = {
    'id':'DQ-X11-P1-RANGE-PAGINATION',
    'severity':'P1',
    'status':'OPEN',
    'title':'合同变更来源相邻页遗漏部分原合同行并重复已生效目标',
    'scope':'project/contract-change/range',
    'expected':'size=2 连续翻页应覆盖 total=7，7 条来源且无重复；相邻页不应跳过来源。',
    'actual':'页1返回2条、页2返回2条、页3返回1条、页4返回1条；连续遍历仅6条且5个唯一ID，遗漏两条原合同行，已生效目标在页3/页4重复。',
    'evidence':['raw/range-pagination-defect-evidence.json','raw/api/008-range-size2-page1.json','raw/api/009-range-size2-page2.json','raw/api/010-range-size2-page3.json','raw/api/011-range-size2-page4.json'],
    'impact':'当前7来源小于默认20条，默认页面未触发；一旦来源数超过一页，选择器会漏项并重复展示，属于正式分页契约缺陷。',
    'observation':'Range.java 使用全局 offset 作为 PageRequest 的零基页码（line 170/174/177）；此处仅作定位线索，本轮未修改实现。'
}

result = {
    'schemaVersion':1,
    'runId':'20261031-dq-x8-x11-independent-acceptance',
    'generatedAt':datetime.now(timezone.utc).astimezone().isoformat(timespec='seconds'),
    'outcome':outcome,
    'passed':passed,
    'failed':failed,
    'total':total,
    'severityCounts':{'P0':0,'P1':1 if failed else 0,'P2':0},
    'defects':[defect] if failed else [],
    'categories':categories,
    'uncovered':[
        '业主身份 customer/contract-line/page 的直接黑盒未执行：未提供业主登录凭证，且本轮禁止创建临时账号；DQ-X9 仅由全量后端测试与 App 契约门禁间接回归。',
        '未做视觉签字：截图已保存，但当前执行环境不能读取图片内容；仅以 DOM、请求、网络和溢出指标判定。',
        '真实数据默认只有7个来源，Web/原生页面未触发服务端翻页器；分页缺陷通过 size=2 API 连续翻页定位。'
    ],
    'limits':[
        '未发布生产，未上传/提审/发布微信小程序正式版。',
        '8090 为验收前已有进程，全程未终止；本轮使用隔离的8092/8081/9430。',
        '验收期间观察到其他未跟踪文档并发出现，未改动或删除。'
    ],
    'evidence':[
        'INDEPENDENT-ACCEPTANCE.md',
        'raw/db-baseline.txt','raw/db-after.txt','raw/integrity-comparison.json','raw/domain-state-readonly.txt',
        'raw/api-acceptance-results.json','raw/api/',
        'raw/range-pagination-defect-evidence.json','raw/web-dom-network-check.json','raw/web-contract-page-check.json','raw/web-network-console-audit.json',
        'raw/app-native-range-check.json','raw/app-native-console.log','raw/batch-snapshot-contract.txt',
        'raw/backend-targeted-tests.log','raw/backend-full-tests.log','raw/process-cleanup.txt',
        'raw/provider-response-audit.log','raw/provider-response-self-test.log','raw/frontend-provider-contract-check.log',
        'raw/web-build.log','raw/app-type-check.log','raw/app-ui-check.log','raw/app-pagination-check.log','raw/app-contract-line-gate.log','raw/app-contract-line-gate-self-test.log','raw/app-range-gate.log','raw/app-range-gate-self-test.log','raw/app-mp-build.log','raw/app-subpackages-check.log','raw/app-mp-build-8092-dev-mode.log',
        'screenshots/web-change-create-initial-1440.png','screenshots/web-change-source-material-1440.png','screenshots/web-change-source-material-1280.png','screenshots/web-change-source-material-1024.png','screenshots/web-contract-lines-1440.png','screenshots/web-contract-lines-1280.png','screenshots/web-contract-lines-1024.png','screenshots/app-range-native-material.png'
    ]
}
(run/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:result[k] for k in ['outcome','passed','failed','total','severityCounts']},ensure_ascii=False,indent=2))
print(json.dumps([{k:c[k] for k in ['id','passed','failed','total']} for c in categories],ensure_ascii=False,indent=2))
