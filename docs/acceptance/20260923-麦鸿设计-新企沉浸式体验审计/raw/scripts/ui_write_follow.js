const fs=require('fs');
const path=require('path');
const http=require('http');
const automator=require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator');
const projectPath='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/dist/dev/mp-weixin';
const outDir='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260923-麦鸿设计-新企沉浸式体验审计/raw';
const CUSTOMER='228259840781189120';
const NOTE='[沉浸审计] 新增一次业务员跟进：客户已确认后续设计和施工信息能够在本项目内继续查看。';
function post(url,body,headers={}){return new Promise((res,rej)=>{const u=new URL(url),d=Buffer.from(JSON.stringify(body));const r=http.request({hostname:u.hostname,port:u.port,path:u.pathname,method:'POST',headers:{'content-type':'application/json','content-length':d.length,...headers}},x=>{let s='';x.on('data',c=>s+=c);x.on('end',()=>{try{const v=JSON.parse(s);v.success?res(v):rej(new Error(JSON.stringify(v)))}catch(e){rej(new Error(s))}})});r.on('error',rej);r.write(d);r.end()})}
function textsOf(elements){return Promise.all(elements.map(async e=>{const t=await e.text();return t&&t.trim()?t.trim():null})).then(x=>[...new Set(x.filter(Boolean))])}
(async()=>{const l=await post('http://127.0.0.1:8090/api/auth/session/password-login',{phone:'19900000002',password:'Maihong2026'});const mp=await automator.launch({cliPath:'/Applications/wechatwebdevtools.app/Contents/MacOS/cli',projectPath,timeout:60000});const logs=[];mp.on('console',e=>logs.push(e));try{
 await mp.callWxMethod('clearStorageSync');
 await mp.callWxMethod('reLaunch',{url:'/pages/auth/login?devToken='+encodeURIComponent(l.data.token)});await new Promise(r=>setTimeout(r,3500));
 let page=await mp.reLaunch('/subpackages/customer/pages/follow?id='+CUSTOMER);await new Promise(r=>setTimeout(r,1500));
 await mp.screenshot({path:path.join(outDir,'screenshots','S04-follow-before.png')});
 const before=await textsOf(await page.$$('*'));
 const input=await page.$('.cf__textarea');
 if(!input) throw new Error('未找到跟进内容输入框');
 await input.input(NOTE);await new Promise(r=>setTimeout(r,400));
 const save=await page.$('.cf__save');if(!save) throw new Error('未找到保存跟进按钮');
 await save.tap();await new Promise(r=>setTimeout(r,1800));
 page=await mp.currentPage();const after=await textsOf(await page.$$('*'));
 await mp.screenshot({path:path.join(outDir,'screenshots','S04-follow-after.png')});
 fs.writeFileSync(path.join(outDir,'accessibility','S04-follow-ui-write.json'),JSON.stringify({inputNote:NOTE,before,after,currentPath:page.path,logs},null,2));
 console.log(JSON.stringify({currentPath:page.path,beforeHasLastFollow:before.some(t=>t.includes('上次跟进')),after:[...new Set(after)].slice(0,40),logs:logs.slice(-10)},null,2));
}finally{try{mp.disconnect()}catch(e){}}})().catch(e=>{console.error(e.stack||e);process.exit(1)});
