const {connect,sleep,Drv}=require('./lib.cjs')
const CID='232960420686594048', PID='232961774796668928'
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'C1-DESIGN')
 try{
   await d.login('Designer-Dong')
   await d.goto(`/subpackages/design/pages/list?customerId=${CID}&propertyId=${PID}`,2600)
   await d.shot('design-list')
   let p=await d.page()
   const els=await p.$$('*');let txt=[];for(const el of els){try{const t=String(await el.text()).trim();if(t&&t.length<80)txt.push(t)}catch{}}
   d.note('listTexts',[...new Set(txt)].slice(0,50))
   await d.goto(`/subpackages/design/pages/scheme?customerId=${CID}&propertyId=${PID}`,2600)
   await d.shot('design-scheme-empty')
   const pd=await d.data();d.note('schemeDataKeys',Object.keys(pd||{}).slice(0,40))
   const btns=await d.$$('button');let bt=[];for(const b of btns){try{bt.push(String(await b.text()).trim())}catch{}}
   d.note('schemeButtons',bt)
   console.log(JSON.stringify({listTexts:d.log.find(x=>x.step==='listTexts').v,buttons:bt},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,300)});console.log('ERR',String(e).slice(0,200))}
 d.save('probe-design.json');try{mp.disconnect()}catch{}
})()
