const fs = require('fs');
const path = require('path');
const automator = require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator');
const projectPath = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/dist/dev/mp-weixin';
const outDir = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260923-麦鸿设计-新企沉浸式体验审计/raw';
(async () => {
  const mp = await automator.launch({cliPath: '/Applications/wechatwebdevtools.app/Contents/MacOS/cli', projectPath, timeout: 60000});
  const logs=[]; mp.on('console', e => logs.push(e));
  try {
    await mp.callWxMethod('reLaunch', {url: '/pages/index/index'});
    await new Promise(r => setTimeout(r, 2500));
    const page = await mp.currentPage();
    const data = await page.data();
    const all = await page.$$('*');
    const texts = [];
    for (const el of all) {
      const t = await el.text();
      if (t && t.trim()) texts.push(t.trim());
      if (texts.length >= 160) break;
    }
    fs.writeFileSync(path.join(outDir, 'accessibility', 'probe-home-after-api-fix.json'), JSON.stringify({path: page.path, query: page.query, data, texts, logs}, null, 2));
    await mp.screenshot({path: path.join(outDir, 'screenshots', 'probe-home-after-api-fix.png')});
    console.log(JSON.stringify({path: page.path, textCount: texts.length, texts: texts.slice(0, 80), logs: logs.slice(-12)}, null, 2));
  } finally { try { mp.disconnect(); } catch(e){} }
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });
