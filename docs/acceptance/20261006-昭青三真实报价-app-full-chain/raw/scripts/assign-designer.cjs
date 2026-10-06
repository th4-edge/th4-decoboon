const {connect,sleep,post,Drv}=require('./lib.cjs')
const CID='232960420686594048', WANT='董夏鹏'
async function tapByText(d,match){const p=await d.page();const els=await p.$$('*');for(const el of els){let t='';try{t=String(await el.text()).trim()}catch{}if(t&&match(t)){await el.tap();await sleep(1200);return t}}return null}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'ASSIGN')
 try{
   await d.login('StoreManager')
   await d.goto(`/subpackages/customer/pages/detail?id=${CID}`,3000)
   await d.shot('detail')
   const t1=await tapByText(d,t=>t==='待指派'||t==='负责设计师'||t==='指定设计师')
   d.note('tapped1',t1)
   await d.shot('sheet')
   // dump what's visible now
   let p=await d.page();let vis=[];for(const el of await p.$$('*')){try{const t=String(await el.text()).trim();if(t&&t.length<40)vis.push(t)}catch{}}
   d.note('sheetTexts',[...new Set(vis)].slice(0,60))
   const t2=await tapByText(d,t=>t==='董夏鹏 · 总店'||t.startsWith('董夏鹏 ·'))
   d.note('tapped2',t2)
   await sleep(2000);await d.shot('after-assign')
   const info=await post('customer/customer/info',{customerId:CID},d.token||await require('./lib.cjs').login('StoreManager'))
   const c=info.data||{}
   console.log(JSON.stringify({tapped1:t1,tapped2:t2,designer:c.designer||null,name:c.name},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,300)});console.log('ERR',String(e).slice(0,250))}
 d.save('assign-designer.json');try{mp.disconnect()}catch{}
})()
