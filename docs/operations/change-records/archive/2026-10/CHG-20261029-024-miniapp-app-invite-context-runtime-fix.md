# CHG-20261029-024 · 小程序 App 顶层 inviteContext 依赖修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261029-024` |
| 类型 | 缺陷热修 / 小程序运行时 |
| 风险级别 | L1 |
| 状态 | `DEV_VERIFIED_PENDING_USER` |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | CHG-20261029-022、微信开发者工具 `inviteContext.js is not defined` |

## 2. 目标与范围

- 目标：消除 `app.js` 对 `domains/identity/inviteContext.js` 的顶层运行时依赖，修复小程序启动模块解析错误。
- 范围内：`src/App.vue` 的邀请落点 URL 计算方式。
- 非范围：邀请发现 Provider、邀请页、登录分流和业务权限。
- 受影响端：App / 微信小程序。

## 3. 业务与决策

- 不改变邀请优先级、手机号匹配或邀请接受规则。
- 仅将 App 内部使用的落点 URL 计算内联，避免 App 启动额外加载页面级邀请上下文模块。
- 不涉及对象、权限、金额、成本或数据范围变化。

## 4. 验收条件

| 序号 | 场景 | 预期结果 |
| --- | --- | --- |
| 1 | 小程序 App 启动 | 不再报 `inviteContext.js is not defined` |
| 2 | 生成 app.js | 不包含 `require("./domains/identity/inviteContext.js")` |
| 3 | 邀请发现 | 单邀请仍进入员工/业主页面，多邀请仍进入选择页 |
| 4 | App 构建 | mp-weixin 构建通过 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-app | `dev` | 未提交 | `src/App.vue` |

## 6. 验证

| 检查 | 结果 |
| --- | --- |
| `npm run type-check` | PASS |
| `npm run check:invite-flow` | PASS |
| `npm run build:mp-weixin` | PASS |
| `npm run check:subpackages` | PASS |
| dev/build `app.js` 无 inviteContext require | PASS |

## 7. 发布

- 暂不发布；本地开发版已验证。

## 8. 回滚

- 回滚条件：邀请发现落点异常。
- 回滚步骤：恢复 App 对 `pendingEntriesLandingUrl` 的导入，但该做法会重新引入小程序 App 顶层依赖风险。
- 实际是否回滚：否

## 9. 成本与结论

- 最终结论：模块解析错误已修复，等待微信开发者工具重新编译确认。

## 10. 复盘与沉淀

- 反模式：让小程序 App 入口依赖页面级邀请上下文模块，增加启动期模块图复杂度。
- 正确路径：App 入口只保留轻量类型和无跨页面副作用的本地计算；页面级邀请逻辑留在页面模块。
