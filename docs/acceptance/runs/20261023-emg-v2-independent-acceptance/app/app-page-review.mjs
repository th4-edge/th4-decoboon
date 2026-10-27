import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import fs from 'node:fs'
const APP='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app'
const ROOT='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon'
const EVID=resolve(ROOT,'docs/acceptance/runs/20261023-emg-v2-independent-acceptance')
const SHOTS=resolve(EVID,'screenshots')
const require=createRequire(resolve(APP,'package.json'))
const automator=require(resolve(APP,'node_modules/miniprogram-automator/out/index.js'))
const wait=ms=>new Promise(r=>setTimeout(r,ms))
const result={at:new Date().toISOString(),automationEndpoint:'ws://127.0.0.1:9424',auth:'真实店长令牌已通过接口签发并注入 App 既有会话存储；页面运行态不依赖研发截图',steps:[],consoleErrors:[],pass:false}
let mp
async function texts(page){const out=[];for(const sel of ['.member-page','.ci-page','.ci-field__readonly','.ci-field','.ci-page text']){for(const el of await page.$$(sel)){try{const t=(await el.text()).trim();if(t)out.push(t)}catch{}}}return [...new Set(out)]}
try{
  mp=await automator.connect({wsEndpoint:'ws://127.0.0.1:9424'})
  const login=await fetch('http://127.0.0.1:8090/api/auth/session/password-login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({phone:process.env.EMG_ACCEPT_PHONE,password:process.env.EMG_ACCEPT_PASSWORD})}).then(r=>r.json())
  if(!login.success) throw new Error('店长接口登录失败')
  await mp.evaluate(t=>{const s=JSON.parse(wx.getStorageSync('jzb_session_v2')||'{}');s.token=t;wx.setStorageSync('jzb_token',t);wx.setStorageSync('jzb_session_v2',JSON.stringify(s))},login.data.token)
  mp.on('console',e=>{if(e.type==='error')result.consoleErrors.push(e)})
  mp.on('exception',e=>result.consoleErrors.push(e))
  try{await mp.navigateTo('/subpackages/company/pages/members')}catch{}
  await wait(2200)
  const page=await mp.currentPage()
  const cards=await page.$$('.member-card')
  const rows=[]
  for(const card of cards){const text=await card.text();const actions=[];for(const a of await card.$$('.member-action'))actions.push((await a.text()).trim());rows.push({text,actions})}
  const manager=rows.find(r=>r.text.includes('验收店长'))
  const boss=rows.find(r=>r.text.includes('验收老板'))
  result.steps.push({step:'members',path:page.path,rowCount:rows.length,rows,managerActions:manager?.actions||[],bossActions:boss?.actions||[]})
  await mp.screenshot({path:resolve(SHOTS,'app-manager-members-final.png')})
  let invitePage
  try{await mp.navigateTo('/subpackages/company/pages/invite');await wait(1800);invitePage=await mp.currentPage()}catch{}
  if(invitePage){
    const inviteTexts=await texts(invitePage)
    result.steps.push({step:'invite',path:invitePage.path,hasStoreManagerRole:inviteTexts.some(t=>t==='店长'),hasTeamLeaderRole:inviteTexts.some(t=>t.includes('团队负责人')),hasFinanceRole:inviteTexts.some(t=>t.includes('财务')),roleTexts:inviteTexts.filter(t=>['店长','设计师','业务员','项目经理','财务'].includes(t)||t.includes('团队负责人')||t.includes('验收总店')||t.includes('目标门店'))})
    await mp.screenshot({path:resolve(SHOTS,'app-manager-invite-roles.png')})
  }
  const members=result.steps.find(s=>s.step==='members');const invite=result.steps.find(s=>s.step==='invite')
  result.pass=Boolean(members?.path.endsWith('/members')&&members.rowCount>=2&&(members.managerActions.length===0)&&(members.bossActions.length===0)&&invite?.path.endsWith('/invite')&&!invite.hasStoreManagerRole&&!invite.hasTeamLeaderRole&&invite.hasFinanceRole&&result.consoleErrors.length===0)
}catch(e){result.error=e instanceof Error?e.message:String(e)}finally{fs.writeFileSync(resolve(EVID,'app/app-page-review.json'),JSON.stringify(result,null,2)+'\n');try{await mp?.disconnect()}catch{}}
console.log(JSON.stringify(result,null,2))
