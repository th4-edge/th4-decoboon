const {connect,sleep,post,Drv}=require('./lib.cjs')
const C={name:'荷塘家园-陈先生',phone:'13700001201',community:'荷塘家园',addr:'荷塘家园3栋1202室',area:'118',budget:'65'}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'HTJY')
 try{
   await d.login('Marketer')
   let p=await d.goto('/subpackages/customer/pages/edit',1800)
   await d.shot('create-empty')
   const ins=await d.$$('input')
   const vals=[C.name,C.phone,C.community,C.addr,C.area,C.budget]
   for(let i=0;i<vals.length && i<ins.length;i++){await ins[i].input(vals[i]);await sleep(200)}
   await d.shot('create-filled')
   // dump page data to inspect current form state
   d.note('formData',await d.data())
   await d.tapSel('.jz-btn--primary',0)
   await sleep(2500)
   await d.shot('after-save')
   const pg=await d.page();d.note('after-path',pg.path)
   // capture visible texts quickly
   const els=await pg.$$('*');let txt=[];for(const el of els){try{const t=String(await el.text()).trim();if(t&&t.length<120)txt.push(t)}catch{}}
   d.note('visibleTexts',[...new Set(txt)].slice(0,60))
   const r=await post('customer/customer/page',{current:1,size:10},d.token)
   const list=r.data&&r.data.content?r.data.content:[]
   const hit=list.find(x=>x.phone===C.phone)
   console.log(JSON.stringify({saved:!!hit,customer:hit,page:pg.path,count:list.length},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,300)});console.log('ERR',String(e).slice(0,300))}
 d.save('case1-customer.json');try{mp.disconnect()}catch{}
})()
