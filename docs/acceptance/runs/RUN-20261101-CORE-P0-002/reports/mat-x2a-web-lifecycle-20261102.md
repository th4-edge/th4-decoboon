# MAT-X2A Web 作业单维护生命周期自测

- 时间：2026-11-02
- 设计依据：`PI-20261022-015 / MAT-X2`
- Web 提交：`489948c`
- 环境：Web `http://127.0.0.1:5178`，后端 `http://127.0.0.1:8092/api`，隔离库 `decoboon_accept_20261101`
- 页面：`/workbench/material-sheets`

## 执行结果

### Designer

1. 从项目列表进入“主材作业单”，选择麦鸿演示项目12；
2. 从项目材料计划加入瓷砖计划；
3. 填写安装位置、空间、型号、颜色、尺寸和数量10㎡；
4. 保存草稿，服务端回读为已测量；
5. 编辑备注并保存；
6. 确认测量、配送、安装时间并保存；
7. 提交请购，状态变为待下单；
8. 导出作业单；
9. 取消临时验收单，原单保留为已取消；
10. API/DB 回读：计划来源 `Plan`、数量10、项目/房屋一致。

### 角色

- Pm：页面无新建/编辑/提交/取消，保留确认测量；越权新建返回业务403；
- Finance：页面无新建，主材作业单菜单可达；越权新建返回业务403；
- Designer：无财务下单入口，等待财务下单。

### 渲染

- 1440×900、1280×800、1024×768 均无横向溢出；
- 页面无内部项目编号、内部作业单号、技术实现文案或预览夹具；
- 当前会话无图像输入能力，视觉人工评审标记 `VISUAL_REVIEW_UNAVAILABLE`。

## 证据

- `evidence/TC-MAT-DESIGNER-SHEET-002/20-mat-x2a-designer-list.png`
- `evidence/TC-MAT-DESIGNER-SHEET-002/21-mat-x2a-create-dialog.png`
- `evidence/TC-MAT-DESIGNER-SHEET-002/22-mat-x2a-create-filled.png`
- `evidence/TC-MAT-DESIGNER-SHEET-002/23-mat-x2a-draft-created.json`
- `evidence/TC-MAT-DESIGNER-SHEET-002/24-mat-x2a-draft-created.png`
- `evidence/TC-MAT-DESIGNER-SHEET-002/25-mat-x2a-measure-dialog.png`
- `evidence/TC-MAT-DESIGNER-SHEET-002/26-mat-x2a-submitted.png`
- `evidence/TC-MAT-DESIGNER-SHEET-002/27-mat-x2a-cancelled.png`
- `evidence/TC-MAT-DESIGNER-SHEET-002/28-mat-x2a-api-readback.json`
- `evidence/TC-MAT-DESIGNER-SHEET-002/29-mat-x2a-db.txt`
- `evidence/TC-MAT-DESIGNER-SHEET-002/32-mat-x2a-role-negative.json`
- `evidence/TC-MAT-DESIGNER-SHEET-002/33-mat-x2a-final-1440x900.png`
- `evidence/TC-MAT-DESIGNER-SHEET-002/33-mat-x2a-final-1280x800.png`
- `evidence/TC-MAT-DESIGNER-SHEET-002/33-mat-x2a-final-1024x768.png`
- `evidence/TC-MAT-DESIGNER-SHEET-002/34-mat-x2a-final-responsive.json`

## 结论

`DEV_VERIFIED`。本切片只覆盖 Web 作业单维护、测量、请购、取消和导出；到场确认、App 真机、独立验收和发布仍待后续切片。
