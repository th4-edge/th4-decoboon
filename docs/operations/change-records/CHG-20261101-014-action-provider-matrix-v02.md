# CHG-20261101-014 角色动作依赖矩阵 V0.2 与店长费用代填修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261101-014` |
| 类型 | 测试资产、质量门禁、缺陷 |
| 风险级别 | `L1` |
| 状态 | `DEV_VERIFIED_PENDING_USER` |
| 负责人 | codex |
| 日期 | 2026-11-01 |
| 关联问题 | `PI-20261101-004` |
| 业务事实 | `BR-057`、`DEC-091` |

## 2. 目标与范围

- 将动作依赖矩阵扩展到6个高风险动作；
- 新增运行时依赖Provider烟测；
- 修复店长费用代填员工选择器403；
- 保持财务不能代他人提交费用的边界。

## 3. 动作矩阵扩展

总动作数：6。本轮新增：

- 客户负责设计师指派；
- 项目成员调整；
- 财务安排付款；
- 财务主材下单；
- 企业费用代填员工选择。

## 4. 实现与变更

- 新增 `docs/testing/scripts/action_provider_smoke.py`；
- 动作矩阵增加依赖Probe参数；
- 资产校验要求允许角色的主/依赖Provider预期均为200；
- 新增 `TC-FIN-EXPENSE-PROXY-007`；
- Oracle新增 `BR-057`、`DEC-091`；
- `payroll/employee/page` 从编辑权限改为薪酬读取权限；
- 新增 `PayrollEmployeePickerAccessTest`。

## 5. 验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 动作依赖烟测修复前 | 17/18，店长403 | `raw/smoke-before-fix.json` |
| 允许角色依赖烟测修复后 | 18/18 PASS | `raw/smoke-after-fix.json` |
| 全角色依赖权限烟测 | 32/32 PASS | `raw/smoke-after-all-roles.json` |
| 定向测试 | PASS | `raw/targeted-tests.log` |
| 后端构建 | PASS | `raw/build.log` |
| Provider契约 | findings=0 | `raw/provider-contract.log` |
| 资产validate/self-test/trace/catalog | PASS | `raw/test-assets-*.log` |

## 6. 未完成

- App费用员工选择器手工/真机点击；
- 店长真实代填保存与提交人/归属员工留痕；
- 跨店候选负向页面证据；
- Web/App动作首层点击全量自动化仍待后续批次。

## 7. 结论

动态依赖烟测成功捕获了静态契约无法发现的第二个权限接缝BUG。修复后，矩阵内32项角色×依赖Provider检查全部符合预期：允许角色返回200且候选非空，拒绝角色返回约定错误码。继续保留用户手工复验门。
