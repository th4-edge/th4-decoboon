const fs=require('fs');
const path=require('path');
const http=require('http');
const automator=require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator');
const projectPath='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/dist/dev/mp-weixin';
const outDir='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260923-麦鸿设计-新企沉浸式体验审计/raw';
const PROJECT='228259849996075008'; const QUOTE='228259844124049408';
function post(url,body,headers={}){return new Promise((res,rej)=>{const u=new URL(url),d=Buffer.from(JSON.stringify(body));const r=http.request({hostname:u.hostname,port:u.port,path:u.pathname,method:'POST',headers:{'content-type':'application/json','content-length':d.length,...headers}},x=>{let s='';x.on('data',c=>s+=c);x.on('end',()=>{try{const v=JSON.parse(s);v.success?res(v):rej(new Error(JSON.stringify(v)))}catch(e){rej(new Error(s))}})});r.on('error',rej);r.write(d);r.end()})}
async function texts(page){const els=await page.$$('*');const a=[];for(const e of els){const t=await e.text();if(t&&t.trim())a.push(t.trim())}return [...new Set(a)]}
(async()=>{const l=await post('http://127.0.0.1:8090/api/auth/session/password-login',{phone:'18660219918',password:'Maihong2026'});const mp=await automator.launch({cliPath:'/Applications/wechatwebdevtools.app/Contents/MacOS/cli',projectPath,timeout:60000});const logs=[];mp.on('console',e=>logs.push(e));const result={projectTabs:[],quoteVersions:[]};try{
 await mp.callWxMethod('clearStorageSync');await mp.callWxMethod('reLaunch',{url:'/pages/auth/login?devToken='+encodeURIComponent(l.data.token)});await new Promise(r=>setTimeout(r,4500));
 let page=await mp.reLaunch('/subpackages/project/pages/detail?id='+PROJECT);await new Promise(r=>setTimeout(r,1000));
 let tabCount=(await page.$$('.pd__tab')).length;
 for(let i=0;i<tabCount;i++){const fresh=await (await mp.currentPage()).$$('.pd__tab');const label=await fresh[i].text();await fresh[i].tap();await new Promise(r=>setTimeout(r,650));page=await mp.currentPage();const text=await texts(page);result.projectTabs.push({index:i,label,text:text.filter(t=>t.length<220).slice(0,35)});await mp.screenshot({path:path.join(outDir,'screenshots',`S05-project-tab-${i}.png`)});}
 page=await mp.reLaunch('/subpackages/quote/pages/detail?id='+QUOTE);await new Promise(r=>setTimeout(r,1000));
 let cardCount=(await page.$$('.qd__cq-card')).length;
 for(let i=0;i<Math.min(cardCount,3);i++){const fresh=await (await mp.currentPage()).$$('.qd__cq-card');await fresh[i].tap();await new Promise(r=>setTimeout(r,650));page=await mp.currentPage();const text=await texts(page);result.quoteVersions.push({index:i,text:text.filter(t=>t.length<180).slice(0,28)});await mp.screenshot({path:path.join(outDir,'screenshots',`S05-quote-version-${i}.png`)});}
 fs.writeFileSync(path.join(outDir,'accessibility','S05-ui-interactions.json'),JSON.stringify({...result,logs},null,2));console.log(JSON.stringify({tabs:result.projectTabs.map(x=>({label:x.label,textCount:x.text.length})),versions:result.quoteVersions.length,logs:logs.slice(-8)},null,2));
}finally{try{mp.disconnect()}catch(e){}}})().catch(e=>{console.error(e.stack||e);process.exit(1)});
