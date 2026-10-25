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
  const page=await rel(mp,`/subpackages/project/pages/detail?id=${PROJECT}`,'project/pages/detail');await wait(4000)
  const entries=await texts(page,'.pd__hero-meta-item');check('ENTRY-'+role,entries.some(x=>x.includes('业主视角')),role+' owner preview entry visible')
 }
 for(const role of ['finance','marketer']){
  await rel(mp,`/pages/auth/login?devToken=${encodeURIComponent(await login(accounts[role]))}`,'pages/index/index')
  const page=await rel(mp,`/subpackages/project/pages/detail?id=${PROJECT}`,'project/pages/detail');await wait(4000)
  const entries=await texts(page,'.pd__hero-meta-item');const body=(await texts(page,'text')).join('\n')
  check('ENTRY-'+role+'-HIDDEN',!body.includes('项目不存在或无权访问')&&!entries.some(x=>x.includes('业主视角')),role+' detail accessible and entry hidden')
 }
 await rel(mp,`/pages/auth/login?devToken=${encodeURIComponent(await login(accounts.boss))}`,'pages/index/index')
 let page=await rel(mp,`/subpackages/project/pages/detail?id=${PROJECT}`,'project/pages/detail');await wait(4000);page=await mp.currentPage()
 let tapped=false;for(const x of await page.$$('.pd__hero-meta-item')){if((await x.text()).includes('业主视角')){await x.tap();tapped=true;break}}
 if(!tapped)throw new Error('boss entry missing')
 page=await rel(mp,`/subpackages/project/pages/owner-preview?projectId=${PROJECT}`,'pages/owner-preview');await wait(1200);page=await mp.currentPage();const root=await page.$('.op');const home=root?await root.text():''
 check('BANNER',home.includes('业主视角预览 · 只读')&&home.includes('不代表业主已查看或确认'),'readonly banner')
 check('WATERMARK',home.includes('业主视角预览 · 仅内部使用 · 造数M负责人 · 工号'),'fixed watermark')
 check('TABS',['首页','全过程','验收'].every(x=>home.includes(x)),'three tabs')
 check('NO-ACTIONS',!['分享给家人','上传照片','提交意见','导出 PDF','导出 Excel'].some(x=>home.includes(x)),'no write/export controls')
 check('NO-INTERNAL',!['成本价','供应商价格','内部备注','毛利','利润'].some(x=>home.includes(x)),'no internal data')
 await mp.screenshot({path:OUT+'/37-test-app-preview-home.png'})
 const tabs=await page.$$('.op__tab');await tabs[1].tap();await wait(500);const r1=await page.$('.op');const journey=r1?await r1.text():'';check('JOURNEY',journey.includes('阶段进度'),'journey tab');await mp.screenshot({path:OUT+'/38-test-app-preview-journey.png'})
 await tabs[2].tap();await wait(500);const r2=await page.$('.op');const acc=r2?await r2.text():'';check('ACCEPTANCE',acc.includes('验收'),'acceptance tab');check('ACCEPTANCE-NO-WRITE',!['确认验收','驳回验收'].some(x=>acc.includes(x)),'acceptance readonly');await mp.screenshot({path:OUT+'/39-test-app-preview-acceptance.png'})
 fs.writeFileSync(OUT+'/test-app-results.json',JSON.stringify({checks,home,journey,acceptance:acc},null,2)+'\n')
 fs.writeFileSync(OUT+'/test-app-summary.txt',checks.map(x=>`${x.pass?'PASS':'FAIL'}\t${x.id}\t${x.detail}`).join('\n')+'\n')
 if(checks.some(x=>!x.pass))process.exitCode=1
}finally{if(mp)mp.disconnect()}
