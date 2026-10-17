# 材料履约与材料业务助手 · 实现记录

## 本批范围

- Batch：B1
- 垂直切片：业务事实核对、结构化蓝图 v1、字段字典与对账矩阵
- 不包含：Web/App 页面实现、材料业务助手运行时、独立验收、正式发布

## 变更

- 后端：无新增代码；核对 `97d5705` 已实现的模板字段投影、提交必填门禁与 `material/sheet/cancel`。
- App：无新增代码；后续 B3 需要验证模板字段渲染、必填提示和取消动作。
- Web：无新增代码；后续 B2 需要验证蓝图中心与项目材料业务体检。
- 迁移：无。
- 配置：无。

## 测试与证据

- 定向测试：`mvn -q -Dtest=MaterialSheetOrderFlowTest,MaterialSheetTemplateFieldConfigTest,MaterialSheetEnumsProviderTest,MaterialSheetTemplateWhitelistTest,MaterialSheetResponseProjectionContractTest test`，2026-10-17 通过。
- 构建/类型检查：定向测试阶段完成测试编译；全量构建留到 B2/B3。
- 真实页面/真机：本批未做，不得据此宣称 UI 通过。
- 部署/重启：本批无部署。
- 截图/日志：复用已有材料蓝图截图，仅作产品可视化，不充当实现验收证据。

## 风险与未闭环

- 模板基线目前只有 Tile；其他品类不能宣称已具备同等字段门禁。
- 页面是否已正确使用 `headerFields/lineFields`、提交错误是否能恢复、取消按钮是否只在允许状态出现，仍待 UI 审查。
- 独立验收尚未执行。
- `GAP-MAT-TIME-SEMANTICS` 仍需业务裁决，当前不新增时间字段。
