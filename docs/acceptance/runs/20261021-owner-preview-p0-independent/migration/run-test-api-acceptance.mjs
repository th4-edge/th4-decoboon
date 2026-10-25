import fs from 'node:fs'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const BASE='http://127.0.0.1:18092/api/'
const CNF='/tmp/th4-owner-preview-acceptance.cnf'
const DB='decoboon-test'
const PROJECT='235520576473530368'
const OUT=fileURLToPath(new URL('.', import.meta.url))
const PASSWORD='Zdq123456'
const checks=[]
const previews={}
const accounts={
 boss:'13910000100',storeManager:'13910000110',teamLeader:'13910000109',designer:'13910000111',pm:'13910000113',
 finance:'13910000114',marketer:'13910000112',nonMemberDesigner:'13910000121',crossCompanyBoss:'13910000200',
 otherStoreManager:'13910000120',otherTeamLeader:'13910000119'
}
function sql(q){const r=spawnSync('mysql',['--defaults-extra-file='+CNF,'--batch','--skip-column-names',DB,'-e',q],{encoding:'utf8'});if(r.status!==0)throw new Error(r.stderr);return r.stdout.trim()}
async function post(path,body={},token=''){const r=await fetch(BASE+path,{method:'POST',headers:{'content-type':'application/json',...(token?{token}:{})},body:JSON.stringify(body)});return {http:r.status,body:await r.json()}}
function check(id,pass,detail){checks.push({id,pass:!!pass,detail});console.log(`${pass?'PASS':'FAIL'}\t${id}\t${detail}`)}
function walk(v,visitor,p='$'){visitor(v,p);if(Array.isArray(v))v.forEach((x,i)=>walk(x,visitor,`${p}[${i}]`));else if(v&&typeof v==='object')Object.entries(v).forEach(([k,x])=>walk(x,visitor,`${p}.${k}`))}
const before=sql("CHECKSUM TABLE db_project,db_project_stage,db_project_task,db_acceptance,db_finance_receivable,db_finance_receipt,cust_customer,cust_customer_access,cust_project_share_link")
const beforeCounts=sql("select 'access',count(*) from cust_customer_access union all select 'share',count(*) from cust_project_share_link where project_id="+PROJECT)
const tokens={}
for(const [k,phone] of Object.entries(accounts)){
 const r=await post('auth/session/password-login',{phone,password:PASSWORD});tokens[k]=r.body?.data?.token||'';check('LOGIN-'+k,!!tokens[k],`${phone} login http=${r.http}`)
}
if(Object.values(tokens).some(x=>!x))throw new Error('login failed')
for(const role of ['boss','storeManager','teamLeader','designer','pm']){
 const r=await post('project/owner-preview/info',{projectId:PROJECT},tokens[role]);previews[role]=r.body?.data
 const d=r.body?.data||{};const sections=(d.sections||[]).map(x=>x.key);let bad='';walk(d,(v,p)=>{if(!bad&&/(^|\.)(cost|costPrice|supplierPrice|purchasePrice|profit|internalRemark|supplier)(\.|\[|$)/i.test(p))bad=p})
 check('ALLOW-'+role,r.body?.success===true&&d.projectId===PROJECT,'project preview')
 check('ACTIONS-'+role,Array.isArray(d.actions)&&d.actions.length===0,'actions=[]')
 check('NO-FEEDBACK-'+role,!sections.includes('feedback'),sections.join(','))
 check('META-'+role,d.previewMeta?.mode==='EmployeeOwnerPreview'&&d.previewMeta?.readOnly===true,'previewMeta readOnly')
 check('WATERMARK-'+role,typeof d.previewMeta?.watermark==='string'&&d.previewMeta.watermark.startsWith('业主视角预览 · 仅内部使用 · '),d.previewMeta?.watermark||'missing')
 check('NO-INTERNAL-'+role,!bad,bad||'no internal keys')
}
const ref=structuredClone(previews.boss||{});delete ref.previewMeta
let diff='';for(const r of ['storeManager','teamLeader','designer','pm']){const c=structuredClone(previews[r]||{});delete c.previewMeta;if(JSON.stringify(c)!==JSON.stringify(ref))diff+=r+';'}
check('FIELD-CONSISTENCY',!diff,diff||'all allowed roles same projection')
for(const role of ['finance','marketer']){const r=await post('project/owner-preview/info',{projectId:PROJECT},tokens[role]);check('DENY-'+role,r.body?.success===false&&r.body?.errorCode?.code==='403',r.body?.errorCode?.code||'')}
for(const role of ['crossCompanyBoss','otherStoreManager','otherTeamLeader','nonMemberDesigner']){const r=await post('project/owner-preview/info',{projectId:PROJECT},tokens[role]);check('DENY-SCOPE-'+role,r.body?.success===false&&r.body?.errorCode?.code==='404',r.body?.errorCode?.code||'')}
for(const [id,path,body] of [
 ['OWNER-ENDPOINT','customer/project/journey/info',{projectId:PROJECT}],
 ['ACCEPT-WRITE','customer/acceptance/update',{acceptanceId:1,confirmed:true}],
 ['SHARE-WRITE','customer/sharelink/save',{projectId:PROJECT,scope:'Family',showAmount:false}],
 ['FEEDBACK-WRITE','customer/feedback/save',{projectId:PROJECT,kind:'Opinion',type:'Other',title:'test',content:'test'}]
]){const r=await post(path,body,tokens.boss);check(id,r.body?.success===false&&r.body?.errorCode?.code==='403',r.body?.errorCode?.code||'')}
sql("update db_tenant_employee set status='Resigned',resigned_at=now(6) where emp_id=999200000000000029")
{const r=await post('project/owner-preview/info',{projectId:PROJECT},tokens.pm);check('DENY-RESIGNED',r.body?.success===false,r.body?.errorCode?.code||'')}
sql("update db_tenant_employee set status='Active',resigned_at=NULL where emp_id=999200000000000029")
const after=sql("CHECKSUM TABLE db_project,db_project_stage,db_project_task,db_acceptance,db_finance_receivable,db_finance_receipt,cust_customer,cust_customer_access,cust_project_share_link")
const afterCounts=sql("select 'access',count(*) from cust_customer_access union all select 'share',count(*) from cust_project_share_link where project_id="+PROJECT)
check('NO-SIDE-EFFECT',before===after,'business table checksums unchanged')
check('NO-CUSTOMER-ACCESS',beforeCounts.includes('access\t0')&&afterCounts.includes('access\t0'),'CustomerAccess unchanged')
check('NO-SHARE-TOKEN',beforeCounts.includes('share\t0')&&afterCounts.includes('share\t0'),'ShareLink unchanged')
const audit=sql("select employee_role,result,count(*) from db_project_owner_preview_log group by employee_role,result order by employee_role,result")
const cols=sql("select column_name from information_schema.columns where table_schema='"+DB+"' and table_name='db_project_owner_preview_log' order by ordinal_position")
check('AUDIT-SUCCESS',/Boss\tSuccess/.test(audit),audit)
check('AUDIT-DENY',/(Finance|Marketer)\tForbidden/.test(audit),audit)
check('AUDIT-PRIVACY',!['click','duration','stay','path','scroll','content','frequency','trajectory','read','viewed'].some(x=>cols.toLowerCase().includes(x)),cols)
const result={checks,audit,before,after,beforeCounts,afterCounts}
fs.writeFileSync(OUT+'test-api-results.json',JSON.stringify(result,null,2)+'\n')
fs.writeFileSync(OUT+'test-api-summary.txt',checks.map(x=>`${x.pass?'PASS':'FAIL'}\t${x.id}\t${x.detail}`).join('\n')+'\n')
if(checks.some(x=>!x.pass))process.exitCode=1
