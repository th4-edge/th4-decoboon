# B2 月度考勤跨端对齐 · 黑盒验收记录

## 环境

- 后端：Java 22，`127.0.0.1:8090`
- Web：Vite `127.0.0.1:8085`
- 数据库：临时 MySQL `33332 / decoboon_h1_schema`
- 角色：BOSS、总部财务、门店财务、店长
- 生产：未访问

## 结论

`BLACK_BOX_PASS_PENDING_USER_ACCEPTANCE`

- API：`10 PASS / 0 FAIL`
- Web 页面：PASS
- console error：0

## API 结果

| 用例 | 结果 |
| --- | --- |
| 员工+2026-11 方案读取 | `scheduledDays=27`，PASS |
| 店长本店新增考勤 | 成功，随后端写入 `scheduledDays=27` |
| 店长本店编辑考勤 | 成功，正常出勤 26、事假 1 |
| 店长本店删除未入工资考勤 | 成功 |
| 店长跨店新增 | 404 |
| 店长跨店删除 | 404 |
| 已生成工资项的考勤更新 | 409 |
| 店长跨店更新 | 404 |
| 数据库快照回读 | `scheduled_days=27` |

证据：`api-results.json`、`update-guard-results.json`、`http/`、`sql/`。

## Web 结果

1. BOSS 员工工资页 → 月度考勤，选择 `H1总店员工` 后，应出勤显示 **27 天**，不再显示 22。
2. 考勤表单完整展示正常出勤、事假、病假、旷工、外勤、年假、调休和备注。
3. 点击已有考勤“编辑”后，应出勤回填 27，正常出勤回填 27，进入“保存变更/取消编辑”状态。
4. 无当月薪酬方案的员工显示“该工资月份没有生效薪酬方案”，不回退 22。
5. 从 2026-09 工资单点击“去确认考勤”，考勤抽屉月份为 **2026-09**，历史月份上下文正确。
6. 考勤历史行提供编辑、确认、删除入口；已入工资状态不在前端允许编辑/删除，后端同时兜底。
7. 页面 console error 为 0。

截图：

- `screenshots/web-attendance-scheme-27.png`
- `screenshots/web-attendance-edit-existing.png`
- `screenshots/web-attendance-no-scheme-month-context.png`

## 自动化门禁

- B2 定向测试：`6/0/0/0`
- B2 隔离 JDK 22 全量：`931/0/0/0`
- Web type-check/build/PASS
- 权限、门店范围、工作台、客户端文案门禁 PASS

## 边界

- 本次为同一执行会话完成开发后的黑盒验证，不冒充独立验收。
- 未发布生产、未执行 DDL。
- B3A/B3B 未启动。
