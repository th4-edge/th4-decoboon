#!/usr/bin/env python3
import csv, io, json, subprocess
from datetime import datetime
from pathlib import Path
import requests
ROOT=Path(__file__).resolve().parents[5]
RUN=ROOT/'docs/acceptance/runs/RUN-20261101-ACC-P0-001'
API='http://127.0.0.1:8090/api'; PASSWORD='Audit2026!'; PROJECT='242541620656865280'; STAGE='242541620686225408'; ACCEPTANCE='242559592377090048'; ROUND='242559592389672960'; FIXTURE_PROJECT='242297231061811200'; FIXTURE_ACCEPTANCE='242297272967102464'

def login(phone):
 r=requests.post(API+'/auth/session/password-login',json={'phone':phone,'password':PASSWORD},timeout=20); b=r.json();
 if not b.get('success'): raise RuntimeError(b)
 return b['data']['token']
def provider(token,route,payload):
 r=requests.post(API+'/'+route,headers={'token':token,'Content-Type':'application/json'},json=payload,timeout=30);return {'httpStatus':r.status_code,'body':r.json()}
def sql(q):
 r=subprocess.run(['mysql','-h127.0.0.1','-uroot','-pmysqladmin','-Ddecoboon','--batch','--raw','-e',q],check=True,capture_output=True,text=True); rows=list(csv.reader(io.StringIO(r.stdout),delimiter='\t'));return [dict(zip(rows[0],x)) for x in rows[1:]] if rows else []
def write(rel,obj):
 p=RUN/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
pm=login('13910002006')
apis={
 'acceptancePage':provider(pm,'project/acceptance/page',{'projectId':PROJECT}),
 'acceptanceInfo':provider(pm,'project/acceptance/info',{'stageId':STAGE}),
 'roundPage':provider(pm,'project/acceptance/round/page',{'acceptanceId':ACCEPTANCE}),
 'taskPage':provider(pm,'project/task/page',{'projectId':PROJECT}),
 'stagePage':provider(pm,'project/stage/page',{'projectId':PROJECT}),
 'projectInfo':provider(pm,'project/project/info',{'projectId':PROJECT}),
 'fixtureAcceptanceInfo':provider(pm,'project/acceptance/info',{'stageId':'242297231078588417'}),
}
db={
 'acceptance':sql(f"select acceptance_id,project_id,stage_id,status,conclusion,confirmed_at,confirmed_by,owner_id,emp_id,conclusion from db_acceptance where acceptance_id in ({ACCEPTANCE},{FIXTURE_ACCEPTANCE})"),
 'rounds':sql(f"select round_id,acceptance_id,round_no,result,conclusion,submitted_at,confirmed_at,confirmed_by_id,submitted_by_id from db_acceptance_round where acceptance_id in ({ACCEPTANCE},{FIXTURE_ACCEPTANCE}) order by acceptance_id,round_no"),
 'acceptance_evidence':sql(f"select acceptance_id,url from db_acceptance_evidence where acceptance_id in ({ACCEPTANCE},{FIXTURE_ACCEPTANCE})"),
 'round_evidence':sql(f"select round_id,url from db_acceptance_round_evidence where round_id in (select round_id from db_acceptance_round where acceptance_id in ({ACCEPTANCE},{FIXTURE_ACCEPTANCE}))"),
 'tasks':sql(f"select task_id,name,status,progress,actual_start,actual_end from db_project_task where project_id={PROJECT} order by task_id"),
 'task_evidence':sql(f"select task_id,url from db_project_task_evidence where task_id in (select task_id from db_project_task where project_id={PROJECT})"),
 'stage':sql(f"select stage_id,name,status,progress,actual_start,actual_end from db_project_stage where stage_id={STAGE}"),
 'project':sql(f"select project_id,status,progress from db_project where project_id={PROJECT}"),
 'fixture_access_count':sql(f"select count(*) access_count from cust_customer_access where project_id={FIXTURE_PROJECT}"),
}
base={'capturedAt':datetime.now().astimezone().isoformat(timespec='seconds'),'role':'Pm','projectId':PROJECT,'stageId':STAGE,'acceptanceId':ACCEPTANCE,'provider':apis,'db':db}
base['checks']={
 'oneAcceptanceOneRound':len([x for x in db['acceptance'] if x['project_id']==PROJECT])==1 and len([x for x in db['rounds'] if x['acceptance_id']==ACCEPTANCE])==1,
 'pendingUnconfirmed':next(x for x in db['acceptance'] if x['acceptance_id']==ACCEPTANCE)['status']=='Pending' and next(x for x in db['acceptance'] if x['acceptance_id']==ACCEPTANCE)['confirmed_at']=='NULL' and next(x for x in db['acceptance'] if x['acceptance_id']==ACCEPTANCE)['confirmed_by']=='NULL',
 'taskDone':any(x['status']=='Done' for x in db['tasks']),
 'taskEvidenceBound':len(db['task_evidence'])>0,
 'acceptanceEvidenceBound':any(x['acceptance_id']==ACCEPTANCE for x in db['acceptance_evidence']),
 'roundEvidenceBound':any(x['round_id']==ROUND for x in db['round_evidence']),
 'stageAccepting':db['stage'][0]['status']=='Accepting',
 'fixtureStillPending':any(x['acceptance_id']==FIXTURE_ACCEPTANCE and x['status']=='Pending' for x in db['acceptance']),
 'fixtureNoOwnerAccess':db['fixture_access_count'][0]['access_count']=='0',
}
write('evidence/TC-ACC-PM-SUBMIT-001/17-acceptance-provider-db-corrected.json',base)
write('evidence/TC-ACC-PERMISSION-EMPLOYEE-002/12-permission-provider-db-corrected.json',base)
print(json.dumps(base['checks'],ensure_ascii=False,indent=2))
