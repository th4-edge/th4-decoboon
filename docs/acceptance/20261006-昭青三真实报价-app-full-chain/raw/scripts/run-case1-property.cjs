const {connect,sleep,post,Drv}=require('./lib.cjs')
const CID='232960420686594048'
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'C1-PROP')
 try{
   await d.login('Marketer')
   await d.goto(`/subpackages/customer/pages/property-edit?customerId=${CID}`,2200)
   await d.shot('property-empty')
   const ins=await d.$$('input')
   d.note('input-count',ins.length)
   const vals=['合肥市蜀山区荷塘家园3栋1202室','荷塘家园','3栋','1202','118','主卧朝南']
   for(let i=0;i<vals.length&&i<ins.length;i++){await ins[i].input(vals[i]);await sleep(180)}
   await d.shot('property-filled')
   await d.tapSel('.jz-btn--primary',0)
   await sleep(2200)
   const p=await d.page();d.note('after-path',p.path)
   const r=await post('property/property/page',{customerId:CID,current:1,size:10},d.token)
   console.log(JSON.stringify({path:p.path,ok:r.success,props:(r.data&&r.data.content||[]).map(x=>({id:x.propertyId||x.id,addr:x.address,area:x.area}))},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,300)});console.log('ERR',String(e).slice(0,200))}
 d.save('case1-property.json');try{mp.disconnect()}catch{}
})()
