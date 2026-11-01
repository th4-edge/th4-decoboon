# CHG-20261030-033 · App 方案模版真实页面验收与缺陷修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-033 |
| 类型 | 缺陷修复 / 真实页面验收 |
| 风险级别 | L2 |
| 状态 | 微信开发者工具真实页面复核通过；物理真机与独立验收待执行 |
| 负责人 | codex |
| 日期 | 2026-10-30 |
| 关联需求/裁决 | `DEC-084`、`DEC-050`、`BR-040`、`OBJ-005`、`DQ-X3/X4/X5`、`CHG-20261030-030~032` |

## 2. 目标与范围

- 目标：完成 App 方案模版库、编辑器、发布、实例化、复制、停用和新版本的微信开发者工具真实页面闭环，修复实际页面暴露的问题。
- 范围内：
  - App `templates/template-detail/template-edit` 真实页面与交互；
  - 模板 Provider 状态、价格白名单、参考尺寸复制和数据范围；
  - 真实页面截图、页面文本、API/DB 回读和控制台证据；
  - 合成测试数据清理与基线计数核对。
- 非范围：
  - 物理 iPhone/Android 真机；
  - 独立验收签字；
  - 生产发布、正式小程序提审；
  - 房屋删除 `GAP-WEB-025`。

## 3. 业务与决策

- 业务事实来源：`DEC-084`、`DEC-050`、`BR-040`、`OBJ-005`。
- 不改变模板状态机、权限范围、金额/成本口径或公式规则。
- 模板只承载区域、分项引用和工程量公式，不承载价格/成本。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 新建、区域、分项、发布 | 草稿可维护，发布有确认，发布后只读 | `20261030-dq-x3-x5-app-template-acceptance/raw/screenshots/03~10` |
| 2 | 详情与实例化 | 无价格展示，携带当前房屋生成方案并回读 | `32-template-detail-final-verified.png`、`33-design-scheme-final.png` |
| 3 | 复制/停用/新版本 | 参考值完整继承，版本幂等，停用后阻断 | `15~23`、`raw/api/03~06,09` |
| 4 | 错误与权限态 | 空公式、错误单位、引用区域删除、缺房屋、非维护者均阻断 | `24~29` |
| 5 | 数据清理 | 无合成数据残留，与备份基线一致 | `raw/db/cleanup.txt`、`raw/db/baseline-compare.txt` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交 | `designtemplate` Provider 修复、白名单/版本复制测试 |
| th4-decoboon-app | `dev` | 未提交 | `template-edit.vue`、`template-detail.vue`、真实页面验收包 |
| th4-decoboon-web | `dev` | 不适用 | 复用共享 Provider，无源码改动 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端定向测试 | `mvn -Dtest=DesignTemplateWhitelistContractTest,DesignTemplateVersionCopyTest,DesignTemplateInstantiateInheritanceTest,DesignTemplateFormulaContextTest test` | PASS | Maven 输出 |
| App 类型/契约/UI | `type-check`、`check:design-items`、`check:navigation`、`check:ui`、`check:client-copy` | PASS | 命令输出 |
| App 构建 | `npm run build:mp-weixin`、`check:subpackages` | PASS；总包 4,223,451 B、180 页 | 命令输出 |
| 真实页面 | 微信开发者工具 iPhone 16，393×754，SDK 3.17.5 | PASS | `20261030-dq-x3-x5-app-template-acceptance/run.json` |
| 控制台 | 最终列表/详情/方案页 | error=0、warning=0 | `raw/console-errors.json` |
| 数据清理 | 关闭临时登录态后按 FK 顺序删除 | PASS；30 表 0 mismatch | `raw/db/cleanup.txt`、`raw/db/baseline-compare.txt` |

- 未执行项及原因：未连接物理 iPhone/Android；视觉仅 `qwen2.5vl:7b` 机器预检，最终视觉签字和独立验收未执行。
- 已知失败及是否与本次相关：外部并发全量测试 1143 项出现 2 项 customer/labor 错误，与本轮模板定向测试无关；本轮不以其替代定向结论。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 未发布 |
| 上一版本/回滚点 | 当前后端/App 工作区 |
| 健康检查 | 本地后端 8090 `/api/` 200 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：模板参考值再次丢失、模板响应出现价格、停用仍可编辑/实例化、数据清理误伤非测试记录。
- 回滚步骤：恢复本变更涉及的模板 Provider 和页面文件；数据库使用 `~/.decoboon-local/template-acceptance-20261030/before.sql` 定向恢复。
- 回滚验证：后端模板定向测试、App type-check/build、模板列表/详情/实例化冒烟通过。
- 实际是否回滚：否。

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否；真实页面发现 7 个页面/契约缺陷并完成修复复验。
- 返工原因：确认弹窗宿主缺失、参考尺寸复制遗漏、状态展示取错层级、停用过滤与模板价格白名单未在静态构建中暴露。
- 最终结论：微信开发者工具原生小程序功能链路通过，进入 `UI_REVIEWED`；物理真机、最终视觉签字和独立验收仍待执行。

## 10. 复盘与沉淀

- 是否出现重复问题：是；新增跨端能力只做 type-check/build 会漏掉确认弹窗宿主、当前版本状态和复制字段完整性。
- 是否需要新增门禁：模板 Provider 已有价格白名单测试；版本/复制参考尺寸已补定向测试。真实页面门禁继续执行 `run.json` 与 console/API/DB 回读。
- 候选 `EXP/ANT/PATH`：
  - `EXP`：`currentVersion.status` 是编辑/详情的主状态，不可用模板主档状态替代；
  - `ANT`：只验证 Provider 路由共享就宣称模板双端验收；
  - `PATH`：先跑真实页面主链，再补负向与清理，最后更新领域账本。
- 后续动作与负责人：物理真机复核、最终视觉签字与独立验收。负责人：用户 + codex。
