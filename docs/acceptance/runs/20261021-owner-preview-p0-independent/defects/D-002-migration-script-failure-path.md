# D-002 · 迁移失败路径自身异常退出

> 后续状态：`FIX_VERIFIED_PENDING_REACCEPTANCE`，证据见 `../fix-verification/README.md`。

- 级别：P1
- 复现：121 迁移失败后，由 `run-migrations.sh.template` 进入失败分支。
- 实际：出现 `line 346: description�: unbound variable`，原始 SQL 失败先被脚本未绑定变量错误掩盖。
- 影响：失败原因和退出码不稳定，降低迁移事故可诊断性；本次已通过 SQL 日志和账本确认 121 为 FAILED。
- 证据：
  - `migration/02-apply-121-123.log`
  - `docs/design/migrations/run-migrations.sh.template:346`
- 交回：研发修复失败分支变量处理并加入失败路径测试。
