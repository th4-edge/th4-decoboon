import json
from pathlib import Path
from datetime import datetime, timezone

run=Path('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261031-dq-x8-x11-independent-acceptance')
raw=run/'raw'
initial=json.loads((raw/'result-initial-fail.json').read_text())
api=json.loads((raw/'independent-reaccept-api.json').read_text())
integrity=json.loads((raw/'reaccept-integrity.json').read_text())
new_test=(raw/'reaccept-new-consecutive-test.log').read_text()
targeted=(raw/'reaccept-backend-targeted-tests.log').read_text()
provider_response=(raw/'independent-provider-response-audit.log').read_text()
provider_self=(raw/'independent-provider-response-self-test.log').read_text()
provider_frontend=(raw/'independent-frontend-provider-contract.log').read_text()
cleanup=(raw/'reaccept-process-cleanup.txt').read_text()
full_impl=(raw/'backend-full-tests-reaccept.log').read_text()
impl_api=json.loads((raw/'reaccept-range-pages.json').read_text())

categories=[
 {'id':'REACCEPT_API_BLACKBOX','label':'Independent API reaccept','passed':api['passed'],'failed':api['failed'],'total':api['total'],'checks':{x['id']:x['pass'] for x in api['checks']}},
 {'id':'REACCEPT_NEW_TEST','label':'New consecutive-page test executed alone','passed':1 if 'Tests run: 1, Failures: 0, Errors: 0' in new_test and 'BUILD SUCCESS' in new_test else 0,'failed':0 if 'Tests run: 1, Failures: 0, Errors: 0' in new_test and 'BUILD SUCCESS' in new_test else 1,'total':1},
 {'id':'REACCEPT_TARGETED_TESTS','label':'RangeProviderTest + ContractChangeRangeContextTest','passed':12 if 'Tests run: 12, Failures: 0, Errors: 0' in targeted and 'BUILD SUCCESS' in targeted else 0,'failed':0 if 'Tests run: 12, Failures: 0, Errors: 0' in targeted and 'BUILD SUCCESS' in targeted else 12,'total':12},
 {'id':'REACCEPT_PROVIDER_AUDITS','label':'Provider response/front-end audits','passed':sum([('findings=0' in provider_response),('self-test: PASS' in provider_self),('unmatched=0' in provider_frontend and 'PASS' in provider_frontend)]),'failed':3-sum([('findings=0' in provider_response),('self-test: PASS' in provider_self),('unmatched=0' in provider_frontend and 'PASS' in provider_frontend)]),'total':3},
 {'id':'REACCEPT_WORKTREE','label':'Only expected implementation delta','passed':sum([integrity['onlyExpectedImplementationFilesChanged'],integrity['expectedFixPresent'],integrity['sourceAndDataIdenticalBeforeAfter']]),'failed':3-sum([integrity['onlyExpectedImplementationFilesChanged'],integrity['expectedFixPresent'],integrity['sourceAndDataIdenticalBeforeAfter']]),'total':3},
 {'id':'REACCEPT_CLEANUP','label':'Owned process cleanup','passed':sum(['java    34129' in cleanup and '*:8090' in cleanup,'*:8092' not in cleanup]),'failed':2-sum(['java    34129' in cleanup and '*:8090' in cleanup,'*:8092' not in cleanup]),'total':2},
]
total=sum(c['total'] for c in categories)
passed=sum(c['passed'] for c in categories)
failed=sum(c['failed'] for c in categories)
outcome='PASS' if failed==0 else 'FAIL'
closed_defect=dict(initial['defects'][0])
closed_defect.update({
 'status':'CLOSED',
 'closedAt':datetime.now(timezone.utc).astimezone().isoformat(timespec='seconds'),
 'resolution':'Range.java 改用 PageRequest.of(page - 1, pageSize, LINE_SORT)，并按 lineLimit 截取；独立 API 连续四页严格覆盖 7 个唯一来源。',
 'closureEvidence':['raw/independent-reaccept-api.json','raw/independent-reaccept-console.log','raw/reaccept-new-consecutive-test.log','raw/reaccept-backend-targeted-tests.log','raw/reaccept-integrity.json']
})
result={
 'schemaVersion':2,
 'runId':'20261031-dq-x8-x11-independent-acceptance',
 'generatedAt':datetime.now(timezone.utc).astimezone().isoformat(timespec='seconds'),
 'outcome':outcome,
 'passed':passed,
 'failed':failed,
 'total':total,
 'severityCounts':{'P0':0,'P1':0,'P2':0},
 'historicalSeverityCounts':{'P0':0,'P1':1,'P2':0},
 'defects':[closed_defect],
 'initialAcceptanceRun':{
   'outcome':initial['outcome'],'passed':initial['passed'],'failed':initial['failed'],'total':initial['total'],
   'resultFile':'raw/result-initial-fail.json','status':'SUPERSEDED_BY_INDEPENDENT_REACCEPT'
 },
 'cumulativeExecutions':{
   'total':initial['total']+total,'passed':initial['passed']+passed,
   'historicalFailed':initial['failed'],'currentFailed':failed,
   'note':'初验失败保留为历史；当前失败数为复验失败数。'
 },
 'reacceptCategories':categories,
 'supplementalImplementationEvidence':{
   'countsAsIndependentVerification':False,
   'apiReacceptFile':'raw/reaccept-range-pages.json',
   'apiPages':[p['ids'] for p in impl_api['pages']],
   'fullTestLog':'raw/backend-full-tests-reaccept.log',
   'fullTestResult':'1187/1187 PASS' if 'Tests run: 1187, Failures: 0, Errors: 0' in full_impl else 'NOT_CONFIRMED',
   'providerLogs':['raw/provider-response-audit-reaccept.log','raw/provider-response-self-test-reaccept.log','raw/frontend-provider-contract-reaccept.log']
 },
 'uncovered':initial['uncovered'],
 'limits':initial['limits']+['本轮复验聚焦后端缺陷与定向回归，未重跑 Web/原生页面；其首次验收证据保留。','全量1187/1187来自实现方证据，独立复验未重跑全量。'],
 'evidence':initial['evidence']+[
   'raw/result-initial-fail.json','raw/reaccept-db-baseline.txt','raw/reaccept-db-after.txt','raw/reaccept-integrity.json',
   'raw/run-reaccept-independent.mjs','raw/independent-reaccept-api.json','raw/independent-reaccept-console.log',
   'raw/reaccept-new-consecutive-test.log','raw/reaccept-backend-targeted-tests.log',
   'raw/independent-provider-response-audit.log','raw/independent-provider-response-self-test.log','raw/independent-frontend-provider-contract.log',
   'raw/reaccept-process-cleanup.txt'
 ]
}
(run/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'outcome':outcome,'passed':passed,'failed':failed,'total':total,'severityCounts':result['severityCounts'],'historicalSeverityCounts':result['historicalSeverityCounts'],'cumulative':result['cumulativeExecutions']},ensure_ascii=False,indent=2))
print(json.dumps([{k:c[k] for k in ['id','passed','failed','total']} for c in categories],ensure_ascii=False,indent=2))
if outcome!='PASS': raise SystemExit(1)
