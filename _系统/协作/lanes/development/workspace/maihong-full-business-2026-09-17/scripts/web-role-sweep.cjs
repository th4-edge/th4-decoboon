const { chromium }=require('playwright');
const base='http://127.0.0.1:8080';
const users=[
  {key:'marketer',phone:'13900000006',routes:['/crm/customers','/crm/measures']},
  {key:'designer',phone:'13900000002',routes:['/design/schemes','/trade/quotes']},
  {key:'pm',phone:'13900000003',routes:['/projects/tasks','/projects/acceptance']},
  {key:'finance',phone:'13900000004',routes:['/finance/overview','/finance/partners-ledger']},
  {key:'support',phone:'13900000005',routes:['/materials/inventory']},
  {key:'storeManager',phone:'13900000001',routes:['/crm/customers']},
  {key:'boss',phone:'18660219918',routes:['/statistics/business','/subscription','/projects/226054084552032256']}
];
(async()=>{const browser=await chromium.launch({headless:true,executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',args:['--no-sandbox']});const report=[];for(const u of users){const context=await browser.newContext({viewport:{width:1440,height:1000}});const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(String(e.message).slice(0,220)));page.on('console',m=>{if(m.type()==='error')errors.push(m.text().slice(0,220))});await page.goto(base+'/login',{waitUntil:'domcontentloaded'});await page.locator('input[placeholder="请输入手机号码"]').fill(u.phone);await page.locator('input[placeholder="请输入登录密码"]').fill('Maihong123');await page.getByRole('button',{name:'进入工作台'}).click();await page.waitForURL(/\/workbench/, {timeout:12000});for(const route of u.routes){await page.goto(base+route,{waitUntil:'networkidle',timeout:20000});const text=(await page.locator('body').innerText()).replace(/\s+/g,' ').trim();report.push({user:u.key,route,url:page.url(),title:await page.title(),text:text.slice(0,700),errors:errors.slice(-3)});await page.screenshot({path:`/private/tmp/web-${u.key}-${route.replace(/\//g,'_')}.png`,fullPage:false});}await context.close();}console.log(JSON.stringify(report,null,2));await browser.close()})().catch(e=>{console.error(e.stack);process.exit(1)})
