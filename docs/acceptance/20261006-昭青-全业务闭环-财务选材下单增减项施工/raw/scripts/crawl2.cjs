const automator=require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js')
const fs=require('fs'),path=require('path'),http=require('http')
const RUN='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20261006-昭青-全业务闭环-财务选材下单增减项施工'
const ACCOUNTS=JSON.parse(fs.readFileSync('/private/tmp/decoboon-audit-tokens.json','utf8'))
const sleep=ms=>new Promise(r=>setTimeout(r,ms))
function login(phone){return new Promise((res,rej)=>{const d=Buffer.from(JSON.stringify({phone,password:'Zhuang2026Test'}));const r=http.request({host:'127.0.0.1',port:8090,path:'/api/auth/session/password-login',method:'POST',headers:{'content-type':'application/json','content-length':d.length}},x=>{let s='';x.on('data',c=>s+=c);x.on('end',()=>{try{const v=JSON.parse(s);v.success?res(v.data.token):rej(new Error(s))}catch(e){rej(new Error(s))}})});r.on('error',rej);r.write(d);r.end()})}
async function retry(fn,n=3){let e;for(let i=0;i<n;i++){try{return await fn()}catch(err){e=err;await sleep(700)}}throw e}
const R={
 Boss:['/pages/index/index','/pages/customer/list','/pages/project/list','/pages/finance/index','/subpackages/finance/pages/ledger','/subpackages/finance/pages/receivables','/subpackages/finance/pages/payables','/subpackages/finance/pages/payments','/subpackages/finance/pages/vouchers','/subpackages/project/pages/material-plan-workbench','/subpackages/project/pages/selection-board','/subpackages/project/pages/change-workbench','/subpackages/project/pages/plan','/subpackages/supply/pages/index','/subpackages/company/pages/members','/subpackages/statistic/pages/performance','/pages/mine/index'],
 Finance:['/pages/index/index','/pages/finance/index','/subpackages/finance/pages/ledger','/subpackages/finance/pages/receivables','/subpackages/finance/pages/receipts','/subpackages/finance/pages/payables','/subpackages/finance/pages/payments','/subpackages/finance/pages/projects','/subpackages/finance/pages/entry','/subpackages/finance/pages/vouchers','/subpackages/supply/pages/index','/subpackages/project/pages/material-plan-workbench','/pages/mine/index'],
 StoreManager:['/pages/index/index','/pages/customer/list','/subpackages/customer/pages/measure-workbench','/subpackages/design/pages/workbench','/subpackages/team/pages/designers','/pages/project/list','/subpackages/project/pages/selection-board','/subpackages/project/pages/material-plan-workbench','/subpackages/project/pages/change-workbench','/subpackages/project/pages/plan','/pages/mine/index'],
 Marketer:['/pages/index/index','/pages/customer/list','/subpackages/customer/pages/flow','/subpackages/customer/pages/measure-workbench','/pages/project/list','/pages/mine/index'],
 'Designer-Dong':['/pages/index/index','/subpackages/design/pages/workbench','/subpackages/design/pages/list','/subpackages/quote/pages/workbench','/subpackages/project/pages/selection-board','/subpackages/project/pages/material-plan','/subpackages/project/pages/change-workbench','/pages/project/list','/pages/mine/index'],
 'Designer-Xie':['/pages/index/index','/subpackages/design/pages/workbench','/subpackages/quote/pages/workbench','/subpackages/project/pages/selection-board','/subpackages/project/pages/change-workbench','/pages/project/list','/pages/mine/index'],
 'Pm-Xu':['/pages/index/index','/subpackages/team/pages/workbench','/pages/project/list','/subpackages/project/pages/plan','/subpackages/project/pages/material-plan-workbench','/subpackages/project/pages/material-category','/subpackages/project/pages/material-batch-order','/subpackages/project/pages/selection-board','/subpackages/project/pages/change-workbench','/subpackages/project/pages/report/daily','/pages/mine/index'],
 'Pm-Wang':['/pages/index/index','/subpackages/team/pages/workbench','/pages/project/list','/subpackages/project/pages/plan','/subpackages/project/pages/material-plan-workbench','/subpackages/project/pages/change-workbench','/subpackages/project/pages/report/daily','/pages/mine/index'],
 TeamLeader:['/pages/index/index','/subpackages/team/pages/list','/subpackages/team/pages/workbench','/pages/customer/list','/pages/project/list','/pages/mine/index']
}
async function main(){
 fs.mkdirSync(path.join(RUN,'raw','screenshots'),{recursive:true})
 const mp=await automator.connect({wsEndpoint:'ws://127.0.0.1:9420',timeout:60000})
 const cons=[];mp.on('console',m=>{if(m&&(m.type==='error'||m.type==='warn'))cons.push({t:new Date().toISOString(),type:m.type,args:(m.args||[]).map(a=>String(a).slice(0,300))})})
 const summary={startedAt:new Date().toISOString(),roles:{}}
 for(const role of Object.keys(R)){
   const acc=ACCOUNTS[role];const rec={role,name:acc.name,empId:acc.empId,roleCode:acc.role,pages:[],errors:[]}
   let token;try{token=await login(acc.phone)}catch(e){rec.errors.push({phase:'login',error:String(e)});summary.roles[role]=rec;continue}
   fs.writeFileSync('/private/tmp/decoboon-audit-tokens.json',JSON.stringify({...ACCOUNTS,[role]:{...acc,token}}))
   try{await retry(()=>mp.reLaunch('/pages/auth/login?devToken='+encodeURIComponent(token)),2);await sleep(2600);let p=await mp.currentPage();rec.loginPath=p.path}catch(e){rec.errors.push({phase:'relogin',error:String(e)})}
   for(let i=0;i<R[role].length;i++){
     const route=R[role][i];const item={route,path:null,dataKeys:'',screenshot:null,err:null}
     try{await retry(()=>mp.reLaunch(route),2);await sleep(1100);const p=await mp.currentPage();item.path=p.path
       try{const d=await p.data();item.dataKeys=Object.keys(d||{}).slice(0,24).join(',')}catch(e){item.err='data:'+String(e).slice(0,120)}
       const out=`${role}-${String(i).padStart(2,'0')}-${route.replace(/[\/?=&]/g,'-').replace(/^-/,'')}.png`
       try{await retry(()=>mp.screenshot({path:path.join(RUN,'raw','screenshots',out)}),2);item.screenshot=out}catch(e){item.err=(item.err||'')+' shot:'+String(e).slice(0,120)}
     }catch(e){item.err=String(e).slice(0,200);rec.errors.push({route,error:item.err})}
     rec.pages.push(item)
   }
   summary.roles[role]=rec
   fs.writeFileSync(path.join(RUN,'raw','accessibility',`role-${role}.json`),JSON.stringify(rec,null,2))
   console.log(JSON.stringify({role,login:rec.loginPath,pages:rec.pages.length,errs:rec.errors.length,shots:rec.pages.filter(x=>x.screenshot).length}))
 }
 summary.finishedAt=new Date().toISOString();summary.consoleErrors=cons.slice(0,400)
 fs.writeFileSync(path.join(RUN,'raw','console','console-and-exceptions.json'),JSON.stringify(cons,null,2))
 fs.writeFileSync(path.join(RUN,'raw','accessibility','roles-summary.json'),JSON.stringify(summary,null,2))
 try{mp.disconnect()}catch{}
}
main().catch(e=>{console.error(e&&e.stack||e);process.exit(1)})
