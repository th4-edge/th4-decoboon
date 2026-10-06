const {connect,sleep,Drv}=require('./lib.cjs')
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'VERIFY')
 try{
   await d.login('Marketer')
   await d.goto('/pages/customer/list',3000)
   await d.goto('/subpackages/team/pages/workbench',2500)
   await d.goto('/pages/customer/list',3000)
   const dateWarn=d.cons.filter(x=>JSON.stringify(x.args).includes('无法正常使用'))
   console.log(JSON.stringify({dateWarnings:dateWarn.length,consoleTotal:d.cons.length}))
   d.note('dateWarnings',dateWarn.length)
 }catch(e){console.log('ERR',String(e).slice(0,200))}
 d.save('verify-date-fix.json');try{mp.disconnect()}catch{}
})()
