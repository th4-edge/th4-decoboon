const {connect,sleep,Drv}=require('./lib.cjs')
async function texts(p,cap=200){const els=await p.$$('*');const out=[];for(const el of els){try{const t=String(await el.text()).trim();if(t&&t.length<40)out.push(t)}catch{}}return [...new Set(out)].slice(0,cap)}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'CAT')
 try{
   await d.login('StoreManager')
   await d.goto('/subpackages/quote/pages/catalog/manage',3000)
   await d.shot('catalog-manage')
   let p=await d.page()
   // tap create button (新增施工/新增主材...)
   let ok=false
   for(const el of await p.$$('*')){try{const s=String(await el.text()).trim();if(/^新增/.test(s)){await el.tap();ok=true;break}}catch{}}
   await sleep(1500);await d.shot('catalog-editor')
   p=await d.page()
   const inputs=await p.$$('input');const ph=[]
   for(const i of inputs){try{ph.push(await i.attribute('placeholder'))}catch(e){ph.push('?')}}
   const tas=await p.$$('textarea');const th=[]
   for(const i of tas){try{th.push(await i.attribute('placeholder'))}catch(e){th.push('?')}}
   d.note('openedEditor',ok);d.note('inputPlaceholders',ph);d.note('textareaPlaceholders',th)
   d.note('texts',(await texts(p,120)).slice(0,80))
   console.log(JSON.stringify({openedEditor:ok,inputs:ph,textareas:th},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,300)});console.log('ERR',String(e).slice(0,200))}
 d.save('probe-catalog.json');try{mp.disconnect()}catch{}
})()
