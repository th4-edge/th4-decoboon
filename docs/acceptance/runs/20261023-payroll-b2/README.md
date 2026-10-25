# B2 财务月度考勤跨端对齐验收执行包

- 状态：`BLACK_BOX_PASS_PENDING_USER_ACCEPTANCE`
- 批次：`B2`
- 变更级别：L3
- 生产发布：未执行
- DDL：无

## 范围

1. Web 按员工+工资月份读取薪酬方案应出勤；
2. 已有考勤编辑、删除和完整异常字段；
3. 工资单跳考勤的月份上下文；
4. 店长本店考勤新增/编辑/删除权限；
5. 已生成工资项的考勤禁止修改或删除。

## 证据

- 黑盒验收：`02-black-box-acceptance.md`
- API：`api-results.json`、`update-guard-results.json`
- HTTP：`http/`
- SQL：`sql/`
- 截图：`screenshots/`
- 截图哈希：`screenshot-sha256.txt`
- 环境：`environment.txt`

## 门禁

- 黑盒验证 PASS；
- 等待用户验收；
- 用户验收前不启动 B3A；
- 独立评审仍为可选项，不得把本次自测写成独立验收。
