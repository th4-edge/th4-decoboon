# CHG-20261027-001 · 设计分项编辑权限投影热修

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261027-001 |
| 类型 | 线上缺陷 / 热修 |
| 风险级别 | L3（权限投影，未放宽权限） |
| 状态 | 已发布 / 待观察 |
| 负责人 | Codex |
| 日期 | 2026-10-27 |
| 关联需求/事故/业务裁决 | `docs/operations/incidents/2026-10-27/INC-design-editable-projection-regression.md` |

## 2. 目标与范围

- 目标：恢复“设计中”设计方案的 `editable` 权限投影，使设计师可正常新增和维护分项。
- 范围内：`design/scheme/info`、设计工作区写操作权限投影、发布基线门禁。
- 非范围：报价、项目、合同、金额计算、数据库结构和相邻邀请/量房问题。
- 受影响端：后端；Web 消费既有契约。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是（权限投影，不扩权）
- 业务事实来源与裁决编号：既有设计写权限规则；Boss/StoreManager/TeamLeader/Designer 可写，归档或已有确认报价/项目时锁定。
- 尚存 GAP 或假设置：无

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 设计师/设计中方案 | Draft、Designer、无确认报价、无项目 | `editable=true`，新增分项可用 | 生产数据条件核对、单元测试 |
| 2 | 锁定方案 | 已确认报价或已有项目 | `editable=false` | `DesignWritePermissionTest` |
| 3 | 归档方案 | Archived | `editable=false` | `DesignWritePermissionTest` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `hotfix/design-editable-prod-20261027` | `fcd711b`、`ac80b1b`、`4a2798c`、`ede3068` | `DesignContext`、`Design`、`scheme/info/Info`、发布门禁 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 定向测试 | `DesignWritePermissionTest,DesignSchemeInfoFormulaTest` | PASS | Maven surefire |
| 全量测试 | `mvn -o -q test` | 244 套件 / 945 用例 / 0 失败 | Maven surefire |
| 发布包门禁 | `verify-release-boot-jar.py`、`verify-release-contracts.py` | PASS | 构建日志 |
| 基线门禁 | `verify-release-baseline.sh --build/--deploy` | PASS / 负向 FAIL 已验证 | 构建与脚本记录 |
| 生产数据条件 | 故障方案 `Draft / Designer / 确认报价 0 / 项目 0` | 符合 `editable=true` | 生产只读查询 |

- 未执行项及原因：发版复核时原浏览器登录态过期，未完成按钮视觉复验；需用户重登后验证。
- 已知失败及是否与本次相关：无。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 20261027-103227 |
| 制品路径/SHA-256 | `2375bf5c6586f6b4aea1d984e86be8bdf566e00c327bfeadaee0d68623acb9bf` |
| 生产 release/版本 | `/opt/decoboon/releases/20261027-103227` |
| 上一版本/回滚点 | `/opt/decoboon/releases/20261027-001549` |
| 健康检查 | HTTPS 200，服务 active，生产 JAR 含 `editable` |
| 发布后观察 | 待观察 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：登录后设计工作区仍不可编辑，或出现新的权限异常。
- 回滚命令/步骤：`bash scripts/rollback-prod.sh /opt/decoboon/releases/20261027-001549`
- 回滚验证：健康检查 200，确认锁定方案只读规则未变化。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：约 20 分钟定位与发布验证
- 一次通过：否（首次热修未被后续发布回灌）
- 返工原因：发布分支基线漂移
- 最终结论：已发布 / 待观察

## 10. 复盘与沉淀

- 是否出现重复问题：是（11:02 修复被 23:47、00:15 发布覆盖）
- 是否需要新增门禁：是；已新增跨端契约门禁与发布基线门禁，均有负向验证
- 候选 `EXP/ANT/PATH`：待过程负责人复盘
- 后续动作与负责人：用户重登后完成真实页面按钮复验；后续发布使用统一变更记录
