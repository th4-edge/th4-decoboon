# B3B1 薪酬复核与员工发放权限闭环验收执行包

- 状态：`BLACK_BOX_PASS_PENDING_USER_ACCEPTANCE`
- 批次：`B3B1`
- 变更级别：L3（权限、薪酬动作）
- 业务裁决：`DEC-093` / `T12-BIZ-20261023-002`
- 生产发布：未执行
- DDL：无

## 范围

1. BOSS/财务提交 `Draft → Pending`；
2. BOSS 将 `Pending → Draft`，原因必填并显示/留痕；
3. BOSS 可直接确认 Draft 或 Pending；
4. 店长不显示员工工资/提成发放入口；
5. 店长员工发放 page/save 接口均 403；
6. Finance/店长确认、删除继续 403；
7. BOSS 确认/草稿删除回归。

## 证据

- 黑盒验收：`02-black-box-acceptance.md`
- API：`api-results.json`
- HTTP：`http/`
- SQL：`sql/`
- 审计日志：`audit-lines.txt`
- Web/App 截图：`screenshots/`
- 截图哈希：`screenshot-sha256.txt`
- 环境：`environment.txt`
- 测试摘要：`test-summary.txt`
- 可复跑脚本：`run_b3b1_acceptance.py`

## 门禁

- API 14/14 PASS；
- Web 与 App H5 三角色运行态 PASS；
- 隔离 JDK22 全量 `938/0/0/0`；
- 等待用户验收，不冒充独立评审；
- B3B2 与 B4 继续阻塞。
