const fs = require('fs');
const path = require('path');
const http = require('http');
const automator = require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator');
const projectPath = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/dist/dev/mp-weixin';
const outDir = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260923-麦鸿设计-新企沉浸式体验审计/raw';
const role = process.argv[2];
const apiBase = process.env.API_BASE || 'http://127.0.0.1:8090/api';
const CONFIG = {
  boss: {phone:'18660219918', routes:[
    '/pages/index/index','/pages/customer/list','/subpackages/design/pages/list','/subpackages/quote/pages/list',
    '/pages/project/list','/pages/finance/index','/subpackages/finance/pages/projects','/subpackages/team/pages/workbench',
    '/pages/dynamic/index','/pages/mine/index'
  ]},
  marketer: {phone:'19900000002', routes:[
    '/pages/index/index','/pages/customer/list','/subpackages/customer/pages/flow','/subpackages/customer/pages/edit',
    '/subpackages/customer/pages/follow','/subpackages/customer/pages/measure-workbench','/subpackages/customer/pages/property-edit',
    '/pages/project/list','/pages/mine/index'
  ]},
  designer: {phone:'19900000003', routes:[
    '/pages/index/index','/subpackages/design/pages/list','/subpackages/design/pages/workbench','/subpackages/design/pages/requirements',
    '/subpackages/design/pages/scheme','/subpackages/quote/pages/list','/subpackages/quote/pages/workbench','/subpackages/quote/pages/catalog/manage',
    '/pages/project/list','/pages/mine/index'
  ]},
  pm: {phone:'19900000006', routes:[
    '/pages/index/index','/subpackages/team/pages/workbench','/pages/project/list','/subpackages/project/pages/detail?id=228259849996075008',
    '/subpackages/project/pages/plan?id=228259849996075008','/subpackages/project/pages/album?id=228259849996075008','/subpackages/project/pages/report/daily?id=228259849996075008','/subpackages/project/pages/accept/edit?stageId=228259850050600960',
    '/pages/mine/index'
  ]},
  finance: {phone:'19900000005', routes:[
    '/pages/index/index','/pages/finance/index','/subpackages/finance/pages/ledger','/subpackages/finance/pages/receivables',
    '/subpackages/finance/pages/receipts','/subpackages/finance/pages/payables','/subpackages/finance/pages/payments',
    '/subpackages/finance/pages/projects','/subpackages/finance/pages/entry','/pages/mine/index'
  ]}
};
if (process.env.ROUTES) CONFIG[role].routes = JSON.parse(process.env.ROUTES);
if (!CONFIG[role]) { console.error('unknown role', role); process.exit(2); }

function postJson(url, body, headers={}) {
  return new Promise((resolve,reject) => {
    const data = Buffer.from(JSON.stringify(body));
    const u = new URL(url);
    const req = http.request({hostname:u.hostname, port:u.port, path:u.pathname+u.search, method:'POST', headers:{'Content-Type':'application/json','Content-Length':data.length,...headers}}, res => {
      let raw=''; res.on('data', c => raw += c); res.on('end', () => {
        try { const v=JSON.parse(raw); if (!v.success) reject(new Error(JSON.stringify(v))); else resolve(v); }
        catch(e){ reject(new Error(raw)); }
      });
    });
    req.on('error', reject); req.write(data); req.end();
  });
}
function uniqTexts(arr) { return [...new Set(arr.map(x => String(x).trim()).filter(Boolean))]; }

(async () => {
  const login = await postJson(apiBase + '/auth/session/password-login', {phone: CONFIG[role].phone, password:'Maihong2026'});
  const token = login.data.token;
  const mp = await automator.launch({cliPath:'/Applications/wechatwebdevtools.app/Contents/MacOS/cli', projectPath, timeout:60000});
  const consoleLogs=[]; mp.on('console', e => consoleLogs.push(e));
  const results=[];
  try {
    await mp.callWxMethod('clearStorageSync');
    await mp.callWxMethod('reLaunch', {url:`/pages/auth/login?devToken=${encodeURIComponent(token)}`});
    await new Promise(r => setTimeout(r, 4500));
    const home = await mp.currentPage();
    results.push({route:'__login__', actualPath:home.path, texts:[], logs:[]});
    for (let i=0;i<CONFIG[role].routes.length;i++) {
      const route=CONFIG[role].routes[i];
      const before=consoleLogs.length;
      let actualPath=null, texts=[], error=null;
      try {
        const page = await mp.reLaunch(route);
        await new Promise(r => setTimeout(r, 900));
        actualPath = page && page.path;
        const all = await page.$$('*');
        for (const el of all) { const t=await el.text(); if (t && t.trim()) texts.push(t.trim()); }
        await mp.screenshot({path:path.join(outDir,'screenshots',`${role}-${String(i+1).padStart(2,'0')}-${route.replace(/[\\/?:=&]/g,'-')}.png`)});
      } catch(e) { error=String(e && e.stack || e); }
      results.push({route, actualPath, texts, logs:consoleLogs.slice(before), error});
    }
    fs.writeFileSync(path.join(outDir,'accessibility',`role-${role}.json`), JSON.stringify({role, results}, null, 2));
    console.log(JSON.stringify({role, loginPath: results[0].actualPath, pages: results.slice(1).map(x=>({route:x.route, actualPath:x.actualPath, textCount:x.texts.length, errors:x.error?1:0}))}, null, 2));
  } finally { try { mp.disconnect(); } catch(e){} }
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });
