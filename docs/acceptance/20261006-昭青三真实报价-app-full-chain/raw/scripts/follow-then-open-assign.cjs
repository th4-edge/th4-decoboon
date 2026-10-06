const {connect,sleep,post,login,Drv}=require('./lib.cjs')
const CID='232960420686594048'
async function tapByText(d,match){const p=await d.page();for(const el of await p.$$('*')){let t='';try{t=String(await el.text()).trim()}catch{}if(t&&match(t)){await el.tap();await sleep(1100);return t}}return null}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'FOLLOW')
 try{
   await d.login('StoreManager')
   await d.goto(`/subpackages/customer/pages/follow?id=${CID}`,2600)
   await d.shot('follow-form')
   const tas=await d.$$('.cf__textarea')
   d.note('textareaCount',tas.length)
   if(tas[0]) await tas[0].input('已完成首次电话沟通，客户确认装修意向与预算，同意进入量房与设计阶段。')
   await d.shot('follow-filled')
   const t=await tapByText(d,x=>x==='保存跟进')
   d.note('tappedSave',t)
   await sleep(1800)
   const r=await post('customer/customer-follow/page',{customerId:CID,current:1,size:5},d.token||await login('StoreManager'))
   const list=(r.data&&r.data.content)||[]
   d.note('follows',list.slice(0,2).map(x=>({way:x.way,content:String(x.content||'').slice(0,20)})))
   // verify customer lastFollowAt
   const info=await post('customer/customer/info',{customerId:CID},await login('StoreManager'))
   d.note('customer',{lastFollowAt:(info.data||{}).lastFollowAt,stage:(info.data||{}).status,designer:(info.data||{}).designer||null})
   await d.shot('follow-after')
   // open assign sheet
   await d.goto(`/subpackages/customer/pages/detail?id=${CID}`,3000)
   const t2=await tapByText(d,x=>x==='负责设计师'||x==='待指派')
   d.note('tappedAssign',t2)
   await d.shot('assign-sheet-open')
   console.log(JSON.stringify({saveClicked:t,followCount:list.length,customer:d.log.find(x=>x.step==='customer')?.v,assignEntry:t2},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,300)});console.log('ERR',String(e).slice(0,250))}
 d.save('follow-then-open-assign.json');try{mp.disconnect()}catch{}
})()
