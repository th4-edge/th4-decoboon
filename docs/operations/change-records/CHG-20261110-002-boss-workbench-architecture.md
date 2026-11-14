# CHG-20261110-002 · 企业负责人经营总览页面架构修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261110-002 |
| 类型 | 页面架构修复 / 普通业务功能 |
| 风险级别 | L2 |
| 状态 | 开发完成，待视觉/用户确认 |
| 负责人 | Codex |
| 日期 | 2026-11-10 |
| 关联需求/事故/业务裁决 | `ANS-BIZ-20261101-002`、`SCN-016`、`DEC-032/BR-028`、`WEB-BATCH7-BOSS G1` |

## 2. 目标与范围

- 目标：把 Boss 首页从通用入口聚合页修成“风险与待办 → 经营结果 → 业务全景 → 明细与口径”的经营总览。
- 范围内：Boss 专属首页、Boss 首页路由、静态菜单标题、Boss Figma 壳层变体、Boss dashboard 前端契约、风险/指标/漏斗/财务摘要真实数据接线、三档真实页面验证。
- 非范围：后端 Provider、指标公式、风险处置动作、权限、状态机、金额/成本/利润算法、客户端其他角色首页。
- 受影响端：Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及 Boss 可见经营指标与风险的展示顺序，但不改变任何口径和权限。
- 业务事实来源与裁决编号：`ANS-BIZ-20261101-002` 已确认 Boss 首页按“风险与待办 → 经营结果 → 明细/口径”组织。
- 尚存 GAP 或假设置：`GAP-070` 未关闭，不新增公司/门店经营利润金额；未完成口径核对的最终报价接受率暂不在首页展示。

## 4. 研发前置判断

- 本轮目标：企业负责人打开首页后先识别风险与待办，再查看经营结果，并能进入现有工作面处置。
- 业务闭环：进入经营总览 → 识别风险/待办 → 查看 KPI 与漏斗 → 进入风险、财务、客户或项目工作面 → 返回后有真实回读路径。
- 系统事实源：风险 `statistic/risk/report`；指标 `statistic/boss/dashboard`；漏斗 `statistic/boss/business/report`；资金健康度 `finance/summary/report`；岗位视角入口清单 `src/roles/workbenchHome.ts`。
- 操作减负：不再要求用户从通用入口页自行寻找经营结果与风险入口；首页直接给出行级风险和唯一主行动。
- 能力边界：`INTEGRATED`，复用既有 Provider，不新增接口。
- 实时性 SLO：页面打开和手动刷新时拉取；各区独立失败，不用实时推送伪装。
- 准入状态：`READY`。

## 5. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | Boss 全部门店 | 真实登录 | 首屏出现紧凑风险条、4 列 KPI，并开始看到主辅两栏业务全景；主栏与右栏桌面等高 | 1440 截图 |
| 2 | Boss 单门店 | 门店筛选 | 指标和漏斗按门店重新取数，风险明确企业范围 | 浏览器切换验证 |
| 3 | 风险失败 | Provider 失败 | 风险区“暂不可确认”，指标和漏斗不阻断 | 代码分区与页面状态 |
| 4 | 指标失败 | Provider 失败 | 指标区失败，风险与漏斗不阻断 | 代码分区与页面状态 |
| 5 | 成本不完整 | `complete=false` | 项目毛利显示“暂不可确认/成本未完整” | 页面对账 |
| 6 | 岗位视角 | 切换店长/业务员/设计/PM/财务 | 只切工作面，不改身份和权限 | 浏览器切换验证 |
| 7 | 下钻 | 点击主行动/风险/分析入口 | 分别进入风险队列、经营分析、财务台账 | 浏览器路由验证 |
| 8 | 响应式 | 1440/1024/390 | 无整页横向溢出 | 三档截图与宽度检查 |

## 6. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | 当前工作区 | 待提交 | `docs/operations/change-records/CHG-20261110-002-boss-workbench-architecture.md` |
| th4-decoboon-web | `codex/finance-role-high-risk-permissions-20261102` | 待提交 | `src/views/workbench/BossWorkbenchHomeView.vue`、`src/domains/boss/{model,api}.ts`、`src/roles/routes.ts`、`src/roles/workbenchConfig.ts`、`src/layouts/RoleAreaShell.vue`、`src/components/shell/RoleSideNav.vue`、`src/views/workbench/BossOverviewView.vue`、Boss 架构文档、三档证据 |
| th4-decoboon-app | 不适用 | 不适用 | 本轮不修改 |

## 7. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 类型检查 | `npm run type-check` | PASS | 命令输出 |
| 工作台静态清单 | `npm run check:workbenches` | PASS | 命令输出 |
| 壳层契约 | `npm run check:shell-layout` | PASS | 命令输出 |
| 页面系统 | `npm run check:page-system` | PASS | 命令输出 |
| 指标注册 | `npm run check:metrics` | PASS（存量非阻断差异仍提示） | 命令输出 |
| 真实页面 | Boss `13910002001`，`/workbench` | PASS：Figma 壳层 222/70；1440 内容 1150；KPI 位于 y=206；主辅栏各 654px 高，左 841 / 右 295；重点推进 3 条；风险 13；漏斗 8/0/3/4/4；无整页横向溢出 | `docs/operations/evidence/20261110-boss-workbench-architecture/` |
| 下钻 | 风险 / 经营分析 / 财务台账 | PASS：分别到 `/workbench/risk`、`/workbench/overview`、`/finance/ledger` | 浏览器验证 |
| 岗位视角 | 负责人切换店长视角 | PASS：显示店长工作面，身份仍为负责人 | 浏览器验证 |
| 生产构建 | `npm run build` | PASS：全部 Web 门禁与 Vite 构建通过；仅有既有 Dart Sass legacy API 弃用告警 | 命令输出 |

- 未执行项及原因：尚未做独立视觉验收和用户签字；尚未模拟单接口故障的真实浏览器截图。
- 已知失败及是否与本次相关：`check:metrics` 提示存量 `quoteFinalAcceptanceRate` 与 `monthReceiptAmount` 口径差异，非本次引入；因此首页未展示最终报价接受率。

## 8. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用（本地开发验证） |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 未发布 |
| 上一版本/回滚点 | 当前 Web 开发工作区 |
| 健康检查 | 待发布时执行 |
| 发布后观察 | 待发布时执行 |

- 数据库迁移：无
- 配置/密钥变化：无

## 9. 回滚

- 回滚条件：Boss 首页无法进入、权限/数据范围错误、指标明显串数或视觉方向被用户否决。
- 回滚步骤：把 Boss 路由恢复为 `L.wbHome`，移除 `BossWorkbenchHomeView.vue` 和本轮 dashboard 契约改动。
- 回滚验证：Boss 登录后回到原通用工作台，其他角色不受影响。
- 实际是否回滚：否

## 10. 结论与复盘

- 可运行：是。
- 业务通过：否，待用户按真实经营场景确认入口顺序和主行动。
- 视觉通过：否，待用户查看三档真实截图。
- 客户签字：未取得。
- 候选知识：暂不沉淀，等待用户反馈后再判断是否形成可复用页面架构经验。
