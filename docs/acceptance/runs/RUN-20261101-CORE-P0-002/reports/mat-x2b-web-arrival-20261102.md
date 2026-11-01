# MAT-X2B Web 到场确认自测

- 时间：2026-11-02
- Web 提交：`ec3da3b`
- 环境：Web `http://127.0.0.1:5179`，后端 `http://127.0.0.1:8092/api`，隔离库 `decoboon_accept_20261101`
- 页面：`/workbench/material-sheets`

## 执行结果

1. Designer 打开已下单作业单，进入“确认到场”；
2. 弹窗读取采购明细，显示采购量20㎡、已到场0、待收20；
3. 首批登记8㎡，作业单变为部分到场，采购明细累计8；
4. 再次打开显示待收12㎡；输入13㎡被页面和 Provider 同时拒绝；
5. 尾量登记12㎡，作业单变为已到场，采购和采购明细均为已到货；
6. 到场记录共2条：8+12，合计20；
7. 应付仍为原下单形成的1条1760，receiptDate=2026-11-02，dueDate=2026-12-02；
8. 已知采购价88，实际成本1760；材料计划需求30中已到场20，计划状态为部分到场；
9. 到场默认日期为本地时区 `2026-11-02`；
10. 1440/1280/1024 无横向溢出。

## 证据

- `evidence/TC-MAT-FINANCE-ORDER-004/40-mat-x2b-arrival-entry.png`
- `evidence/TC-MAT-FINANCE-ORDER-004/41-mat-x2b-arrival-dialog.png`
- `evidence/TC-MAT-FINANCE-ORDER-004/42-mat-x2b-partial-filled.png`
- `evidence/TC-MAT-FINANCE-ORDER-004/43-mat-x2b-partial-result.json`
- `evidence/TC-MAT-FINANCE-ORDER-004/44-mat-x2b-partial-result.png`
- `evidence/TC-MAT-FINANCE-ORDER-004/45-mat-x2b-over-limit-page.png`
- `evidence/TC-MAT-FINANCE-ORDER-004/46-mat-x2b-over-limit-api.json`
- `evidence/TC-MAT-FINANCE-ORDER-004/47-mat-x2b-arrived-result.json`
- `evidence/TC-MAT-FINANCE-ORDER-004/48-mat-x2b-arrived-result.png`
- `evidence/TC-MAT-FINANCE-ORDER-004/49-mat-x2b-api-readback.json`
- `evidence/TC-MAT-FINANCE-ORDER-004/50-mat-x2b-db.txt`
- `evidence/TC-MAT-FINANCE-ORDER-004/51-mat-x2b-1440x900.png`
- `evidence/TC-MAT-FINANCE-ORDER-004/51-mat-x2b-1280x800.png`
- `evidence/TC-MAT-FINANCE-ORDER-004/51-mat-x2b-1024x768.png`
- `evidence/TC-MAT-FINANCE-ORDER-004/52-mat-x2b-responsive.json`
- `evidence/TC-MAT-FINANCE-ORDER-004/53-mat-x2b-arrival-date-local.png`

## 结论

`DEV_VERIFIED`。Web 到场确认与部分到场已闭合；App 真机、独立验收和异常退换仍待后续。
