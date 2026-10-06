const {connect,sleep,Drv}=require('./lib.cjs')
async function tapByText(d,match){const p=await d.page();for(const el of await p.$$('*')){let t='';try{t=String(await el.text()).trim()}catch{}if(t&&match(t)){await el.tap();await sleep(1100);return t}}return null}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'SPACE2')
 try{
   const p0=await d.page();d.note('startPath',p0.path)
   d.note('tab',await tapByText(d,x=>x.startsWith('区域尺寸')))
   await sleep(800)
   d.note('add',await tapByText(d,x=>x==='新增区域'))
   await sleep(1300);await d.shot('space-sheet')
   const n=await d.$$('.ds__space-name-input');d.note('nameInputs',n.length)
   if(n[0]) await n[0].input('全屋')
   const dims=await d.$$('.ds__dim-input');d.note('dimInputs',dims.length)
   const v=['10','12','2.8']
   for(let i=0;i<dims.length&&i<v.length;i++){await dims[i].input(v[i]);await sleep(250)}
   await d.shot('space-filled')
   d.note('save',await tapByText(d,x=>/^保存/.test(x)))
   await sleep(2000);await d.shot('space-after')
   const p=await d.page();let vis=[];for(const el of await p.$$('*')){try{const t=String(await el.text()).trim();if(t&&t.length<30)vis.push(t)}catch{}}
   d.note('texts',[...new Set(vis)].slice(0,40))
   console.log(JSON.stringify({tab:d.log.find(x=>x.step==='tab')?.v,add:d.log.find(x=>x.step==='add')?.v,nameInputs:n.length,dimInputs:dims.length,save:d.log.find(x=>x.step==='save')?.v,texts:[...new Set(vis)].slice(0,25)},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,250)});console.log('ERR',String(e).slice(0,200))}
 d.save('scheme-space2.json');try{mp.disconnect()}catch{}
})()
