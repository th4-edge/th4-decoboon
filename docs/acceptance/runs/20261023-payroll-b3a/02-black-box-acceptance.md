# B3A 工资单现有执行链跨端对齐 · 黑盒验收记录

## 1. 环境

- 后端：Java 22，本地 `127.0.0.1:8090`
- Web：Vite `127.0.0.1:8085`
- 数据：本地开发数据目录克隆出的独立库 `33332 / decoboon_b3a_20261023`
- 角色：BOSS、总部财务、店长
- 生产：未访问
- DDL：无

## 2. 结论

`USER_ACCEPTED_NON_INDEPENDENT / BLACK_BOX_PASS`

- API：`20 PASS / 0 FAIL`
- Web：BOSS / Finance / StoreManager 三角色 PASS
- Console error：`0`
- 隔离 JDK22 全量：`932 / 0 / 0 / 0`

## 3. API 与数据库结果

| 用例 | 结果 |
| --- | --- |
| BOSS 读取工资单详情 | `Confirmed`、考勤已完整、应发/扣款/实发为 `21000/0/21000`，PASS |
| BOSS 读取工资项 | 5 条，金额字段、员工、归属、状态完整，PASS |
| Finance 读取详情与工资项 | PASS |
| StoreManager 读取本店详情与工资项 | PASS |
| Finance 最终确认 | 403，PASS |
| Finance 删除工资单 | 403，PASS |
| Finance 生成草稿 | 成功，PASS |
| StoreManager 最终确认/删除/生成 | 403，PASS |
| BOSS 删除已确认工资单 | 409，PASS |
| BOSS 重复确认 | 成功且应付/工资条计数保持 `5/5`，PASS |
| BOSS 生成并删除草稿 | 成功，删除后详情 404，PASS |
| 表头金额与工资项汇总 | 应发、扣款、实发三组完全一致，PASS |
| StoreManager 读取他店已知 `sheetId` | 404，PASS |
| 总部 Finance 读取他店工资单 | 成功，PASS |

证据：`api-results.json`、`http/`、`sql/`。

## 4. Web 页面结果

### BOSS

- 打开 2026-09 草稿工资单后调用详情接口并显示完整摘要；
- 工资项展示基本工资、提成、奖金、补贴、考勤扣款、社保、个税、其他扣款、应发、扣款、实发、状态、归属和备注；
- 点击确认出现二次确认：“确认后系统将按实发工资生成员工应付并计入项目/企业成本；工资条在发放完成后才对员工可见”；
- 确认成功后工资单状态更新为“已确认”，工资项状态同步为“已确认”，显示确认人和时间；
- Draft 工资单显示“删除草稿”，二次确认后列表和详情不再出现该工资单。

### Finance

- 可查看工资单详情和完整工资项；
- 不显示“确认工资单”和“删除草稿”。

### StoreManager

- 页面显示“本店财务只读”；
- 可查看本店工资单详情和完整工资项；
- 不显示生成工资单、确认工资单、删除草稿。

截图：

- `screenshots/web-boss-sheet-detail.png`
- `screenshots/web-boss-confirmed.png`
- `screenshots/web-boss-deleted-draft.png`
- `screenshots/web-finance-sheet-list.png`
- `screenshots/web-finance-sheet-detail.png`
- `screenshots/web-manager-sheet-list.png`
- `screenshots/web-manager-sheet-detail.png`

## 5. 自动化门禁

- B3A 定向测试：`5/0/0/0`
- 隔离 JDK22 全量：`932/0/0/0`
- Web type-check / build：PASS
- 权限、门店范围、工作台、客户端文案门禁：PASS

## 6. 边界与遗留

- 本次为同一执行会话完成开发后的黑盒验证，不冒充独立验收。
- 未发布生产，未执行 DDL。
- 本批不覆盖工资单复核、作废/退回、员工发放权限。
- App 薪酬方案筛选、提成报告、工资条详情等剩余差异归 B4。
- B3B/B4 仍受 `GAP-060`、员工发放权限和复核/作废设计阻塞。
