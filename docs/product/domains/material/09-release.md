# 材料履约与材料业务助手 · 发布记录

## B2 范围

- 后端：只读 `blueprint/runtime/info`、`blueprint/check/report`；
- Web：`/business-blueprints`、项目核算页材料体检；
- App：本批未改；
- 数据库/迁移：无；
- 配置：无。

## 环境与版本

- 环境：本地 dev 验证；
- 版本/提交：后端 `fdc42ab`；Web `7547f96`；
- 发布路径：未发布生产；
- 上传信息：不适用。

## 冒烟

| 用例 | 结果 | 证据 |
| --- | --- | --- |
| 健康检查 | 本地后端启动成功 | `/tmp/zdq-backend.log` |
| 核心链路 | `runtime/info` + `check/report` 真实数据 PASS | `evidence/B2/api-*.json` |
| 权限/租户 | Provider 角色门禁 + 项目数据范围复用既有上下文 | `MaterialBlueprintRuntimeTest` |
| 回滚验证 | 本批无数据写入、无迁移；移除只读 Provider/页面入口即可回滚 | 本文件 |

## 回滚

- 后端回退本批 Provider、上下文与资源；
- Web 回退上一静态构建；
- 不涉及数据回滚。

## 发版边界

- 小程序体验版：本批未改；
- 小程序正式版：仍由用户本人提审和发布。
