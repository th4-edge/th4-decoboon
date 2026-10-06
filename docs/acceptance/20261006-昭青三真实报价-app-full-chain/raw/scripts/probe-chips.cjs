const {connect,sleep,Drv}=require('./lib.cjs')
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'CHIP')
 try{
   const p=await mp.currentPage();d.note('path',p.path)
   for(const sel of ['.jz-chip','.jz-form-input','.jz-form-textarea','.cm__sheet','.cm__title']){
     let n=0,txt=[];try{const a=await p.$$(sel);n=a.length;for(let i=0;i<Math.min(a.length,12);i++){let t='';try{t=String(await a[i].text()).trim()}catch{};txt.push(t)}}catch(e){}
     d.note(sel,{n,txt})
   }
   const btns=await p.$$('button');let bt=[];for(const b of btns){try{bt.push(String(await b.text()).trim())}catch{}}
   d.note('buttons',bt)
   console.log(JSON.stringify(d.log,null,2))
 }catch(e){console.log('ERR',String(e).slice(0,200))}
 d.save('probe-chips.json');try{mp.disconnect()}catch{}
})()
