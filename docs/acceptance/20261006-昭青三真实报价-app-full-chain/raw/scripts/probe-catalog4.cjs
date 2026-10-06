const {connect,sleep,Drv}=require('./lib.cjs')
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'CAT4')
 try{
   await d.login('StoreManager')
   await d.goto('/subpackages/quote/pages/workbench',3000)
   let p=await d.page();d.note('workbench',p.path)
   let els=await p.$$('*');let out=[];for(const el of els){try{const t=String(await el.text()).trim();if(t&&t.length<40)out.push(t)}catch{}}
   d.note('wbTexts',[...new Set(out)].slice(0,60))
   // tap entry containing 维护 / 项目库 / 材料库 / 管理
   let tapText=null
   for(const el of els){try{const s=String(await el.text()).trim();if(/报价项目库|项目库|维护报价|材料库管理/.test(s)){await el.tap();tapText=s;break}}catch{}}
   d.note('tapped',tapText)
   await sleep(2000);p=await d.page();d.note('afterPath',p.path)
   await d.shot('after-tap')
   els=await p.$$('*');out=[];for(const el of els){try{const t=String(await el.text()).trim();if(t&&t.length<40)out.push(t)}catch{}}
   d.note('afterTexts',[...new Set(out)].slice(0,60))
   console.log(JSON.stringify({workbenchTexts:d.log.find(x=>x.step==='wbTexts').v,tapped:tapText,afterPath:p.path,afterTexts:[...new Set(out)].slice(0,40)},null,2))
 }catch(e){console.log('ERR',String(e).slice(0,200))}
 d.save('probe-catalog4.json');try{mp.disconnect()}catch{}
})()
