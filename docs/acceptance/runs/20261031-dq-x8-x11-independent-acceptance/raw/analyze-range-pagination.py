import json, subprocess, os
from pathlib import Path
root=Path('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261031-dq-x8-x11-independent-acceptance/raw/api')
def q(sql):
    p=subprocess.run(['mysql','-h127.0.0.1','-uroot','-N','-B','decoboon','-e',sql],capture_output=True,text=True,check=True,env={**os.environ,'MYSQL_PWD':os.environ.get('MYSQL_PWD','')})
    return [line.split('\t') for line in p.stdout.strip().splitlines() if line]
line_ids=[r[0] for r in q("SELECT line_id FROM db_contract_line WHERE contract_id=241939522005237760 ORDER BY sort,line_id")]
effective_ids=[r[0] for r in q("SELECT ci.change_item_id FROM db_contract_change_item ci JOIN db_contract_change c ON c.change_id=ci.change_id WHERE c.contract_id=241939522005237760 AND c.status='CustomerConfirmed' ORDER BY ci.change_item_id")]
expected=line_ids+effective_ids
pages={}
for p in range(1,5):
    files=list(root.glob(f'*-range-size2-page{p}.json'))
    if len(files)!=1: raise RuntimeError((p,files))
    data=json.loads(files[0].read_text())['response']['json']['data']
    ids=[row.get('sourceLineId') or row.get('sourceChangeItemId') for row in data['sources']]
    pages[p]={'ids':ids,'count':len(ids),'hasMore':data['hasMore'],'total':int(data['total'])}
actual=[x for p in range(1,5) for x in pages[p]['ids']]
seen=[]; duplicates=[]
for x in actual:
    if x in seen and x not in duplicates: duplicates.append(x)
    seen.append(x)
missing=[x for x in expected if x not in actual]
report={'expectedOrder':expected,'pages':pages,'actualTraversal':actual,'missing':missing,'duplicates':duplicates,'traversalCount':len(actual),'uniqueCount':len(set(actual)),'passed':actual==expected}
out=root.parent/'range-pagination-defect-evidence.json'
out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False,indent=2))
raise SystemExit(0 if report['passed'] else 1)
