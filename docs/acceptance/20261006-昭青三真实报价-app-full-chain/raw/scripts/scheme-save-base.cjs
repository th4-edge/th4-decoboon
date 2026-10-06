const {connect,sleep,Drv}=require('./lib.cjs')
const CID='232960420686594048', PID='232961774796668928', NAME='荷塘家园-陈先生装修设计方案'
async function tapByText(d,match){const p=await d.page();for(const el of await p.$$('*')){let t='';try{t=String(await el.text()).trim()}catch{}if(t&&match(t)){await el.tap();await sleep(1100);return t}}return null}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'SAVEBASE')
 try{
   await d.login('Designer-Dong')
   await d.goto(`/subpackages/design/pages/scheme?customerId=${CID}&propertyId=${PID}`,3200)
   const ins=await d.$$('.jz-form-input')
   d.note('formInputs',ins.length)
   let ph=''
   for(const i of ins){try{ph=String(await i.attribute('placeholder'))}catch{}if(ph.includes('现代简约')||ph.includes('方案')){await i.input(NAME);break}}
   d.note('nameFilled',ph)
   await d.shot('base-filled')
   const t=await tapByText(d,x=>x==='更多')
   d.note('moreTap',t)
   await d.shot('more-sheet')
   console.log(JSON.stringify({formInputs:ins.length,namePh:ph,more:t}))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,250)});console.log('ERR',String(e).slice(0,200))}
 d.save('scheme-save-base.json');try{mp.disconnect()}catch{}
})()
