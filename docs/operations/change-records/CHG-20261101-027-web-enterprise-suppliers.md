# CHG-20261101-027 · Web 企业运营供应商档案

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261101-027` |
| 类型 | 功能补齐 |
| 风险级别 | L2 |
| 状态 | 已完成 |
| 负责人 | Codex |
| 日期 | 2026-11-01 |
| 关联需求/业务裁决 | 用户要求新增 Web 企业运营分组和供应商管理页，并与 App API 同源；`BR-019` |

## 2. 目标与范围

- 目标：在 Web 增加“企业运营 → 供应商档案”工作面，支持供应商档案查询、查看、新增和编辑。
- 范围内：Web 静态路由、角色菜单、工作台首页入口、供应商 Provider 客户端和真实页面。
- 非范围：不新增后端接口；不改变 App 页面；不改变采购、应付、金额、成本、结算和权限规则；不建设第二套供应商数据。
- 受影响端：Web；App 作为同源 Provider 事实基准。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是。
- 业务事实来源：`BR-019`，业务概念 `supplier`（`stateMachineStatus=frozen`）；Provider 权限由后端强制。
- 同源 Provider：`partner/page`、`partner/save`、`partner/update`，与 App `src/domains/supply/api.ts` 完全一致。
- 角色：Boss、StoreManager、Finance 可维护；Pm 仅查看（与 App `view:supply` 能力一致）；Designer、Marketer 无供应商入口。
- 尚存 GAP：供应商旧 `PartnerPrice` 采购价已停止新增使用，本页不将其作为采购价维护入口。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | Finance 打开供应商 | 已登录且企业存在供应商 | 列表返回与 App `partner/page` 同源数据 | API/页面对账 |
| 2 | 服务端筛选 | 输入名称/联系人关键字 | 由 `keyword` 服务端过滤并按数据库分页 | 页面 1 行结果 |
| 3 | 查看供应商 | 点击已有供应商 | 抽屉展示联系人、品牌、账期、银行与应付余额 | 页面截图 |
| 4 | 新增/编辑入口 | Finance/Boss/StoreManager | 可打开表单；Pm 只读；写权限仍由 Provider 复核 | DOM/源码 |
| 5 | 三档渲染 | 1440/1280/1024 | 无横向溢出，企业运营分组和供应商入口可见 | 三档截图 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev | 工作区 | 变更记录 |
| th4-decoboon-web | dev | 工作区 | `src/domains/supply/*`、`src/views/supply/SupplierManagementView.vue`、角色静态路由/菜单/首页 |
| th4-decoboon-app | dev | 不修改 | 同源 Provider 事实基准 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| TypeScript | `npm run type-check` | PASS | 本记录 |
| 工作台治理 | `npm run check:workbenches` / `check:workspaces` | PASS | 本记录 |
| 表单/概念/分页 | `check:forms` / `check:concepts` / `check:pagination` | PASS | 本记录 |
| 完整构建 | `npm run build` | PASS，含类型、表单、概念、分页、文案、编号和品牌门禁 | 本记录 |
| 真实页面 | Finance `/workbench/suppliers` | PASS：企业运营分组与供应商档案可见；5 家供应商；查看/新增抽屉可用；关键字“东鹏”服务端筛选为 1 行；三档无溢出 | `logs/web-supplier-list-1440.png`、`-1280.png`、`-1024.png`、`web-supplier-view-drawer-1440.png` |
| App/Web 同源 | App/Web 均调用 `partner/page` | PASS：同一账号同筛选返回 5 家；Web 首行东鹏瓷砖与 App Provider 一致 | 本地 API 响应、页面 |

## 7. 发布

- Release ID：不适用
- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：菜单死链、供应商数据与其他角色范围不一致、写权限被前端放大或 Web 构建失败
- 回滚步骤：移除 Web 静态路由、菜单、首页入口、供应商 domain 和页面文件
- 实际是否回滚：否

## 9. 成本与结论

- 最终结论：开发自测通过；Web 供应商档案页已接入企业运营分组，读写均复用 App 同源 Provider；待用户视觉确认和独立验收
