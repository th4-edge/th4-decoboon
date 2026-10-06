const {connect,sleep,Drv}=require('./lib.cjs')
const CID='232960420686594048'
async function texts(p){const els=await p.$$('*');const out=[];for(const el of els){try{const t=String(await el.text()).trim();if(t&&t.length<60)out.push(t)}catch{}}return [...new Set(out)]}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'C1-ASSIGN')
 try{
   await d.login('StoreManager')
   await d.goto(`/subpackages/customer/pages/detail?id=${CID}`,2800)
   await d.shot('detail')
   let p=await d.page();let t=await texts(p)
   d.note('detailTexts',t.slice(0,60))
   // tap a text containing 指定设计师 / 设计师
   let tapped=false
   const els=await p.$$('*')
   for(const el of els){try{const s=String(await el.text()).trim();if(s==='待指派'||s==='负责设计师'||s==='指定设计师'){await el.tap();tapped=true;break}}catch{}}
   d.note('tappedAssign',tapped)
   await sleep(1500);await d.shot('assign-sheet')
   p=await d.page();t=await texts(p)
   d.note('sheetTexts',t.slice(0,60))
   console.log(JSON.stringify({tapped,sheetTexts:t.slice(0,40)},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,300)});console.log('ERR',String(e).slice(0,200))}
 d.save('probe-assign.json');try{mp.disconnect()}catch{}
})()
