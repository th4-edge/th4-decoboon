import automator from 'file:///Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
import path from 'node:path'
const RUN='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261008-昭青装饰-20261008-昭青装饰-合同签约发起确认生效-微信沉浸式验收'
const API='http://127.0.0.1:8090/api'
const r=await fetch(`${API}/auth/session/password-login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({phone:'19900000002',password:'123456'})})
const d=await r.json(); const token=d.data.token
const mp=await automator.connect({wsEndpoint:'ws://127.0.0.1:9420'})
try{await mp.reLaunch(`/pages/auth/login?devToken=${encodeURIComponent(token)}`);await new Promise(r=>setTimeout(r,1800));await mp.reLaunch('/subpackages/finance/pages/project?projectId=233710289445453824');await new Promise(r=>setTimeout(r,2200));await mp.screenshot({path:path.join(RUN,'raw/screenshots/S05b-finance-project.png')});console.log(JSON.stringify({path:(await mp.currentPage()).path}))}finally{try{await mp.disconnect()}catch{}}
