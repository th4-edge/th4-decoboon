import automator from 'file:///Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
import fs from 'node:fs'
import path from 'node:path'
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261008-昭青装饰-20261008-昭青装饰-合同签约发起确认生效-微信沉浸式验收'
const API = 'http://127.0.0.1:8090/api'
async function login(phone) { const r = await fetch(`${API}/auth/session/password-login`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({phone,password:'123456'}) }); const d=await r.json(); if(!d.success) throw new Error('login'); return d.data.token }
async function allText(page){const out=[];for(const el of await page.$$('*')){const t=(await el.text())?.trim();if(t)out.push(t)}return [...new Set(out)]}
const mp=await automator.connect({wsEndpoint:'ws://127.0.0.1:9420'})
try{const token=await login('19900000002');await mp.reLaunch(`/pages/auth/login?devToken=${encodeURIComponent(token)}`);await new Promise(r=>setTimeout(r,2000));await mp.reLaunch('/subpackages/finance/pages/receivables');await new Promise(r=>setTimeout(r,2500));const page=await mp.currentPage();const texts=await allText(page);await mp.screenshot({path:path.join(RUN,'raw/screenshots/S05-finance-receivables.png')});fs.writeFileSync(path.join(RUN,'raw/accessibility/S05-finance-receivables.json'),JSON.stringify({path:page.path,texts},null,2));console.log(JSON.stringify({path:page.path,hasContractAmount:texts.some(t=>t.includes('58903')||t.includes('5.89'))},null,2))}finally{try{await mp.disconnect()}catch{}}
