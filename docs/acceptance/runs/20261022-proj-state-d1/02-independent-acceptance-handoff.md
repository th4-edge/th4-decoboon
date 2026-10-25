# PROJ-STATE-D1 独立验收交接

执行方：独立验收责任方

实现方不得自批

环境：本地 dev，MySQL `127.0.0.1:3307` / `decoboon_accept`，后端 `127.0.0.1:8090`，Web `127.0.0.1:8080`

## 测试身份与数据

- 合成项目：`projectId=9101`，编号 `D1-PJ-001`
- 业主：`18800000001`，仅本案 dev 登录
- 员工：`18800000002` / `D1Verify2026`
- 阶段：
  - `9201 PREPARE = None`
  - `9202 WATER_ELECTRIC = AcceptanceDone`
  - `9203 MASONRY = ConstructionDone`
  - `9204 LEGACY = null`

> 以上均为本地合成测试数据，不得用于生产。

## 必须覆盖

1. 业主 `customer/stage/list`、`customer/stage/info` 返回三种模式和未配置空值。
2. 员工 `project/stage/page` 返回相同四种状态。
3. Web `/pm/workbench?projectId=9101` 显示“无需验收 / 业主验收 / 施工完成 / 待核验”。
4. App 导入 `dist/build/mp-weixin`；需使用真实员工微信会话，进入项目阶段详情并显示同一个验收方式。当前开发者工具 `devToken` 会话入口未生效，不能以夹具绕过。
5. 历史 `null` 不得自动完成、不得猜测为任一模式。
6. 日期排期或项目更新不得改变 `acceptanceMode`。
7. 越权：非参与员工、其他企业、其他项目不可读取。
8. 页面/API 失败时显示错误或“暂不可核验”，不得显示空数组伪装成功。

## 证据要求

- Web 和 App 截图；
- 接口响应脱敏 JSON；
- 操作人、时间、commit/工作树标识；
- 正向/反向/越权结果；
- 独立验收签署。

## 禁止

- 以开发自测代替独立验收；
- 将“待核验”自动解释为 `None`；
- 在 D1 未验收前启动 D2 三模式完成分流；
- 执行生产迁移或发布。
