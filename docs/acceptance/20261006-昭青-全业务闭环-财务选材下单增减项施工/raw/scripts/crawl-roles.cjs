const automator = require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js')
const fs = require('fs'); const path = require('path')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20261006-昭青-全业务闭环-财务选材下单增减项施工'
const TOK = JSON.parse(fs.readFileSync('/private/tmp/decoboon-audit-tokens.json','utf8'))
const sleep = ms => new Promise(r=>setTimeout(r,ms))
const HOME='/pages/index/index'
const R = {
  Boss: ['/pages/customer/list','/pages/project/list','/pages/finance/index','/subpackages/finance/pages/projects','/subpackages/finance/pages/ledger','/subpackages/finance/pages/receivables','/subpackages/finance/pages/receipts','/subpackages/finance/pages/payables','/subpackages/finance/pages/payments','/subpackages/finance/pages/partners','/subpackages/finance/pages/entry','/subpackages/finance/pages/vouchers','/subpackages/finance/pages/voucher-templates','/subpackages/quote/pages/list','/subpackages/quote/pages/workbench','/subpackages/quote/pages/catalog/manage','/subpackages/quote/pages/package-manage','/subpackages/design/pages/workbench','/subpackages/design/pages/list','/subpackages/team/pages/workbench','/subpackages/team/pages/designers','/subpackages/statistic/pages/performance','/subpackages/statistic/pages/analysis','/subpackages/project/pages/change-workbench','/subpackages/project/pages/material-plan-workbench','/subpackages/project/pages/selection-board','/subpackages/project/pages/plan','/subpackages/company/pages/members','/subpackages/plan/pages/construction-list','/subpackages/plan/pages/payment-list','/subpackages/supply/pages/index','/subpackages/subscription/pages/index','/subpackages/message/pages/list','/pages/dynamic/index','/pages/mine/index'],
  Finance: ['/pages/finance/index','/subpackages/finance/pages/ledger','/subpackages/finance/pages/receivables','/subpackages/finance/pages/receipts','/subpackages/finance/pages/payables','/subpackages/finance/pages/payments','/subpackages/finance/pages/projects','/subpackages/finance/pages/partners','/subpackages/finance/pages/entry','/subpackages/finance/pages/vouchers','/subpackages/finance/pages/voucher-templates','/subpackages/supply/pages/index','/subpackages/project/pages/material-plan-workbench','/subpackages/project/pages/selection-board','/pages/project/list','/pages/dynamic/index','/pages/mine/index'],
  StoreManager: ['/pages/customer/list','/subpackages/customer/pages/measure-workbench','/subpackages/design/pages/workbench','/subpackages/team/pages/workbench','/subpackages/team/pages/designers','/pages/project/list','/subpackages/project/pages/selection-board','/subpackages/project/pages/material-plan-workbench','/subpackages/project/pages/change-workbench','/subpackages/project/pages/plan','/subpackages/company/pages/members','/pages/mine/index'],
  Marketer: ['/pages/customer/list','/subpackages/customer/pages/flow','/subpackages/customer/pages/measure-workbench','/subpackages/customer/pages/measure-record','/pages/project/list','/subpackages/project/pages/selection-board','/pages/mine/index'],
  'Designer-Dong': ['/subpackages/design/pages/list','/subpackages/design/pages/workbench','/subpackages/design/pages/requirements','/subpackages/quote/pages/list','/subpackages/quote/pages/workbench','/subpackages/project/pages/selection-board','/subpackages/project/pages/selection','/subpackages/project/pages/material-plan','/subpackages/project/pages/change-workbench','/subpackages/project/pages/change','/pages/project/list','/pages/mine/index'],
  'Designer-Xie': ['/subpackages/design/pages/list','/subpackages/design/pages/workbench','/subpackages/design/pages/requirements','/subpackages/quote/pages/list','/subpackages/quote/pages/workbench','/subpackages/project/pages/selection-board','/subpackages/project/pages/change-workbench','/pages/project/list','/pages/mine/index'],
  'Pm-Xu': ['/subpackages/team/pages/workbench','/pages/project/list','/subpackages/project/pages/plan','/subpackages/project/pages/material-plan-workbench','/subpackages/project/pages/material-plan','/subpackages/project/pages/material-category','/subpackages/project/pages/material-batch-order','/subpackages/project/pages/selection-board','/subpackages/project/pages/change-workbench','/subpackages/project/pages/change','/subpackages/project/pages/report/daily','/subpackages/project/pages/accept/edit','/pages/mine/index'],
  'Pm-Wang': ['/subpackages/team/pages/workbench','/pages/project/list','/subpackages/project/pages/plan','/subpackages/project/pages/material-plan-workbench','/subpackages/project/pages/change-workbench','/subpackages/project/pages/report/daily','/pages/mine/index'],
  TeamLeader: ['/subpackages/team/pages/list','/subpackages/team/pages/workbench','/subpackages/team/pages/designers','/pages/customer/list','/pages/project/list','/pages/mine/index']
}
const key = { Boss:'Boss', Finance:'Finance', StoreManager:'StoreManager', Marketer:'Marketer', 'Designer-Dong':'Designer-Dong', 'Designer-Xie':'Designer-Xie', 'Pm-Xu':'Pm-Xu', 'Pm-Wang':'Pm-Wang', TeamLeader:'TeamLeader' }
async function visText(page){ const els=await page.$$('*'); const v=[]; for(const el of els){try{const t=String(await el.text()).trim(); if(t) v.push(t)}catch{}} return [...new Set(v)].filter(t=>t.length<=400) }
async function main(){
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const consoleErrors=[]; mp.on('console',m=>{ if(m && (m.type==='error'||m.type==='warn')) consoleErrors.push({t:Date.now(),type:m.type,args:(m.args||[]).map(a=>String(a).slice(0,300))}) })
  try{ mp.on('exception',e=>consoleErrors.push({t:Date.now(),type:'exception',args:[JSON.stringify(e).slice(0,400)]})) }catch{}
  const summary={startedAt:new Date().toISOString(),roles:{}}
  for(const role of Object.keys(R)){
    const tok = TOK[role].token
    const rec={role,name:TOK[role].name,empId:TOK[role].empId,roleCode:TOK[role].role,pages:[],errors:[]}
    await mp.callWxMethod('clearStorageSync')
    await mp.reLaunch('/pages/auth/login?devToken='+encodeURIComponent(tok))
    await sleep(2600)
    let p=await mp.currentPage()
    rec.loginPath=p.path
    const routes=[HOME,...R[role]]
    for(let i=0;i<routes.length;i++){
      const route=routes[i]; const item={route,path:null,textLen:0,textSample:'',screenshot:null,err:null}
      try{
        await mp.reLaunch(route); await sleep(1150); p=await mp.currentPage()
        item.path=p.path
        let txt=[]; try{txt=await visText(p)}catch(e){item.err='text:'+String(e)}
        item.textLen=txt.join('|').length; item.textSample=txt.join(' ').slice(0,900)
        const out=`${role}-${String(i).padStart(2,'0')}-${route.replace(/[\/?=&]/g,'-').replace(/^-/,'')}.png`
        await mp.screenshot({path:path.join(RUN,'raw','screenshots',out)}); item.screenshot=out
      }catch(e){ item.err=String(e&&e.stack||e); rec.errors.push({route,error:item.err}) }
      rec.pages.push(item)
    }
    summary.roles[role]=rec
    fs.writeFileSync(path.join(RUN,'raw','accessibility',`role-${role}.json`),JSON.stringify(rec,null,2))
    console.log(JSON.stringify({role,pages:rec.pages.length,errors:rec.errors.length,loginPath:rec.loginPath}))
  }
  summary.finishedAt=new Date().toISOString(); summary.consoleErrors=consoleErrors.slice(0,400)
  fs.writeFileSync(path.join(RUN,'raw','console','console-and-exceptions.json'),JSON.stringify(consoleErrors,null,2))
  fs.writeFileSync(path.join(RUN,'raw','accessibility','roles-summary.json'),JSON.stringify(summary,null,2))
  try{mp.disconnect()}catch{}
}
main().catch(e=>{console.error(e&&e.stack||e);process.exit(1)})
