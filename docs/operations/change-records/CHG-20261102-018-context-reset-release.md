# CHG-20261102-018 · 上下文重置三端发布

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-018 |
| 类型 | 运维 / 发布 |
| 风险级别 | L0 |
| 状态 | 已发布 |
| 负责人 | Codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | 用户要求推送分支、合并并发版 |

## 2. 范围

- 后端：归档、问题台账行政关闭、Agent 分层文档；合并最新 `origin/dev`。
- Web：旧状态/Skill 归档、AGENTS 分层。
- App：旧资料归档、AGENTS 分层、1.0.78 开发版重新构建上传。
- 非范围：业务规则、权限、金额、数据库结构、小程序正式审核与发布。

## 3. 发布证据

| 端 | 运行提交 | Tag | Release/上传 |
| --- | --- | --- | --- |
| 后端 | `75a2dd0e` | `v2026.11.02.3` | `/opt/decoboon/releases/20261102-042648` |
| Web | `6814e2e` | `v2026.11.02-web.2` | `/opt/decoboon-web/releases/20261102-042649` |
| App | `95bd170` | `v2026.11.02-app.2` | `dist/release/upload-info-1.0.78.json` |

详细记录：`docs/operations/releases/2026-11-02-上下文重置与Agent分层发布.md`。

## 4. 验证

- 三仓 `git diff --check`：PASS。
- 三仓 `dev/test/prod` 推送：PASS。
- 后端不可变制品、thin JAR、契约和 SHA256：PASS。
- Web `npm run build`：PASS。
- App `npm run verify:mp-weixin`：PASS。
- 后端生产服务：active，`NRestarts=0`，HTTP 200。
- Web 生产：HTTP 200，current 指向新 release。
- App 微信开发版上传：PASS。

## 5. 回滚

- 后端：`/opt/decoboon/releases/20261102-034600`。
- Web：`/opt/decoboon-web/releases/20261102-035500`。
- App：正式版未发布；开发版重新上传上一构建即可，正式回退由用户处理。
- 实际是否回滚：否。

## 6. 结论

后端与 Web 已上线，App 开发版已上传。正式小程序提审与发布未执行，等待用户本人操作。

