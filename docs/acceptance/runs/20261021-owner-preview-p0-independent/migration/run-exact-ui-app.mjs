import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import fs from 'node:fs'
const require=createRequire(import.meta.url)
const automator=require(resolve(process.cwd(),'node_modules/miniprogram-automator/out/index.js'))
const OUT='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261021-owner-preview-p0-independent/migration'
const API='http://127.0.0.1:18092/api/'; const PROJECT='235520576473530368'; const wait=ms=>new Promise(r=>setTimeout(r,ms)); const checks=[]
const check=(id,pass,detail)=>{checks.push({id,pass:!!pass,detail});console.log(`${pass?'PASS':'FAIL'}\t${id}\t${detail}`)}
async function login(phone){const r=await fetch(API+'auth/session/password-login',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({phone,password:'Zdq123456'})});const b=await r.json();if(!b.success)throw new Error('login '+phone);return b.data.token}
async function rel(mp,url,frag){try{await mp.evaluate(u=>wx.reLaunch({url:u}),url)}catch{};for(let i=0;i<50;i++){await wait(250);const p=await mp.currentPage();if(p.path.includes(frag))return p}throw new Error('wait '+frag)}
async function texts(page,sel){const out=[];for(const e of await page.$$(sel))out.push((await e.text()).trim());return out.filter(Boolean)}
let mp;for(let i=0;i<30;i++){try{mp=await automator.connect({wsEndpoint:'ws://127.0.0.1:9422'});break}catch{await wait(500)}}if(!mp)throw new Error('automator connect')
try{
 const accounts={boss:'13910000100',storeManager:'13910000110',teamLeader:'13910000109',designer:'13910000111',pm:'13910000113',finance:'13910000114',marketer:'13910000112'}
 for(const role of ['boss','storeManager','teamLeader','designer','pm']){
  await rel(mp,`/pages/auth/login?devToken=${encodeURIComponent(await login(accounts[role]))}`,'pages/index/index')
  const page=await rel(mp,`/subpackages/project/pages/detail?id=${PROJECT}`,'project/pages/detail');await wait(3500)
  const entries=await texts(page,'.pd__hero-meta-item');check('ENTRY-'+role,entries.some(x=>x.includes('业主视角')),role+' entry visible')
 }
 for(const role of ['finance','marketer']){
  await rel(mp,`/pages/auth/login?devToken=${encodeURIComponent(await login(accounts[role]))}`,'pages/index/index')
  const page=await rel(mp,`/subpackages/project/pages/detail?id=${PROJECT}`,'project/pages/detail');await wait(3500)
  const entries=await texts(page,'.pd__hero-meta-item');check('ENTRY-'+role+'-HIDDEN',!entries.some(x=>x.includes('业主视角')),role+' entry hidden')
 }
 await rel(mp,`/pages/auth/login?devToken=${encodeURIComponent(await login(accounts.boss))}`,'pages/index/index')
 let page=await rel(mp,`/subpackages/project/pages/detail?id=${PROJECT}`,'project/pages/detail');await wait(3500);page=await mp.currentPage()
 let tapped=false;for(const x of await page.$$('.pd__hero-meta-item')){if((await x.text()).includes('业主视角')){await x.tap();tapped=true;break}}
 if(!tapped)throw new Error('boss entry missing')
 for(let i=0;i<50;i++){await wait(250);page=await mp.currentPage();if(page.path.includes('project/pages/journey'))break}
 await wait(2500);page=await mp.currentPage();const root=await page.$('.journey');const text=root?await root.text():''
 check('ROUTE-REUSES-OWNER-JOURNEY',page.path.includes('project/pages/journey'),page.path)
 check('OWNER-PAGE-STRUCTURE',text.includes('王芳（造数1100）')&&text.includes('量房')&&text.includes('设计')&&text.includes('报价')&&text.includes('验收'),'owner journey sections rendered')
 check('PREVIEW-BANNER',text.includes('业主视角预览 · 只读')&&text.includes('不代表业主已查看或确认'),'preview banner')
 check('PREVIEW-WATERMARK',text.includes('业主视角预览 · 仅内部使用 · 造数M负责人 · 工号'),'fixed watermark')
 check('NO-OWNER-ACTIONS',!text.includes('待我处理'),'employee preview has no owner action list')
 check('NO-OWNER-SHARE',!text.includes('把进度分享给家人')&&!text.includes('生成亲友观看链接'),'employee preview has no owner share card')
 check('SAME-TEAM',text.includes('为你家服务的人')&&text.includes('A 总店设计'),'owner team block reused')
 const opTabs=await page.$$('.op__tab');check('NO-PREVIEW-ONLY-TABS',opTabs.length===0,'no separate three-tab preview UI')
 await mp.screenshot({path:OUT+'/55-exact-ui-owner-page-in-preview.png'})
 fs.writeFileSync(OUT+'/exact-ui-app-results.json',JSON.stringify({checks,path:page.path,text},null,2)+'\n')
 fs.writeFileSync(OUT+'/exact-ui-app-summary.txt',checks.map(x=>`${x.pass?'PASS':'FAIL'}\t${x.id}\t${x.detail}`).join('\n')+'\n')
 if(checks.some(x=>!x.pass))process.exitCode=1
}finally{if(mp)mp.disconnect()}
