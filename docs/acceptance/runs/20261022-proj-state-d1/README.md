# PROJ-STATE-D1 阶段验收模式继承 · 开发证据

日期：2026-10-22

范围：`PI-20261022-014` / `PROJ-STATE-D1`

结论：开发自测通过，未独立验收

## 改动

- `ProjectStage` 新增可空 `acceptanceMode`；历史来源缺失保持 `null`，页面显示“待核验”。
- 合同生效立项生成项目阶段时，从 `ContractConstructionStage.acceptanceMode` 原样复制。
- `project/stage/info|page|update` 与 `customer/stage/info|list` 增加 `acceptanceMode` 白名单。
- App 阶段详情、阶段计划；Web PM 工作面增加验收方式只读展示。
- 迁移 `125-项目阶段验收模式.sql` 按 `project_stage_id` 回填历史模式；无法关联的行不猜测。

## 验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 后端主代码编译 | PASS | `mvn -DskipTests compile` |
| 合同三模式进入项目阶段 | PASS | `DecoboonClosedLoopTest` 1/1 |
| 日期重排不改模式 | PASS | `ProjectPlanFromScheduleTest` 1/1 |
| 迁移编号/发布范围 | PASS | `MigrationNumberingTest` 1/1 |
| 阶段 Provider 白名单 | PASS | `DetailResponseWhitelistContractTest` |
| App 类型检查 | PASS | `npm run type-check` |
| App 微信构建 | PASS | `npm run build:mp-weixin` |
| Web 类型检查 | PASS | `npm run type-check` |
| Web 生产构建 | PASS | `npm run build` |

后端 Spring 定向测试在 `HEAD + D1 改动` 隔离副本执行，避免并行未完成代码污染；测试目录：`/tmp/proj-state-d1.19vYQv`。

## 未完成

- 125 未在 MySQL/dev/test/prod 执行。
- 未做真实接口回读、真实页面截图或真机走查。
- 未执行独立验收；开发自测不得替代验收。
- D2 三模式完成分流和 D3-D5 停复工仍未授权。

## MySQL 8.4 迁移验证（隔离库）

- 实例：本地 MySQL 8.4 `127.0.0.1:3307`。
- 隔离库：见 `api-and-db-evidence.json` 的 `mysql_migration.isolated_database`。
- 前向执行：PASS。
- 回填：`None / AcceptanceDone / ConstructionDone` 三条关联阶段正确回填；无合同关联阶段保持 `NULL`。
- 回滚：PASS，`acceptance_mode` 列移除。
- 未修改本机现有业务数据；隔离库仅含最小验证表。

## 真实 Provider 回读

在空验收库 `decoboon_accept` 中加入可识别的合成项目 `9101 / D1-PJ-001`，分别通过业主 dev 登录和员工密码登录调用真实接口：

| 接口 | 结果 |
| --- | --- |
| `customer/stage/list` | HTTP 200；4 阶段；`None / AcceptanceDone / ConstructionDone / null` 与数据库一致 |
| `customer/stage/info` | HTTP 200；`WATER_ELECTRIC = AcceptanceDone` |
| `project/stage/page` | HTTP 200；4 阶段；`None / AcceptanceDone / ConstructionDone / null` 与数据库一致 |

- 脱敏结果：`api-and-db-evidence.json`。
- 验收库 schema 备份：`backups/decoboon_accept-schema-pre-d1.sql`。
- 合成数据仅用于本地验收，项目编号 `D1-PJ-001`，未自动删除。

## Web 真实渲染

- 地址：`http://127.0.0.1:8080/pm/workbench?projectId=9101`
- 页面实际显示：
  - 开工交底：验收方式 无需验收
  - 水电施工：验收方式 业主验收
  - 泥瓦防水：验收方式 施工完成
  - 历史待核验：验收方式 待核验
- 截图：`screenshots/web-pm-workbench.png`
- 复验发现并修复：后端枚举对象 `{key,name}` 在 App/Web 被错误按字符串解析，修正后重新类型检查与构建通过。

## 当前结论

D1 开发证据已补全到真实 MySQL、真实 Provider 和 Web 页面。仍未完成 App 真机、独立验收签署和非生产目标库的迁移账本登记。

## App 微信开发者工具尝试（2026-10-23）

- `dist/dev/mp-weixin` 已用 `VITE_API_BASE=http://127.0.0.1:8090/api/` 重建，App API 地址正确。
- 微信开发者工具能打开项目和页面，但当前工具会话未执行 `pages/auth/login` 的 `devToken` 会话持久化分支；设置真实员工存储夹具后重开窗口也被工具清空。
- 因此本轮没有把 App 阶段详情伪记为通过。
- App 构建、分包和类型检查仍为 PASS；App 真机/真实员工会话验收保留给独立验收责任方。
