# 员工薪酬与薪资经营 · B1 发布记录

## 发布结论

- 阶段：`RELEASED`
- Backend release：`/opt/decoboon/releases/20261021-133811`
- Web release：`/opt/decoboon-web/releases/20261021-133900`
- App：`1.0.64` 体验版已上传，正式版由用户本人提审发布
- 无 DDL、无迁移
- 独立验收：`PASS`
- 生产发布详细记录：`docs/operations/releases/2026-10-21-财务薪酬方案B1发布.md`

## 回滚点

- 后端：`/opt/decoboon/releases/20261021-012126`
- Web：`/opt/decoboon-web/releases/20261019-140335`
- App：正式版未发布；体验版由用户按需选择上一构建

## 生产边界

- 仅执行只读健康、资源和路由冒烟。
- 未使用真实员工薪酬执行生产写入。
- App 正式版提审、发布不由 Codex 执行。
