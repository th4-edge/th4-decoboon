const {connect,sleep,Drv}=require('./lib.cjs')
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'CATB')
 try{
   await d.login('Boss')
   await d.goto('/subpackages/quote/pages/catalog/manage',4000)
   const p=await d.page();d.note('path',p.path)
   const pd=await p.data();d.note('dataKeys',Object.keys(pd||{}).slice(0,60))
   d.note('data',JSON.stringify(pd).slice(0,700))
   for(const sel of ['view','text','button','.cp__type','.cp__create','.cm__title']){
     let n=0;try{n=(await p.$$(sel)).length}catch(e){}
     d.note('sel:'+sel,n)
   }
   console.log(JSON.stringify({path:p.path,keys:Object.keys(pd||{}).slice(0,30),counts:d.log.filter(x=>String(x.step).startsWith('sel:')).map(x=>[x.step,x.v])},null,2))
 }catch(e){console.log('ERR',String(e).slice(0,200))}
 d.save('probe-catalog3.json');try{mp.disconnect()}catch{}
})()
