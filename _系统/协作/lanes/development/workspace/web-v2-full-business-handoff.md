# Web v2 全业务体系开发交接

## 输入依据

- 用户要求：归档旧 Web，基于 `th4-decoboon-app` 业务功能体系重新构建 Vite + Vue3 + TypeScript + Element Plus Web。
- 角色范围：Boss/Company、StoreManager/Store、TeamLeader/Team、Marketer/Designer/Pm/ProjectSupport/Finance 均为 Self。
- App 事实源：`docs/01` 至 `docs/14`、`src/domains/**`、`src/roles/permission.ts`、`src/roles/catalog.ts`。

## 归档

- 可浏览旧源码：`../../前端代码/th4-decoboon-web/archive/legacy-20260916/`
- 压缩归档：`../../前端代码/th4-decoboon-web/archive/th4-decoboon-web-legacy-20260916.tar.gz`
- SHA-256：`56e2bb4fc535bcdd5bacd4f364c76de71b48453a106d0a2ed7409e7b24a34336`

## 交付结果

- 重建 `th4-decoboon-web` v2 工程，技术栈为 Vite + Vue3 + TypeScript + Element Plus + Pinia + Vue Router。
- 对齐 App 领域模型和 Provider 访问层，覆盖客户、设计、报价合同、项目、任务现场、验收、材料供应链、组织、财务、统计、消息和订阅。
- 建立 8 类员工角色目录、能力矩阵和显式 `DataScopeType` 映射。
- 建立 40 个导航入口、31 个资源页路由、角色工作台、客户/报价/项目详情、财务总览、经营报表、消息、订阅和个人中心。
- 增加 `npm run verify:system`，校验角色、角色 API 名称、DataScopeType、能力矩阵、导航路径和路由注册。

## Changed Files / Output Paths

- `../../前端代码/th4-decoboon-web/src/**`
- `../../前端代码/th4-decoboon-web/scripts/verify-system.mjs`
- `../../前端代码/th4-decoboon-web/package.json`
- `../../前端代码/th4-decoboon-web/vite.config.ts`
- `../../前端代码/th4-decoboon-web/README.md`
- `../../前端代码/th4-decoboon-web/docs/WEB-SYSTEM-ARCHITECTURE.md`
- `../../前端代码/th4-decoboon-web/docs/ARCHIVE-2026-09-16.md`
- `../../前端代码/th4-decoboon-web/archive/**`

## Verification

- 生产部署：Web 与后端 `20260916-0415` 已上线 `https://decoboon.joytsing.com/`，HTTPS 首页、静态资源和生产 API 基址冒烟通过。
- `npm run type-check`：通过。
- `npm run verify:system`：8 个角色、31 个资源页、40 个导航入口、8 份能力矩阵全部通过。
- `npm run build`：通过；Vite 产物成功生成。
- 归档 SHA-256 校验：OK。

## Risks / Blockers

- 生产库为空，尚未使用生产企业账号和业务数据执行浏览器全页面联调。
- 后端部分领域仍缺少可写 Provider 或分页能力，Web 当前只开放已有 Provider 能支撑的查询/动作，不能伪造成功状态。
- Element Plus 全量入口 chunk 约 916 KB，后续可按需引入优化，但不影响当前构建。

## Next Action

- acceptance lane 使用真实后端分别以 8 类角色登录，核对菜单、数据范围、详情、表单和行级动作。
- 重点验证跨企业、跨门店、跨团队和 Self 伪造参数均被服务端拒绝。
- 补充浏览器自动化，覆盖登录、角色路由、401、403、客户/报价/项目详情和财务写入。

## Acceptance Needed

是。开发自测不等于最终验收结论。
