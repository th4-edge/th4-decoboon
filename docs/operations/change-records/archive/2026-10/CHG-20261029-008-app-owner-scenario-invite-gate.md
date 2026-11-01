# CHG-20261029-008 · App 首页业主场景与邀请入口角色门禁

> 后续修正：本文“登录首屏直接请求手机号”的部分已被 `CHG-20261029-011` 替代。已绑定微信必须先通过 code 恢复凭证，只有首次绑定才请求手机号，避免每次登录重复授权。

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261029-008 |
| 类型 | 需求 / 小程序体验修复 |
| 风险级别 | L1 |
| 状态 | DEV_VERIFIED_PENDING_USER |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `PI-20261029-003`、`USER-DEC-20261029-004` |

## 2. 目标与范围

- 目标：补齐首页业主场景，减少无必要授权步骤，修复未加入企业页图标一致性，建立员工/业主邀请直达门禁。
- 范围内：App 游客首页、登录页、员工邀请页、业主进入页、未加入企业页、邀请上下文。
- 非范围：后端接口、角色权限、金额、状态机、DDL、生产发布、正式小程序审核。
- 受影响端：微信小程序、App H5。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：不改变；沿用员工邀请和 CustomerAccess 业主授权。
- 业务事实来源：现有员工邀请、`CustomerAccess`、`USER-DECISION-20261015-001`、`USER-DEC-20261029-004`。
- 尚存 GAP：微信隐私弹窗和手机号授权为平台机制，无法通过页面代码消除。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 游客 | 未登录打开首页 | 看到业主专属场景和入口 | H5 390 截图 |
| 2 | 首次登录 | 未授权手机号 | 一个主行动直接请求平台手机号授权 | 登录页源码/截图 |
| 3 | 员工邀请 | 邀请链接 | 保存员工上下文，确认后进入企业首页 | 契约脚本 |
| 4 | 业主邀请 | 邀请/项目码 | 保存业主上下文，确认后进入业主首页 | 契约脚本 |
| 5 | 未归属用户 | 存在邀请上下文 | 不停留在未加入企业页，直接分流 | 代码门禁 |
| 6 | 未归属用户 | 无邀请上下文 | 展示统一图标风格的身份选择页 | H5 390 截图 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-app | `dev` | 未提交 | `src/domains/identity/inviteContext.ts`、`pages/index/index.vue`、`pages/auth/login.vue`、`pages/company/join.vue`、`pages/invite/accept.vue`、`pages/owner/enter.vue`、契约脚本 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| TypeScript | `npm run type-check` | PASS | 本记录 |
| 首页契约 | `npm run check:home` | PASS | 本记录 |
| 邀请契约 | `npm run check:invite-flow` | PASS | 本记录 |
| 导航权限 | `npm run check:navigation` | PASS | 本记录 |
| 客户端文案 | `npm run check:client-copy` | PASS | 本记录 |
| 小程序构建 | `npm run build:mp-weixin` | PASS | `dist/build/mp-weixin` |
| 分包 | `npm run check:subpackages` | PASS | 主包 14 页，17 分包 / 175 页 |
| H5 真实页面 | 390×844 | PASS | `docs/acceptance/runs/20261029-app-owner-entry-gate/` |
| 视觉预检 | qwen2.5vl:7b | PARTIAL | `company-join-390-vision-review.md` |

- 未执行项及原因：真实微信邀请接受链路需要用户使用真实微信/邀请账号验证。
- 已知失败及是否与本次相关：H5 直接 hash 跳转无法证明小程序 `reLaunch` 生命周期；以小程序真实链路为最终证据。

## 7. 发布

- 暂不发布；等待用户小程序开发版验证。

## 8. 回滚

- 回滚条件：邀请直达、业主登录或企业登录出现阻断。
- 回滚步骤：回退 App 上述文件至 `f2fb91e`；不涉及数据库。
- 实际是否回滚：否。

## 9. 成本与结论

- 最终结论：App 修改已完成类型、契约和构建验证；待真实小程序用户验证。

## 10. 复盘与沉淀

- 反模式：把微信身份资料授权与手机号授权叠加；把员工/业主邀请落入通用未加入企业页。
- 后续：观察真实邀请链路，必要时补统一邀请路由页。

## 11. 真实页面渠道

- 微信开发者工具：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/dist/dev`
- 小程序 API：`http://172.20.10.2:8090/api/`
- App H5：`http://localhost:8080/`
- 截图：
  - `output/playwright/zdq-auth-owner-gate/home-390.png`
  - `output/playwright/zdq-auth-owner-gate/login-390.png`
  - `output/playwright/zdq-auth-owner-gate/company-join-390.png`
