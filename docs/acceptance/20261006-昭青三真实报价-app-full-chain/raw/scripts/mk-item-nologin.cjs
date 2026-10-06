const {connect,sleep,post,login,Drv}=require('./lib.cjs')
const item=JSON.parse(process.argv[2])
async function tapChip(d,text){const a=await d.$$('.jz-chip');for(const c of a){let t='';try{t=String(await c.text()).trim()}catch{}if(t===text){await c.tap();await sleep(500);return true}}throw new Error('chip not found: '+text)}
async function fillPh(d,sub,val){const a=await d.$$('.jz-form-input');for(const i of a){let ph='';try{ph=String(await i.attribute('placeholder'))}catch{}if(ph.includes(sub)){await i.input(String(val));await sleep(300);return ph}}throw new Error('input not found: '+sub)}
;(async()=>{
 const mp=await connect();const d=new Drv(mp,null,'MK');d.token=await login('Boss')
 try{
   const p=await d.page();let title=''
   try{title=String(await (await p.$('.cm__title')).text()).trim()}catch{}
   d.note('editorTitle',title);d.note('path',p.path)
   if(!title){console.log(JSON.stringify({ok:false,reason:'editor not open'}));d.save('mk-item.json');try{mp.disconnect()}catch{};return}
   await tapChip(d,item.type)
   await fillPh(d,'例：轻钢龙骨',item.name)
   await fillPh(d,'分类',item.category)
   await tapChip(d,item.unit)
   await tapChip(d,'固定总价')
   await fillPh(d,'固定总价',item.fixedTotalPrice)
   const sheet=await p.$('.cm__sheet');d.note('sheetText',String(await sheet.text()).replace(/\s+/g,' ').slice(0,700))
   await d.shot('item-before-save')
   await d.tapText('保存报价项目')
   await sleep(2400)
   d.note('afterPath',(await d.page()).path)
   const r=await post('catalog/item/page',{type:item.apiType,current:1,size:50},d.token||'')
   const list=(r.data&&r.data.content)||[]
   const hit=list.find(x=>String(x.name||'').includes(item.name))
   d.note('apiHit',hit?{id:hit.itemId||hit.id,name:hit.name,fixedTotalPrice:hit.fixedTotalPrice,pricingMode:hit.pricingMode}:null)
   await d.shot('item-after-save')
   console.log(JSON.stringify({ok:!!hit,hit:hit?{id:hit.itemId||hit.id,name:hit.name,fixedTotalPrice:hit.fixedTotalPrice,pricingMode:hit.pricingMode}:null},null,2))
 }catch(e){d.errors.push({fatal:String(e&&e.stack||e).slice(0,300)});console.log('ERR',String(e).slice(0,250))}
 d.save('mk-item.json');try{mp.disconnect()}catch{}
})()
