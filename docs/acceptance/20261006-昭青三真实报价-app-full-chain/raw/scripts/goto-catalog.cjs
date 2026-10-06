const {connect,sleep}=require('./lib.cjs')
;(async()=>{const mp=await connect();try{await mp.reLaunch('/subpackages/quote/pages/catalog/manage');await sleep(3200);const p=await mp.currentPage();console.log('path='+p.path)}catch(e){console.log('ERR '+String(e).slice(0,150))}try{mp.disconnect()}catch{}})()
