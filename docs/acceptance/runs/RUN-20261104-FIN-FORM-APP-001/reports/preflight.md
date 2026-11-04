# 预检

- RUN：`RUN-20261104-FIN-FORM-APP-001`
- Plan：`TP-20261104-FIN-ACCOUNTING-GATE`
- Suite：`SUITE-FIN-ACCOUNTING-GATE-V0.1`
- Oracle：`ZDQ-FIN-20261104-V0.1` / `ab6d4d8`
- 用例数：1
- P0/P1：1/0
- App：生产构建 1.0.86，`e1cd29b`
- 环境：`prod-readonly`
- 账号身份：安徽麦鸿装饰设计工程有限公司 · 吴濛濛 · 财务
- 登录方式：同源密码登录 Provider 获取真实会话后注入自动化运行环境
- 执行边界：不保存考勤；微信快捷登录 UI 未命中该账号，不判定为通过
