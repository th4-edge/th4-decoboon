const automator=require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js')
const fs=require('fs'),path=require('path'),http=require('http')
const RUN='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20261006-昭青三真实报价-app-full-chain'
const PHONE={Boss:'19900000001',Finance:'19900000002',StoreManager:'19900000003',Marketer:'19900000004','Designer-Dong':'19900000005','Designer-Xie':'19900000006','Pm-Xu':'19900000007','Pm-Wang':'19900000008',TeamLeader:'19900000009'}
const sleep=ms=>new Promise(r=>setTimeout(r,ms))
function post(route,body,token){return new Promise((res,rej)=>{const d=Buffer.from(JSON.stringify(body||{}));const h={'content-type':'application/json','content-length':d.length};if(token)h.token=token;const r=http.request({host:'127.0.0.1',port:8090,path:'/api/'+route,method:'POST',headers:h},x=>{let s='';x.on('data',c=>s+=c);x.on('end',()=>{try{res(JSON.parse(s))}catch(e){rej(new Error(s.slice(0,200)))}})});r.on('error',rej);r.write(d);r.end()})}
async function login(role){const r=await post('auth/session/password-login',{phone:PHONE[role],password:'Zhuang2026Test'});if(!r.success)throw new Error('login '+role+' '+JSON.stringify(r).slice(0,160));return r.data.token}
async function retry(fn,n=3){let e;for(let i=0;i<n;i++){try{return await fn()}catch(err){e=err;await sleep(700)}}throw e}
async function connect(){return automator.connect({wsEndpoint:'ws://127.0.0.1:9420',timeout:60000})}
class Drv{
  constructor(mp,runDir,prefix){this.mp=mp;this.runDir=runDir||RUN;this.prefix=prefix||'case';this.step=0;this.log=[];this.cons=[];this.errors=[]
    try{mp.on('console',m=>{if(m&&(m.type==='error'||m.type==='warn'))this.cons.push({t:new Date().toISOString(),type:m.type,args:(m.args||[]).map(a=>typeof a==='string'?a.slice(0,300):JSON.stringify(a).slice(0,400))})})}catch{}}
  async login(role){this.role=role;this.token=await login(role);await retry(()=>this.mp.reLaunch('/pages/auth/login?devToken='+encodeURIComponent(this.token)),2);await sleep(2600);const p=await this.mp.currentPage();this.log.push({step:'login',role,path:p.path});return p}
  async page(){return this.mp.currentPage()}
  async goto(route,wait=1300){await retry(()=>this.mp.reLaunch(route),2);await sleep(wait);const p=await this.mp.currentPage();return p}
  async shot(label){const out=`${this.prefix}-${String(++this.step).padStart(2,'0')}-${label}.png`;try{await retry(()=>this.mp.screenshot({path:path.join(this.runDir,'raw','screenshots',out)}),2)}catch(e){this.errors.push({shot:out,e:String(e).slice(0,120)});return null}return out}
  async data(){const p=await this.page();try{return await p.data()}catch(e){return {__err:String(e)}}}
  async $$(sel){const p=await this.page();return p.$$(sel)}
  async input(sel,idx,text){const a=await this.$$(sel);if(!a[idx])throw new Error(`no input ${sel}[${idx}] (got ${a.length})`);await a[idx].input(String(text));await sleep(250)}
  async tapSel(sel,idx=0){const a=await this.$$(sel);if(!a[idx])throw new Error(`no ${sel}[${idx}] (got ${a.length})`);await a[idx].tap();await sleep(900)}
  async tapText(text){const p=await this.page();const els=await p.$$('*');for(const el of els){try{const t=String(await el.text()).trim();if(t===text){await el.tap();await sleep(900);return true}}catch{}}throw new Error('no element with text '+text)}
  async waitText(text,timeout=8000){const p=await this.page();const t0=Date.now();while(Date.now()-t0<timeout){const els=await p.$$('*');for(const el of els){try{const t=String(await el.text()).trim();if(t.includes(text))return true}catch{}}await sleep(400)}return false}
  note(k,v){this.log.push({step:k,v})}
  save(name){fs.writeFileSync(path.join(this.runDir,'raw',name),JSON.stringify({role:this.role,log:this.log,console:this.cons.slice(0,200),errors:this.errors},null,2))}
}
module.exports={automator,fs,path,http,RUN,PHONE,sleep,post,login,retry,connect,Drv}
