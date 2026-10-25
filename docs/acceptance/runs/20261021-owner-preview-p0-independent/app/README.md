# App 验收证据

## 最终环境

- App worktree：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app`，`dev`，HEAD `ad3fa58`
- 后端：`decoboon-test`，端口 `18092`，`open-in-view=false`，`ddl-auto=validate`
- 构建：`VITE_API_BASE=http://192.168.31.249:18092/api/ npm run dev:mp-weixin`
- 微信开发者工具 `2.02.2609231 RC` + `miniprogram-automator`
- 测试企业：`ZDQ造数M`
- 测试项目：`王芳（造数1100） · 望江府1栋102`

## 最终 UI 结论

用户最新要求是员工预览 UI 与业主端完全一致。当前实现：

- `owner-preview` 只做权限校验和跳转
- 最终进入同一个 `journey.vue?preview=1`
- 两端共用同一套 Hero、纵向业务分区、图片查看、服务团队、空态和刷新逻辑
- 员工预览只增加既有只读安全层：横幅、水印、返回项目、关闭分享
- 原独立三 Tab 摘要预览已移除

详见：`../EXACT-UI-DECISION.md`

## 执行结果

- 精确 UI 自动化：15/15 PASS（`../migration/exact-ui-app-results.json`）
- 五类允许角色均有入口
- 业务员、财务均无入口
- 入口最终路径：`subpackages/project/pages/journey`
- 业主页面结构、完整业务分区、团队区块均渲染
- 无原三 Tab 预览 UI
- 无业主动作列表、无业主分享卡片
- 固定横幅、水印、返回员工项目 PASS
- `npm run verify:mp-weixin`：PASS

## 主要截图与证据

- `../migration/55-exact-ui-owner-page-in-preview.png`
- `../migration/56-exact-ui-app-acceptance.log`
- `../migration/57-exact-ui-return-button.txt`
- `../migration/58-exact-ui-app-verify.log`

## 说明

- 预览与业主端共用同一页面代码，因此页面结构一致性不再依赖前端双份实现。
- 仍遵守既有 P0 边界：不展示业主确认/驳回、分享、上传、反馈详情；这些是业务权限差异，不是另一套 UI 页面。
