const http = require('http');
const automator = require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator');
const projectPath = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/dist/dev/mp-weixin';
function post(url, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const data = Buffer.from(JSON.stringify(body));
    const req = http.request({ hostname: u.hostname, port: u.port, path: u.pathname, method: 'POST', headers: { 'content-type': 'application/json', 'content-length': data.length, ...headers } }, res => {
      let raw = '';
      res.on('data', chunk => raw += chunk);
      res.on('end', () => { try { const value = JSON.parse(raw); value.success ? resolve(value) : reject(new Error(JSON.stringify(value))); } catch (error) { reject(new Error(raw)); } });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}
(async () => {
  const login = await post('http://127.0.0.1:8090/api/auth/session/password-login', { phone: '18660219918', password: 'Maihong2026' });
  const mp = await automator.launch({ cliPath: '/Applications/wechatwebdevtools.app/Contents/MacOS/cli', projectPath, timeout: 60000 });
  try {
    await mp.callWxMethod('clearStorageSync');
    await mp.callWxMethod('reLaunch', { url: '/pages/auth/login?devToken=' + encodeURIComponent(login.data.token) });
    await new Promise(resolve => setTimeout(resolve, 4500));
    let page = await mp.reLaunch('/subpackages/project/pages/detail?id=228259849996075008');
    await new Promise(resolve => setTimeout(resolve, 1200));
    const tabs = await page.$$('.pd__tab');
    console.log('tabs', await Promise.all(tabs.map(item => item.text())));
    await tabs[1].tap();
    await new Promise(resolve => setTimeout(resolve, 1500));
    page = await mp.currentPage();
    console.log('after', page.path);
    const elements = await page.$$('*');
    const texts = [];
    for (const element of elements) {
      const text = await element.text();
      if (text && text.trim()) texts.push(text.trim());
    }
    console.log(JSON.stringify([...new Set(texts)].filter(text => text.length < 180).slice(0, 70), null, 2));
  } finally {
    try { mp.disconnect(); } catch (error) {}
  }
})().catch(error => { console.error(error.stack || error); process.exit(1); });
