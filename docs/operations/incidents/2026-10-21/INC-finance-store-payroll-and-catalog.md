# 财务本店薪酬 403 与报价项目库缺失

- 状态：fixed
- 分类：code_bug
- 级别：L3（权限、薪酬与成本敏感数据）
- 首次用户报告：2026-10-21
- 修复 release：
  - 后端：`/opt/decoboon/releases/20261021-141548`
  - Web：`/opt/decoboon-web/releases/20261021-141716`

## 现象

1. 财务打开 Web「本店薪酬」提示“无权限”。
2. 财务工作台没有「报价项目库」菜单，无法维护报价分项标准成本及材料价、人工价、固定总价。

## 根因

- `payroll/store-payroll/page|info` 使用店长专属角色门禁，财务未进入可读角色集合。
- Finance 静态菜单与路由未声明 `/designer/catalog`。
- `catalog/item/save|update` 与成本字段可见性仅允许 BOSS / 店长，财务被错误排除。

## 生产证据

- 生产日志在 10:26:50–10:31:55 共记录 9 次：
  `catalog/item/update处理失败:当前账号无权维护报价项目库`
- 用户报告财务本店薪酬页面被角色门禁拒绝。

## 修复

- 后端 `365030b`：本店薪酬读取放行 BOSS / 财务 / 店长，数据范围继续由服务端裁决。
- 后端 `82dcd21`：报价库价格维护放行财务；标准成本字段向财务返回；删除与批量导入权限不外扩。
- Web `44518f8`：增加财务报价库菜单、首页入口、成本详情及新增/编辑维护抽屉。

## 验证

- 后端全量测试 895/895 通过。
- Web 类型检查、构建、菜单可达、权限、门店范围、表单门禁通过。
- 生产 health 200、服务 active、Nginx 通过。
- `payroll/store-payroll/page` 与 `catalog/item/page` 未登录均返回 `tokenIsUndefined`，非 404。
- 线上 Web chunk 已确认包含报价库财务维护入口与标准成本字段。
- 发布后同一报价库权限错误指纹未在新版本启动后增长。

## 回滚点

- 后端：`/opt/decoboon/releases/20261021-133811`
- Web：`/opt/decoboon-web/releases/20261021-133900`
