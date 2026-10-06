const {connect,sleep,Drv}=require('./lib.cjs')
async function dump(p,tag,out){for(const sel of ['view','text','button']){let a=[];try{a=await p.$$(sel)}catch(e){}for(let i=0;i<a.length;i++){let t='';try{t=String(await a[i].text()).replace(/\n/g,' ').trim()}catch(e){t='<err>'}out.push(tag+':'+sel+'#'+i+'='+t.slice(0,60))}}}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'BOSSCAT')
 try{
   await d.login('Boss')
   await d.goto('/subpackages/quote/pages/catalog/manage',3500)
   let p=await d.page();const out=[]
   await dump(p,'root',out)
   d.note('rootNodes',out)
   await d.shot('boss-catalog')
   console.log(out.join('\n'))
 }catch(e){console.log('ERR',String(e).slice(0,200))}
 d.save('probe-boss-cat.json');try{mp.disconnect()}catch{}
})()
