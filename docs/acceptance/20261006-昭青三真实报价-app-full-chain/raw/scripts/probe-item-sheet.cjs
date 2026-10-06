const {connect,sleep,Drv}=require('./lib.cjs')
const CID='232960420686594048', PID='232961774796668928'
async function tapByText(d,match){const p=await d.page();for(const el of await p.$$('*')){let t='';try{t=String(await el.text()).trim()}catch{}if(t&&match(t)){await el.tap();await sleep(1100);return t}}return null}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'ITEMS')
 try{
   await d.login('Designer-Dong')
   await d.goto(`/subpackages/design/pages/scheme?customerId=${CID}&propertyId=${PID}`,3000)
   let t=await tapByText(d,x=>x==='区域尺寸')
   d.note('tabSpace',t)
   await sleep(800)
   t=await tapByText(d,x=>/新增区域|新增空间/.test(x))
   d.note('addSpace',t)
   await sleep(1200);await d.shot('space-sheet')
   let p=await d.page()
   for(const sel of ['.jz-chip','.jz-form-input','input','textarea','.jz-btn--primary','button']){
     let n=0,txt=[];try{const a=await p.$$(sel);n=a.length;for(let i=0;i<Math.min(a.length,10);i++){let s='';try{s=String(await a[i].text()).trim()}catch{};txt.push(s||('ph='+String(await a[i].attribute('placeholder')).slice(0,18)))}}catch(e){}
     d.note(sel,{n,txt})
   }
   console.log(JSON.stringify(d.log.filter(x=>['tabSpace','addSpace'].includes(x.step)||typeof x.step==='string'&&x.step.startsWith('.')).slice(-8),null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,250)});console.log('ERR',String(e).slice(0,200))}
 d.save('probe-item-sheet.json');try{mp.disconnect()}catch{}
})()
