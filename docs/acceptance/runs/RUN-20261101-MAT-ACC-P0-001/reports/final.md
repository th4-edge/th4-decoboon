# RUN-20261101-MAT-ACC-P0-001 材料与验收 P0 报告

## 结果

| 总数 | PASS | FAIL | BLOCKED | NOT_RUN |
| ---: | ---: | ---: | ---: | ---: |
| 2 | 2 | 0 | 0 | 0 |

## 用例

### TC-MAT-DESIGNER-SELECTION-001 PASS

- 未选材直接建单：400“该材料尚未完成主材选材，完成后方可下单”。
- 负向失败后 Plan 仍为 `Planned`，未生成作业单。
- Designer Web 保存并交付选材后，Plan 变为 `ReadyToOrder`。
- 同一材料计划随后成功生成 Draft 作业单 `1793517336944847641`。

### TC-ACC-PERMISSION-EMPLOYEE-002 PASS

- Boss、StoreManager、Finance、Pm 调 `project/acceptance/update` 均返回 403。
- 验收单保持 `Pending`，`confirmed_at` 和 `confirmed_by` 为空。
- PM Web 页面明确“业主确认必须由业主本人执行”，没有员工确认或驳回动作。

## 限制

- 本 RUN 的 Web/API 证据完整。
- App 选材页和员工验收页未在本 RUN 单独执行；员工验收的同一后端 Provider 已对四类员工角色统一拒绝。
- 因此本批次结论为 `CONDITIONAL_PASS`：核心门禁已验证，App 页面级覆盖仍待补。

## 结论

`CONDITIONAL_PASS`
