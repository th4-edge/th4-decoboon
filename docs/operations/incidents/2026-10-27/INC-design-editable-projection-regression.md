# 设计方案编辑权限投影发布回归

- 状态：fixed
- 分类：code_bug
- 级别：L3（权限投影，不新增授权范围）
- 用户报告：2026-10-27
- 修复提交：`fcd711b`
- 修复 release：`/opt/decoboon/releases/20261027-103227`

## 现象

线上设计师打开状态为“设计中”的设计方案，顶部“新增分项”等写操作按钮被禁用，点击无响应；页面空态入口仍可打开选择器，但已有分项时无法正常继续维护。

## 根因

- Web 已改为消费 `design/scheme/info.editable`，并据此控制设计工作区写操作。
- `20261026-110210` 后端版本包含 `editable` 投影，但随后 `20261026-234703`、`20261027-001549` 从尚未合入 `6b77784` 的生产分支重新构建，覆盖了该修复。
- 前端将缺失字段归一化为 `false`，导致方案虽为 `Draft`，写按钮仍被禁用。

## 修复

- 在最新生产代码基线上恢复服务端派生的 `editable`：
  - 仅 Boss、StoreManager、TeamLeader、Designer 可编辑；
  - 归档方案不可编辑；
  - 房屋存在 `Accepted/Converted` 报价或唯一项目时不可编辑；
  - 不改变既有写接口权限，不放宽数据范围。
- 新增 `DesignWritePermissionTest` 与方案详情公式回归断言。

## 验证与发布

- 定向测试：`DesignWritePermissionTest`、`DesignSchemeInfoFormulaTest` PASS。
- 全量后端：244 个套件、945 个用例，0 失败、0 异常、0 跳过。
- 发布包：thin boot JAR 校验 PASS；SHA-256 `2375bf5c6586f6b4aea1d984e86be8bdf566e00c327bfeadaee0d68623acb9bf`。
- 生产 release：`/opt/decoboon/releases/20261027-103227`。
- 上一版本：`/opt/decoboon/releases/20261027-001549`。
- 健康检查：HTTP 200，服务 active，生产 JAR 已包含 `editable`。
- 故障方案数据核对：`Draft / Designer / 确认报价 0 / 项目 0`，符合 `editable=true`。
- 数据库：无 DDL、无迁移、无数据变更。

## 回滚

```bash
bash scripts/rollback-prod.sh /opt/decoboon/releases/20261027-001549
```
