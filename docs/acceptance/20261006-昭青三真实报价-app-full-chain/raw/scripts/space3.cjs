const {connect,sleep,Drv}=require('./lib.cjs')
const DID='232965368237260800', CID='232960420686594048', PID='232961774796668928'
async function tapInList(list,match){for(const el of list){let t='';try{t=String(await el.text()).trim()}catch{}if(t&&match(t)){await el.tap();await sleep(1100);return t}}return null}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'SPACE3')
 try{
   // ensure page on scheme with designId
   const p0=await d.page()
   if(p0.path!=='subpackages/design/pages/scheme'){await d.login('Designer-Dong');await d.goto(`/subpackages/design/pages/scheme?customerId=${CID}&propertyId=${PID}&designId=${DID}`,3000)}
   else { await mp.reLaunch(`/subpackages/design/pages/scheme?customerId=${CID}&propertyId=${PID}&designId=${DID}`); await sleep(3000) }
   let p=await d.page()
   d.note('tabs',(await p.$$('.ds__tab')).length)
   d.note('tabTap',await tapInList(await p.$$('.ds__tab'),t=>t.startsWith('区域尺寸')))
   await sleep(900);await d.shot('space-tab')
   let btns=await p.$$('.ds__section-head .jz-btn');d.note('headBtns',btns.length)
   let add=await tapInList(btns,t=>t.includes('新增区域'))
   if(!add){add=await tapInList(await p.$$('.jz-btn.jz-btn--plain.jz-btn--sm'),t=>t.includes('新增区域'))}
   d.note('addTap',add)
   await sleep(1400);await d.shot('space-sheet')
   const n=await d.$$('.ds__space-name-input');const dims=await d.$$('.ds__dim-input')
   d.note('nameInputs',n.length);d.note('dimInputs',dims.length)
   if(n[0]) await n[0].input('全屋')
   const v=['10','12','2.8'];for(let i=0;i<dims.length&&i<v.length;i++){await dims[i].input(v[i]);await sleep(250)}
   await d.shot('space-filled')
   const sv=await tapInList(await p.$$('button'),t=>t.includes('保存区域'))
   d.note('saveTap',sv)
   await sleep(2200);await d.shot('space-after')
   let vis=[];for(const el of await p.$$('*')){try{const t=String(await el.text()).trim();if(t&&t.length<26)vis.push(t)}catch{}}
   d.note('texts',[...new Set(vis)].slice(0,45))
   console.log(JSON.stringify({tab:d.log.find(x=>x.step==='tabTap')?.v,add,names:n.length,dims:dims.length,save:sv,texts:[...new Set(vis)].slice(0,30)},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,250)});console.log('ERR',String(e).slice(0,200))}
 d.save('space3.json');try{mp.disconnect()}catch{}
})()
