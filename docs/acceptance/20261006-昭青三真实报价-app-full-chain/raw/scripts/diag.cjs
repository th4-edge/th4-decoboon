const {connect,sleep,post,Drv}=require('./lib.cjs')
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'DIAG')
 try{
   await d.login('Marketer')
   await d.goto('/pages/customer/list',2500)
   const pd=await d.data();d.note('listKeys',Object.keys(pd||{}))
   d.note('listSample',JSON.stringify(pd).slice(0,600))
   await d.goto('/subpackages/customer/pages/edit',1800)
   const ins=await d.$$('input')
   const vals=['诊断客户A','13700009901','荷塘家园','X','1','1']
   for(let i=0;i<vals.length&&i<ins.length;i++){await ins[i].input(vals[i]);await sleep(150)}
   // list buttons and their texts
   const btns=await d.$$('button');let bt=[];for(const b of btns){try{bt.push(String(await b.text()).trim())}catch{}}
   d.note('buttons',bt)
   let tapped=false
   for(const b of btns){try{if(String(await b.text()).trim().includes('保存')){await b.tap();tapped=true;break}}catch{}}
   d.note('tappedSave',tapped)
   await sleep(2500)
   const p=await d.page();d.note('path',p.path)
   const r=await post('customer/customer/page',{current:1,size:5},d.token)
   d.note('apiFirst',JSON.stringify((r.data&&r.data.content||[]).slice(0,2)).slice(0,400))
   console.log(JSON.stringify({tapped,path:p.path,found:((r.data&&r.data.content)||[]).some(x=>x.telNo==='13700009901')}))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,300)});console.log('ERR',String(e).slice(0,200))}
 d.save('diag.json');try{mp.disconnect()}catch{}
})()
