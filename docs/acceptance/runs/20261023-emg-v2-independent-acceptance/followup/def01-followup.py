import json, subprocess, time, os
from pathlib import Path
E=Path('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261023-emg-v2-independent-acceptance/followup')
BASE='http://127.0.0.1:8090/api/'
PASSWORD=os.environ.get('EMG_ACCEPT_PASSWORD')
if not PASSWORD: raise SystemExit('missing EMG_ACCEPT_PASSWORD')

def api(route, body, token=None):
    c=['curl','-sS','-X','POST',BASE+route,'-H','Content-Type: application/json']
    if token: c += ['-H','token: '+token]
    c += ['-d',json.dumps(body,ensure_ascii=False)]
    return json.loads(subprocess.check_output(c,text=True))

def sql(q):
    return subprocess.check_output(['mysql','--socket=/tmp/syzd-mysql.sock','-uroot','-D','decoboon_accept','--batch','--raw','-e',q],text=True)

def save(name,obj): (E/'http'/name).write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n')

def seat_snapshot(label):
    q="""SELECT
      (SELECT COUNT(*) FROM db_tenant_employee WHERE com_id=1001 AND status='Active') AS active_seats,
      (SELECT COUNT(*) FROM db_tenant_invite WHERE com_id=1001 AND status='Pending' AND expire_time > NOW()) AS pending_invites,
      (SELECT COUNT(*) FROM db_tenant_employee WHERE com_id=1001 AND status='Active') + (SELECT COUNT(*) FROM db_tenant_invite WHERE com_id=1001 AND status='Pending' AND expire_time > NOW()) AS used_seats;"""
    out=sql(q); (E/'sql'/f'{label}.txt').write_text(out); return out

login_b=api('auth/session/password-login',{'phone':'13990000001','password':PASSWORD})
login_m=api('auth/session/password-login',{'phone':'13990000002','password':PASSWORD})
bt=login_b['data']['token']; mt=login_m['data']['token']

result={'at':time.strftime('%Y-%m-%dT%H:%M:%S%z'),'steps':{},'pass':False}
result['steps']['baseline']={'seats':seat_snapshot('def01-baseline-seats')}
# Manager creates and cancels own invitation.
create1=api('invite/invitation/save',{'inviteName':'独立复验店长取消','invitePhone':'13994000003','role':'Finance'},mt)
save('create-manager-own.json',{'route':'invite/invitation/save','response':create1})
id1=(create1.get('data') or {}).get('inviteId')
result['steps']['manager_create']={'success':create1.get('success'),'inviteId':id1,'seats':seat_snapshot('def01-after-manager-create-seats')}
cancel1=api('invite/invitation/cancel',{'inviteId':id1,'reason':'独立复验：店长取消自己的邀请'},mt)
save('cancel-manager-own.json',{'route':'invite/invitation/cancel','response':cancel1})
db1=sql(f"SELECT invite_id,status,store_id,com_id,inviter_id,cancelled_at FROM db_tenant_invite WHERE invite_id={id1};")
(E/'sql'/'manager-cancel-db.txt').write_text(db1)
result['steps']['manager_cancel']={'success':cancel1.get('success'),'code':(cancel1.get('errorCode') or {}).get('code'),'db':db1,'seats':seat_snapshot('def01-after-manager-cancel-seats')}
# Manager creates another invitation; BOSS cancels it.
create2=api('invite/invitation/save',{'inviteName':'独立复验BOSS取消','invitePhone':'13994000004','role':'Finance'},mt)
save('create-manager-boss-cancel.json',{'route':'invite/invitation/save','response':create2})
id2=(create2.get('data') or {}).get('inviteId')
result['steps']['boss_create']={'success':create2.get('success'),'inviteId':id2,'seats':seat_snapshot('def01-after-boss-target-create-seats')}
cancel2=api('invite/invitation/cancel',{'inviteId':id2,'reason':'独立复验：BOSS取消店长邀请'},bt)
save('cancel-by-boss.json',{'route':'invite/invitation/cancel','response':cancel2})
db2=sql(f"SELECT invite_id,status,store_id,com_id,inviter_id,cancelled_at FROM db_tenant_invite WHERE invite_id={id2};")
(E/'sql'/'boss-cancel-db.txt').write_text(db2)
result['steps']['boss_cancel']={'success':cancel2.get('success'),'code':(cancel2.get('errorCode') or {}).get('code'),'db':db2,'seats':seat_snapshot('def01-after-boss-cancel-seats')}
def seat_values(text):
    return [int(x) for x in text.strip().splitlines()[-1].split('\t')]
result['pass']=all([
 create1.get('success'), not (cancel1.get('errorCode') or {}).get('code'), 'Cancelled' in db1,
 create2.get('success'), not (cancel2.get('errorCode') or {}).get('code'), 'Cancelled' in db2,
 seat_values(result['steps']['manager_cancel']['seats']) == [2,0,2],
 seat_values(result['steps']['boss_cancel']['seats']) == [2,0,2],
])
(E/'def01-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(result,ensure_ascii=False,indent=2))
