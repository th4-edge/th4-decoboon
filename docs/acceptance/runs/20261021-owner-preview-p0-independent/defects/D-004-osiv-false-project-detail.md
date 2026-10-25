# D-004 · 关闭 OSIV 时项目详情接口懒加载失败

> 后续状态：`FIX_VERIFIED_PENDING_REACCEPTANCE`，证据见 `../fix-verification/README.md`。

- 级别：P1（验收阻断）
- 环境：隔离 API + MySQL，显式 `spring.jpa.open-in-view=false`。
- 复现：Boss 登录后调用项目详情依赖接口：
  - `project/project/info`
  - `project/stage/page`
  - `project/member/list`
- 实际：
  - 前两者返回截断 JSON（228 B / 113 B，无法解析）。
  - `project/member/list` 返回 `success=false`、`hibernateErrorCode`。
  - 后端日志：`LazyInitializationException: Could not load ... no session`，涉及 `Store` / `Quote` 代理。
  - App 页面显示“工地加载失败 请求失败(200)”，业主视角入口无法渲染。
- 影响：违反“关闭 OSIV 也必须正确”的强制规范，并阻断预览入口的独立验收。
- 证据：
  - `defects/osiv-false-project-info-response.json`
  - `defects/osiv-false-stage-page-response.json`
  - `defects/osiv-false-member-list-response.json`
  - `api/backend-runtime.log`
  - `app/screenshots/entry-boss.png`（OSIV=false 首轮）
- 备注：生产配置 OSIV=true 后页面继续走通；不能以生产兜底掩盖该缺陷。

## 后续复验补充

除上述三接口外，App 复验还发现并修复了同根因阻断：

- `project/task/page`
- `project/contract/info`

最终 OSIV=false 测试环境 App 已正常渲染项目详情并显示“业主视角”入口。可选 `finance/project/report`、`project/report/page` 仍存在同根因错误，另记为 D-005。
