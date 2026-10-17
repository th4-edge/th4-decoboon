# 材料履约与材料业务助手 · UI 与交互审查

## 审查对象

- Batch：B2
- 路由：
  - `/business-blueprints`
  - `/finance/projects/:projectId`
- 角色：企业负责人（Boss，Company 数据范围）
- 环境：本地 dev，后端 8090，Web 8080
- 设备/分辨率：1440×900、1024×768
- 业务蓝图节点：`material.end-to-end`、`material.health`

## 证据

| 场景 | 截图/路径 | 结果 | 问题 |
| --- | --- | --- | --- |
| 蓝图正常态 | `evidence/B2/b2-blueprint-1440x900.png` | PASS：版本/来源/阶段/对象/字段/规则/例外/未决项可见 | 无阻断 |
| 蓝图交互态 | `evidence/B2/b2-blueprint-1024x768.png` | PASS：阶段可切换，字段搜索生效，1024 下无横向溢出 | 无阻断 |
| 项目材料体检正常态 | `evidence/B2/b2-material-health-1440x900.png` | PASS：真实项目“到场确认”、未定价与成本不完整同屏；利润显示“成本未完整” | 无阻断 |
| 项目材料体检 1024 | `evidence/B2/b2-material-health-1024x768.png` | PASS：指标 3 列、主内容单列、无横向溢出 | 无阻断 |
| 空态 | `evidence/B2/b2-material-health-empty-1440x900.png` | PASS：无材料计划时明确说明“无法确认材料履约是否已开始” | 不伪装完成 |
| 失败态 | `evidence/B2/b2-material-health-error-1440x900.png` | PASS：显示“材料业务体检暂不可确认”并提供重试 | 解除网络拦截后重试恢复 |

## 交互检查

- 阶段按钮切换后标题、触发、系统下一步、账务口径、例外和来源同步更新；
- “字段字典”搜索“金额”后表格收缩为 4 行，未出现空表伪装；
- “去项目账本做材料体检”能进入 `/finance/projects`；
- “刷新体检”与失败态“重试”都能重新请求服务端；
- 1024×768 下侧栏收起，蓝图阶段网格为 3 列，体检指标为 3 列、主区单列；
- 正常页面 Console Errors = 0。

## 缺陷

| ID | 页面 | 现象 | 期望 | 级别 | 状态 |
| --- | --- | --- | --- | --- | --- |
| — | — | 本批未发现阻断级页面缺陷 | — | — | — |

## 跨端一致性

- App 与 Web 字段/状态/下一步：App 侧 B3 尚未做 UI 审查；
- 当前只对 Web 与后端 Provider 同源口径做了对账；
- B5 仍需独立验证 App/Web 跨端一致性。
