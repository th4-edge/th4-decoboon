# 运行清单

- 模式：full-role
- 端面（surface）：both
- 目标企业（只读参照）：麦鸿装饰（只读形态参照，本轮未访问）
- 测试企业：昭青科技（`199500000000000001`）
- 形态：controlled-loop
- 审计跑道：local-fixture
- 数据模式：no-real-data
- 角色账号与数据范围：
  - Boss `13910002001`：全企业
  - StoreManager `13910002002`：总店
  - Finance `13910002003`：当前财务工作台全部门店视图
  - Designer `13910002004`、Marketer `13910002005`、Pm `13910002006`：总店/团队，本轮仅夹具
  - TeamLeader `13910002007`：预置审计账号，按现行规则禁止业务登录
  - 滨湖店长 `13910002008`：跨店负向范围
- 场景矩阵：9 个场景，详见 `04-scenario-matrix.md`
- 八大业务重点：`COVERED 0 / PARTIAL 6 / NOT_TESTED 2`
- 备份与回滚：
  - `/Users/zhangwei/.decoboon-local/business-rebuild-20261030/decoboon-before-rebuild.sql.gz`
  - SHA-256 `99338b2bad6c6d0cab7e6d6960702d8c5e406b010a0232167ce0e01b717aeb5b`
- 中断条件：出现生产访问、真实外发、未授权高危写、跨企业写入、无法恢复的数据库错误时立即停止
- 责任人：codex（本地夹具运行）；正式提审发布由用户本人负责

- [x] 已读取业务知识库与 registry.json
- [x] 已核对企业、门店、员工、角色与姓名
- [x] 已确认本地环境与微信开发者工具项目
- [x] 已建立截图与证据目录
- [x] 已备份并演练恢复
- [x] 已完成核心业务链与财务不变量检查
- [ ] 已完成材料、验收、真机、弱网与独立验收

> 当前状态：`PARTIAL`
