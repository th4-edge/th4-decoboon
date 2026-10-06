const {connect,sleep,Drv}=require('./lib.cjs')
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'CM')
 try{
   await d.login('Boss')
   await d.goto('/subpackages/quote/pages/catalog/manage',3200)
   let p=await d.page()
   let r=null,err=null
   try{r=await p.callMethod('create','construction')}catch(e){err=String(e).slice(0,160)}
   d.note('callResult',r);d.note('callErr',err)
   await sleep(1500);await d.shot('after-create')
   p=await d.page()
   const ins=await p.$$('input');const ph=[]
   for(const i of ins){try{ph.push(await i.attribute('placeholder'))}catch(e){ph.push('?')}}
   const tas=await p.$$('textarea');const th=[]
   for(const i of tas){try{th.push(await i.attribute('placeholder'))}catch(e){th.push('?')}}
   d.note('inputs',ph);d.note('textareas',th)
   const b=await p.$$('button');let bt=[];for(const x of b){try{bt.push(String(await x.text()).trim())}catch{}}
   d.note('buttons',bt)
   console.log(JSON.stringify({callResult:r,callErr:err,inputs:ph,textareas:th,buttons:bt},null,2))
 }catch(e){console.log('ERR',String(e).slice(0,200))}
 d.save('probe-callmethod.json');try{mp.disconnect()}catch{}
})()
