const {connect,sleep,post,login,Drv}=require('./lib.cjs')
const CID='232960420686594048', PID='232961774796668928'
async function tapByText(d,match){const p=await d.page();for(const el of await p.$$('*')){let t='';try{t=String(await el.text()).trim()}catch{}if(t&&match(t)){await el.tap();await sleep(1100);return t}}return null}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'SPACE')
 try{
   await d.login('Designer-Dong')
   await d.goto(`/subpackages/design/pages/scheme?customerId=${CID}&propertyId=${PID}`,3000)
   d.note('tabTap',await tapByText(d,x=>x.startsWith('区域尺寸')))
   await sleep(700);await d.shot('space-tab')
   d.note('addTap',await tapByText(d,x=>x==='新增区域'))
   await sleep(1200);await d.shot('space-sheet')
   const nameIn=await d.$$('.ds__space-name-input');d.note('nameInputs',nameIn.length)
   if(nameIn[0]) await nameIn[0].input('全屋')
   const dims=await d.$$('.ds__dim-input');d.note('dimInputs',dims.length)
   const vals=['10','12','2.8']
   for(let i=0;i<dims.length&&i<vals.length;i++){await dims[i].input(vals[i]);await sleep(200)}
   await d.shot('space-filled')
   const sp=await tapByText(d,x=>/^保存/.test(x))
   d.note('saveTap',sp)
   await sleep(1800);await d.shot('space-after')
   // verify
   const tok=await login('Designer-Dong')
   const r=await post('design/scheme/info',{designId:(await post('design/scheme/page',{customerId:CID,current:1,size:10},tok)).data.content[0].designId||''},tok)
   console.log(JSON.stringify({tab:d.log.find(x=>x.step==='tabTap')?.v,add:d.log.find(x=>x.step==='addTap')?.v,save:sp},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,250)});console.log('ERR',String(e).slice(0,200))}
 d.save('scheme-space.json');try{mp.disconnect()}catch{}
})()
