# Web 登录页“自有系统感”与展示门禁 · 开发验证证据

> 状态：`DEV_VERIFIED_PENDING_USER`  
> 风险级别：L0（文案、视觉、构建文案门禁）  
> 变更记录：`CHG-20261029-018-web-login-owned-system-copy-gate.md`

## 1. 反馈修正

本轮针对以下问题做直接修正：

- 登录页不再出现“企业品牌标识与主题色来自企业资料设置。”
- 登录页不再出现“本机只记住企业品牌和手机号，不会保存密码。”
- 登录页不再出现“企业、装企、平台、产品、工作台、SaaS”等平台或管理概念。
- 登录页按自有系统入口表达，只保留品牌、业务范围、项目主线与登录动作。
- 删除左侧户型图纸、家具线稿、尺寸线和全部装饰性 SVG，不再用不成熟的家装图片填充版面。
- 新增构建门禁，后续再次写入这些内容会使 `check:client-copy` 失败。

## 2. 当前页面文案

| 状态 | 页面文案 |
| --- | --- |
| 首次品牌态 | 装得清 / 家装项目经营系统 / 家装项目经营 / 客户、设计、报价、施工与财务 / 项目主线 / 账号登录 / 登录装得清 |
| 已登录品牌态 | 昭青装饰 / 经营系统 / 项目经营 / 客户、设计、报价、施工与财务 / 项目主线 / 账号登录 / 登录昭青装饰 |
| 表单 | 使用手机号和登录密码进入系统。 / 手机号 / 登录密码 / 密码可从小程序「我的 → 账号密码」设置或修改。 / 登录 |
| 浏览器标题（首次） | 登录 \| 装得清 |
| 浏览器标题（昭青装饰） | 登录昭青装饰 |

没有功能说明式、设计说明式、缓存说明式或 SaaS 平台自述文案。

## 3. 左侧设计

```text
品牌身份
  ↓
项目经营
客户、设计、报价、施工与财务
  ↓
项目主线：量房 → 设计 → 报价 → 施工 → 验收
```

- 不再使用户型图、家具图、建筑线稿或装饰性图片。
- 左侧只保留文字层级和项目流程，不依靠插画制造产品感。
- 视觉模型提出增加图标、进度条、状态灯等建议；本轮明确拒绝，避免再次产生装饰噪音。

## 4. 新增门禁

门禁文件：`th4-decoboon-web/scripts/check-client-copy.mjs`

登录页禁止：

```text
企业 / 装企 / 平台 / 产品 / 工作台 / SaaS
本机只记住 / 不会保存密码
品牌标识与主题色来自 / 资料设置
全角色 / 经营闭环 / 岗位权限 / 财务穿透
login__plan- / 户型 / 平面图 / <svg>
```

门禁已接入现有 `npm run check:client-copy`，并随 `npm run build` 执行。

负向验证：

```text
node scripts/check-client-copy.mjs --self-test
PASS：平台概念、系统自述和装饰图片均可被拦截
```

## 5. 真实渲染

| 视口 | 文件 | 结果 |
| --- | --- | --- |
| 1440×900 首次态 | `screenshots/login-platform-1440.png` | 官方 Logo，首次平台态，无被禁登录文案 |
| 1440×900 昭青装饰 | `screenshots/login-company-1440.png` | 企业品牌，`scrollWidth=1440`、`scrollHeight=900`，正文无被禁概念，无装饰 SVG |
| 1280×800 | `screenshots/login-company-1280.png` | 无横向溢出，无装饰 SVG，无被禁文案 |
| 1024×768 | `screenshots/login-company-1024.png` | 左右边界 604px，无横向溢出 |
| 390×844 | `screenshots/login-company-390.png` | `scrollWidth=390`、`scrollHeight=844`，登录卡片底部 707px，完整首屏可见 |
| 1280×800 错误态 | `screenshots/login-error-1280.png` | 显示“手机号或密码错误” |
| 1280×800 登录成功 | `screenshots/workbench-success-1280.png` | 落到 `/workbench`，标题“工作台 \| 昭青装饰” |

## 6. 浏览器 DOM 核验

昭青装饰登录态实测：

```text
禁止文案正则：企业|装企|平台|产品|工作台|本机只记住|不会保存密码
结果：false
左侧装饰 SVG：0
页面标题：登录昭青装饰
```

首次品牌态实测：

```text
页面标题：登录 | 装得清
官方 Logo：/brand/logo-mark-color.png
scrollWidth：1440
scrollHeight：900
```

## 7. 登录链路

```text
错误密码 → 停留 /login → 手机号或密码错误
正确密码 → /workbench → 工作台 | 昭青装饰 → 正文不含装得清
```

## 8. 验证命令

```text
node scripts/check-client-copy.mjs --self-test   PASS
npm run check:client-copy                        PASS
npm run check:brand-theme                        PASS
npm run type-check                               PASS
npm run build                                    PASS
```

## 9. 视觉预检

`vision-review-company-1440.md` 由 `qwen2.5vl:7b` 生成。预检确认页面没有卡片墙、药丸标签、英文 eyebrow、装饰性 2.5D/3D 或模板化 KPI。

模型仍提出增加图标、进度条、状态灯和更多对比色。考虑用户明确要求“设计不好就不要放”和避免 AI 装饰味，本轮不采纳这些建议。

## 10. 范围与结论

- 未修改登录认证、密码校验、权限、last-login 字段和 favicon 契约。
- 未发布生产。
- 当前状态：`DEV_VERIFIED_PENDING_USER`，等待本地真实体验验收。
