const {connect,sleep,post,login,Drv}=require('./lib.cjs')
const CID='232960420686594048', PID='232961774796668928'
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'SCH')
 try{
   await d.login('Designer-Dong')
   const tok=await login('Designer-Dong');d.token=tok
   const info=await post('customer/customer/info',{customerId:CID},tok)
   d.note('canSeeCustomer',info.success)
   d.note('customerDesigner',(info.data||{}).designer||null)
   await d.goto(`/subpackages/design/pages/scheme?customerId=${CID}&propertyId=${PID}`,3000)
   await d.shot('scheme')
   const p=await d.page();d.note('path',p.path)
   for(const sel of ['.ds__section-title','.jz-btn--primary','.jz-btn--plain','.form-input','.jz-chip','input','textarea']){
     let n=0;try{n=(await p.$$(sel)).length}catch(e){}
     d.note('sel:'+sel,n)
   }
   const btns=await p.$$('button');let bt=[];for(const b of btns){try{bt.push(String(await b.text()).trim())}catch{}}
   d.note('buttons',bt)
   let vis=[];for(const el of await p.$$('*')){try{const t=String(await el.text()).trim();if(t&&t.length<45)vis.push(t)}catch{}}
   d.note('texts',[...new Set(vis)].slice(0,60))
   console.log(JSON.stringify({canSee:info.success,designer:(info.data||{}).designer,path:p.path,buttons:bt,texts:[...new Set(vis)].slice(0,40)},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,300)});console.log('ERR',String(e).slice(0,250))}
 d.save('probe-scheme.json');try{mp.disconnect()}catch{}
})()
