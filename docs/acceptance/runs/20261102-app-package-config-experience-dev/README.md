# 20261102 App 套餐查看与配置体验开发验证

> 状态：`DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE`
> 变更：`CHG-20261102-025`
> 性质：开发自测与真实微信开发者工具页面走查，不是独立验收。

## 1. 验证环境

- 后端：本地 `dev` Profile，`http://127.0.0.1:8090/api`
- 数据库：本地 MySQL `decoboon`
- App：uni-app 开发构建 `dist/dev/mp-weixin`
- 运行环境：微信开发者工具 + `miniprogram-automator`
- 验证账号：企业负责人 BOSS（审计测试账号）
- 套餐夹具：企业 `199500000000000001` 的 V1/V2/V3 套餐

## 2. 页面结果

| 场景 | 预期 | 结果 |
| --- | --- | --- |
| 套餐管理列表 | 草稿、当前发布、历史版本均显示，具备“查看详情” | PASS |
| 已发布套餐详情 | 显示当前发布 V2、分区、4 个项目、人工/材料价格 | PASS |
| 企业分区列表 | 分区步骤使用企业已有分区，全宽卡片展示，有无横向滚动容器 | PASS |
| 项目价格与类型分组 | 显示施工 4 项、人工 ¥6.80，材料+人工项目同时展示两类价格 | PASS |

合计：4/4 PASS。

说明：套餐列表和详情通过真实页面导航打开；分区与项目步骤因微信开发者工具自动化无法穿透 uni-app 自定义组件选择器，使用页面状态回读并切换到对应步骤完成真实渲染复核。该方式不替代后续物理点击和真机验收。

## 3. 证据

- `evidence/walkthrough.json`：页面路径、文本回读、步骤可见状态和 PASS 结论。
- `evidence/01-package-manage.png`
- `evidence/02-published-detail.png`
- `evidence/03-enterprise-sections.png`
- `evidence/04-project-groups.png`

## 4. 自动化门禁

| 检查 | 结果 |
| --- | --- |
| App `verify:mp-weixin` | PASS |
| App `check:package-config -- --self-test` | PASS，19 个负向夹具被拒绝 |
| App `check:ui` | PASS |
| App `check:brand-theme` | PASS |
| 后端定向测试 | PASS |
| 后端全量 `mvn -q test` | PASS，315 个测试类 / 1224 个用例，0 失败、0 错误、0 跳过 |
| Provider 响应审计 | PASS，findings=0 |

## 5. 未执行与边界

- 未做物理真机验证；当前证据为微信开发者工具真实渲染。
- 未执行独立验收，不得据此宣称生产验收或发布完成。
- 未改变套餐金额、成本、权限、状态机和数据库结构。
- 未确认人工视觉观感；截图用于后续独立验收复核。

