import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import fs from 'node:fs'
const APP_DIR='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app'
const ROOT='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon'
const require=createRequire(resolve(APP_DIR,'package.json'))
const automator=require(resolve(APP_DIR,'node_modules/miniprogram-automator/out/index.js'))
const API='http://127.0.0.1:8090/api/'
const wait=(ms)=>new Promise(r=>setTimeout(r,ms))
async function post(route,data,token){const r=await fetch(`${API}${route}`,{method:'POST',headers:{'Content-Type':'application/json',...(token?{token}:{})},body:JSON.stringify(data)});return r.json()}
const logs=[]
let mp
for(let i=0;i<20;i++){try{mp=await automator.connect({wsEndpoint:'ws://127.0.0.1:9420'});break}catch{await wait(500)}}
if(!mp)throw new Error('无法连接 miniprogram-automator')
mp.on('console',e=>logs.push({kind:'console',...e}))
mp.on('exception',e=>logs.push({kind:'exception',...e}))
const result={at:new Date().toISOString(),apiBase:API,steps:[],pass:false}
try{
  const login=await post('auth/session/password-login',{phone:process.env.PAYROLL_FINANCE_PHONE,password:process.env.PAYROLL_TEST_PASSWORD})
  if(!login.success||!login.data?.token)throw new Error(`本地登录失败: ${JSON.stringify(login.errorCode)}`)
  try{await mp.evaluate(url=>wx.reLaunch({url}),`/pages/auth/login?devToken=${encodeURIComponent(login.data.token)}`)}catch{}
  await wait(1500)
  try{await mp.evaluate(()=>wx.reLaunch({url:'/subpackages/payroll/pages/compensation-list'}))}catch{}
  await wait(1800)
  let page=await mp.currentPage()
  const cards=await page.$$('.comp-card')
  const cardTexts=[]; for(const card of cards)cardTexts.push(await card.text())
  const successors=await page.$$('.comp-card__successor')
  const deletes=await page.$$('.comp-card__delete')
  result.steps.push({step:'list',path:page.path,cards:cards.length,cardTexts,successorActions:successors.length,deleteActions:deletes.length})
  await mp.screenshot({path:resolve(ROOT,'output/playwright/payroll-b1-independent/20-app-compensation-list.png')})
  if(successors[0]) await successors[0].tap()
  await wait(1600)
  page=await mp.currentPage()
  const successorElements=await page.$$('*')
  const successorTexts=[]
  for(const el of successorElements){try{const t=await el.text();if(t&&t.trim())successorTexts.push(t.trim())}catch{}}
  result.steps.push({step:'successor',path:page.path,texts:[...new Set(successorTexts)].filter(t=>t.includes('后续')||t.includes('吴濛濛')||t.includes('5000')||t.includes('27')||t.includes('生效日期')).slice(0,20)})
  await mp.screenshot({path:resolve(ROOT,'output/playwright/payroll-b1-independent/21-app-successor-edit.png')})
  const listOk=cards.length===5&&successors.length===5&&deletes.length===0&&cardTexts.every(t=>t.includes('已锁定')&&t.includes('新增后续方案'))
  const successorOk=page.path.includes('compensation-edit')&&successorTexts.some(t=>t.includes('原方案和既有工资事实保持不变'))&&successorTexts.some(t=>t.includes('吴濛濛'))
  result.pass=listOk&&successorOk
}catch(e){result.error=e instanceof Error?e.message:String(e)}finally{
  result.logs=logs
  result.consoleErrors=logs.filter(x=>x.kind==='exception'||x.type==='error')
  fs.writeFileSync(resolve(ROOT,'docs/acceptance/runs/20261021-payroll-b1-independent/app-runtime-locked-flow.json'),JSON.stringify(result,null,2))
  try{await mp.disconnect()}catch{}
}
console.log(JSON.stringify(result,null,2))
