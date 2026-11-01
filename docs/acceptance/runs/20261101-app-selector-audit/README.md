# App 全量选择器语义审计与剩余 P1（开发自测）

> 状态：`DEV_SELF_CHECK`，不是独立验收结论。
> 变更：`CHG-20261101-022`
> 日期：2026-11-01
> 业务依据：`DEC-021` / `BR-019`、`BR-059` / `OBJ-010`、`DEC-095` / `BR-060`

## 1. 范围

- 新增全 App 业务对象选择器语义审计 `npm run audit:selectors`。
- 写汇报项目改为 `report-project-picker`。
- 量房房屋改用 `property-picker`，多套房不自动选第一套。
- 客户负责人/设计师、项目岗位和项目筛选设计师统一接入 `employee-picker`。
- 内容任务模板、费用编辑分类改为专用分页选择页。
- 不修改业务对象、状态、权限、金额或数据终态。

## 2. 结果

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 全量语义审计 | PASS，263 文件，硬违规 0 | `evidence/semantic-audit.txt` |
| App 类型检查 | PASS | `npm run type-check` |
| App 分包构建 | PASS | `dist/build/mp-weixin` |
| 选择器契约 | PASS | `npm run check:selectors` |
| UI/文案/分页/分包 | PASS | 对应 npm 脚本 |
| 后端定向测试 | PASS | 5 个 Picker Page 测试 |
| 后端编译 | PASS | Maven 输出 |
| Provider 契约 | PASS，0 findings | 响应契约审计 |
| 真实 API | PASS | `evidence/api-contract.json` |
| 动态页 → 汇报项目 → 汇报表单 | PASS（开发自测） | `dynamic-to-report-project-picker-390.png` |
| 量房页 → 房屋选择 → 回填 | PASS（开发自测） | `measure-property-picker-entry-390.png` |
| 客户/项目人员 → 员工选择器 | PASS（开发自测） | 页面快照、契约 |
| 模板/分类 21 条加载更多 | PASS（夹具 UI） | 21 条截图、加载后 21 条文本 |
| 模板/分类 205 条首屏与布局 | PASS（夹具 UI） | `web-render.json`、205 条截图 |
| 320/390 布局 | PASS，无横向溢出 | `web-render.json` |
| 图像视觉评审 | `VISUAL_REVIEW_UNAVAILABLE` | 当前模型无图像输入能力 |

## 3. 证据

- `evidence/semantic-audit.txt`
- `evidence/api-contract.json`
- `evidence/web-render.json`
- `evidence/screenshots/template-picker-fixture21-390.png`
- `evidence/screenshots/template-picker-fixture205-320.png`
- `evidence/screenshots/template-picker-fixture205-390.png`
- `evidence/screenshots/expense-category-picker-fixture21-390.png`
- `evidence/screenshots/expense-category-picker-fixture205-320.png`
- `evidence/screenshots/expense-category-picker-fixture205-390.png`
- `evidence/screenshots/report-project-picker-real-320.png`
- `evidence/screenshots/report-project-picker-real-390.png`
- `evidence/screenshots/dynamic-to-report-project-picker-390.png`
- `evidence/screenshots/measure-property-picker-entry-390.png`
- `evidence/screenshots/measure-property-result-320.png`
- `evidence/screenshots/measure-property-result-390.png`

## 4. 未覆盖

- 当前租户内容模板和费用分类为 0；21/205 条状态是 UI 夹具，不作为业务事实。
- 未提交真实客户人员变更、项目岗位变更、内容任务或费用草稿，避免改变业务数据。
- 未做所有选择器的 10× 后端数据量、P95、0/21/200+ 全矩阵和微信真机复验。
- 未独立验收、未上传体验版、未发布。
