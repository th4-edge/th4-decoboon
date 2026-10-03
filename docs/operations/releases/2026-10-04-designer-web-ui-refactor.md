# 2026-10-04 发布记录 · web-v2026.10.04-designer-ui

## 1. 发布结论

- 状态：已上线
- 后端：无代码变更；本次不重新部署后端
- App：无代码变更；本次不上传小程序
- Web：已发布设计师工作台、客户与房屋、量房、报价页面重构
- Release ID：`web-v2026.10.04-designer-ui`
- 发布经理：Codex / 用户授权
- 产品批准：用户授权直接发版
- 验收结论：生产 SPA 路由、静态资源、API 健康检查通过；登录后设计师真实业务链仍需生产账号回归
- 主要范围：
  - 今日工作总览、Top4、完整队列下钻
  - 客户与房屋客户360、诉求、沟通、固定高度三栏
  - 量房中心 Tab、固定高度三栏
  - 报价对象池、报价 Tab、报价上下文
  - 去除设计师 hero 和说明性文案
- 明确排除：
  - 后端 Provider / 数据库 / 权限 / 金额语义变更
  - 小程序代码和微信版本上传
  - 生产账号登录后的真实业务闭环验收

## 2. Git 与制品

| 仓库 | branch | commit | tag | 制品 | SHA-256 |
| --- | --- | --- | --- | --- | --- |
| th4-decoboon | prod | 本记录提交 | - | 无运行时制品 | - |
| th4-decoboon-app | prod | 0078593 | - | 无新制品 | - |
| th4-decoboon-web | prod | c1f1d0d | web-v2026.10.04-designer-ui | th4-decoboon-web-2.0.0.tar.gz | 6dd37c8d3b9d59ae4b4952dd56b58418f31ec9aca1940791661791006c10f060 |

Web 制品路径：

`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-web/release/20261004-072958/th4-decoboon-web-2.0.0.tar.gz`

## 3. 发布前门禁

- 后端测试：无后端代码变更，不触发后端发布测试
- Provider 安全扫描：无后端变更
- Web 类型检查与构建：`npm run type-check`、`npm run verify:ui`、`npm run verify:designer-shell`、`npm run verify:roles`、`npm run verify:system`、`npm run build` 全部通过
- App/Web 兼容性：未改共享后端接口；App 代码无变更
- 未关闭阻断项：无代码级阻断；登录后真实业务链待生产账号回归

## 4. 数据库迁移

- 无 DDL、无迁移、无数据删除
- 备份目录：不适用
- 回滚/补偿方案：Web 静态资源回滚，不改数据库

## 5. 部署与验证

- 部署流程：上传 Web release archive -> SHA256 校验 -> 解包到 `/opt/decoboon-web/releases/20261004-072958` -> 原子切换 `/opt/decoboon-web/current` -> HTTPS 冒烟
- 部署 release：`/opt/decoboon-web/releases/20261004-072958`
- 上一稳定 release：`/opt/decoboon-web/releases/20260918-130245`
- 健康检查：
  - `https://decoboon.joytsing.com/` -> 200
  - `/designer/workbench` -> 200
  - `/designer/customers` -> 200
  - `/designer/measure` -> 200
  - `/designer/quotes` -> 200
  - 生产 `index.html` SHA-256：`dbacd56b4db9d74cb86ed70bacbf8a3b12eee79c5e8c94d2ad04f17f49fd375c`
  - 生产 JS 资源：`assets/index-DVz6rjPH.js` -> 200
  - API：`/api/auth/session/info` -> HTTP 200，业务码 `tokenIsUndefined`
- 核心业务冒烟：生产 SPA 与 API 可达；登录后业务链待生产账号验证
- 权限与越权负向：本轮未改后端权限；Web 路由权限沿用既有服务端校验
- 文件/支付/消息等关键链路：未改后端，不做本轮回归
- 日志与监控观察：Web 静态资源发布无后端重启

## 6. 小程序

- 上传版本名/版本号：无
- 上传结果：无
- 体验版验证：无
- 提交审核/发布状态：无
- 未完成事项：无

## 7. 回滚

- 回滚触发条件：生产设计师页面白屏、静态资源 404、核心路由不可达
- 回滚负责人：用户 / Codex
- 回滚命令：
  `ln -sfnT /opt/decoboon-web/releases/20260918-130245 /opt/decoboon-web/current`
- 回滚结果：未触发
- 数据修复/补偿：不适用
- 遗留观察项：观察设计师登录后页面加载与接口请求

## 8. 风险与后续

- 已知风险：当前本地数据没有待量房对象，量房页有真实数据后的交互仍需验证
- 观察窗口：发布后 30 分钟
- 后续动作：
  1. 使用生产设计师账号完成今日工作、客户与房屋、量房、报价主链冒烟；
  2. 如出现静态资源或路由问题，按第 7 节切回上一 release；
  3. 若需 App 新版本，另行执行小程序 release build 和微信上传。
- 关联需求/事故/验收记录：`WEB-V2-DESIGNER-WORKBENCH-OVERVIEW-20261004`
