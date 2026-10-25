import { createRequire } from 'node:module';
import { pathToFileURL, fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const require = createRequire(import.meta.url);
let playwright;
try {
  playwright = require('playwright');
} catch {
  playwright = require('/Users/zhangwei/.codex/skills/guizang-social-card-skill/node_modules/playwright');
}
const { chromium } = playwright;
const root = resolve(fileURLToPath(new URL('./', import.meta.url)));
const source = pathToFileURL(resolve(root, 'index.html')).href;
const browser = await chromium.launch({ headless: true });

async function openPage(deviceScaleFactor) {
  const context = await browser.newContext({
    viewport: { width: 1240, height: 1818 },
    deviceScaleFactor
  });
  const page = await context.newPage();
  await page.goto(source, { waitUntil: 'networkidle' });
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(350);
  return { context, page };
}

const preview = await openPage(1);
const report = await preview.page.evaluate(() => {
  const pageEl = document.querySelector('.page');
  const overflow = [...document.querySelectorAll('.page *')]
    .filter((el) => el.scrollHeight > el.clientHeight + 1 || el.scrollWidth > el.clientWidth + 1)
    .map((el) => ({ tag: el.tagName, className: el.className, text: (el.textContent || '').trim().slice(0, 70), client: [el.clientWidth, el.clientHeight], scroll: [el.scrollWidth, el.scrollHeight] }))
    .filter((x) => !['IMG', 'SVG'].includes(x.tag));
  const sections = [...pageEl.children].map((el) => {
    const r = el.getBoundingClientRect();
    return { className: el.className, top: Math.round(r.top), bottom: Math.round(r.bottom), height: Math.round(r.height) };
  });
  return { page: { width: pageEl.scrollWidth, height: pageEl.scrollHeight }, overflow, sections };
});
console.log(JSON.stringify(report, null, 2));
await preview.page.locator('.page').screenshot({ path: resolve(root, '装得清地推产品页-A4.png') });
await preview.context.close();

const printPage = await openPage(2);
await printPage.page.locator('.page').screenshot({ path: resolve(root, '装得清地推产品页-A4-300dpi.png') });
await printPage.context.close();
await browser.close();
