# BUG 缺陷台账

| ID | 严重度 | 状态 | 标题 | 修复 | 复验 |
| --- | --- | --- | --- | --- | --- |
| XA-20261007-003 | P1 | FIXED | PayrollItem 引用 transient PayrollSheet | 先保存工资单头 | 工资单生成成功 |
| XA-20261007-004 | P1 | FIXED | 发放幂等校验顺序错误 | 幂等检查提前 | 同键重试返回同一 Payment |
| XA-20261007-005 | P1 | FIXED | App 枚举状态读取 value 为空 | 优先读取 key | BOSS/店长页面显示数据 |
| XA-20261007-001 | P2 | FIXED | 接口异常伪装空态 | PageState 错误态 | 财务只读回归 |
| XA-20261007-002 | P1 | FIXED_ENV | property_id 非空 | 本地执行 migration 81 | 应付写入成功 |
| XA-20261007-006 | P2 | FIXED_VERIFIED | 提成分页不返回 remark | Page 白名单增加 remark | 重复执行提成脚本复用已完成测试记录 |
| XA-20261007-007 | P1 | FIXED_VERIFIED | 提成释放分页返回完整关联聚合 | 收紧 CommissionRelease/Receipt/Receivable/Employee/CommissionItem 白名单 | 单次响应由约 17.8MB 降至约 2KB，提成 E2E PASS |
