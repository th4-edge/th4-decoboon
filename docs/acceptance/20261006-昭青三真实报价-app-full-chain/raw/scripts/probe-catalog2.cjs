const {connect,sleep,Drv}=require('./lib.cjs')
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'CAT2')
 try{
   await d.login('StoreManager')
   await d.goto('/subpackages/quote/pages/catalog/manage',3500)
   await d.shot('catalog-manage')
   const p=await d.page()
   const els=await p.$$('*');const out=[];for(const el of els){try{const t=String(await el.text()).trim();if(t&&t.length<40)out.push(t)}catch{}}
   d.note('texts',[...new Set(out)].slice(0,80))
   const btns=await p.$$('button');const bt=[];for(const b of btns){try{bt.push(String(await b.text()).trim())}catch{}}
   d.note('buttons',bt)
   console.log(JSON.stringify({texts:[...new Set(out)].slice(0,60),buttons:bt},null,2))
 }catch(e){console.log('ERR',String(e).slice(0,200))}
 d.save('probe-catalog2.json');try{mp.disconnect()}catch{}
})()
