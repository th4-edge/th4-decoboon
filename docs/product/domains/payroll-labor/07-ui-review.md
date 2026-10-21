# 员工薪酬与薪资经营 · B1 UI 与交互审查

## 审查对象

- 页面/路由：`/finance/compensation-schemes`
- 角色：Finance（可写）、BOSS（全企业可写）、StoreManager（本店只读）
- 设备/分辨率：1440×900、1280×800、1024×768
- 业务蓝图节点：UI-B1-01～06；enter/filter/create/edit/lock/successor/terminate-delete/recover
- 环境：本地 dev 后端 `http://127.0.0.1:8090` + Web `http://127.0.0.1:8080` + 隔离 MySQL 数据副本
- 性质：真实浏览器开发自测与 UI 审查；不替代独立验收

## 证据

| 场景 | 截图/路径 | 结果 | 问题 |
| --- | --- | --- | --- |
| Finance 列表 1440×900 | `output/playwright/payroll-b1-ui-review/01-finance-1440x900.png` | PASS | 无页面级横向溢出；岗位/门店/团队中文与锁定状态正确 |
| Finance 列表 1280×800 | `output/playwright/payroll-b1-ui-review/02-finance-1280x800.png` | PASS | `docScrollWidth=1280` |
| Finance 列表 1024×768 | `output/playwright/payroll-b1-ui-review/03-finance-1024x768.png` | PASS | `docScrollWidth=1024`，表格在内容区滚动，无页面溢出 |
| 新增抽屉 | `output/playwright/payroll-b1-ui-review/04-finance-drawer-1024x768.png` | PASS | 员工、工资、应出勤、生效/失效日期、备注和影响说明完整 |
| 客户端校验 | `output/playwright/payroll-b1-ui-review/05-finance-drawer-validation-1024x768.png` | PASS | 未选员工/未填工资时保留抽屉并提示可执行原因 |
| 新增后续方案 | `output/playwright/payroll-b1-ui-review/06-finance-successor-1024x768.png` | PASS | 员工只读，预填工资/应出勤，明确历史事实不回写 |
| 空态 | `output/playwright/payroll-b1-ui-review/07-finance-empty-1024x768.png` | PASS | 显示“暂无薪酬方案”和下一步说明 |
| StoreManager 只读 | `output/playwright/payroll-b1-ui-review/08-storemanager-readonly-1440x900.png` | PASS | 只显示刷新与“本店只读”，无新增/编辑/终止/删除 |
| BOSS 可写 | `output/playwright/payroll-b1-ui-review/09-boss-1280x800.png` | PASS | 企业负责人全公司范围；写入口可见 |

### 真实交互证据

- Finance 成功创建徐勇薪酬方案 `¥8,888 / 22 天`；
- 成功编辑为 `¥9,999`；
- 成功终止为 `2026-10-21 至 2026-10-21`；
- 成功删除临时方案，列表回到 5 条；
- 筛选“王宏文”返回 1 条；
- 筛选不存在员工返回 0 条和明确空态；
- 员工选择器可加载在职员工并显示角色、门店、团队和在职状态；
- Finance/StoreManager/BOSS 页面 console error 均为 0；
- 1440/1280/1024 三档均无页面级横向溢出。

## 缺陷

| ID | 页面 | 现象 | 期望 | 级别 | 状态 |
| --- | --- | --- | --- | --- | --- |
| UI-B1-001 | 薪酬方案列表 | 岗位、门店、团队显示“待确认”，角色显示英文枚举 | 正确映射 Provider 的 employee/store/team 对象并显示中文岗位 | P1 | FIXED |
| UI-B1-002 | 编辑薪酬方案抽屉 | 未锁定方案错误显示“员工已锁定”提示 | 只有服务端 `locked=true` 才显示锁定提示 | P1 | FIXED |

## 布局与可访问性

- 三档视口 `document.documentElement.scrollWidth === window.innerWidth`。
- 表格内容在 1024 下保持业务可读，页面壳层不横向溢出。
- 抽屉字段均有 label；禁用员工字段可见；主次按钮清楚。
- 日期、状态和动作均有中文展示，未直接落屏英文枚举。
- 初始登录/直达路由存在 1 条 Vue Router 路由集合装载前的瞬态 warning，不是页面运行错误，也未阻断导航；记录为既有壳层行为。

## 跨端一致性

- Web 与 App 均使用 `payroll/compensation/*`，字段、锁定规则和错误文案同源。
- App 已补锁定状态与“新增后续方案”入口。
- 本轮未做微信开发者工具/真机 UI 审查，不能以 Web 证据替代 App 运行时验收。

## 未覆盖

- 当前隔离数据只有一个门店，未在 UI 层构造第二门店的跨店负向页面；
  跨店权限当前由后端自动化测试覆盖，独立验收阶段应补真实双门店账号/对象。
- 未执行生产只读冒烟和独立验收。
- 未上传或发布小程序。

## 数据与安全清理

- 测试使用隔离 MySQL 数据目录，不修改生产或共享数据库。
- 三个本地测试账号临时口令已重置，原始密码哈希从备份恢复。
- 临时薪酬方案均已删除；徐勇方案数恢复为 0，吴濛濛恢复为原始 1 条。
- 未输出密码哈希、完整 token 或 Cookie。

## 结论

- UI 与交互门禁：`PASS`
- 当前阶段：`UI_REVIEWED`
- 下一阶段：`RECONCILED`，逐条核对 REQ-B1-01～11，再进入独立验收。
