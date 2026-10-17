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

---

# B3 App 业务体检与页面上下文审查

## 审查对象

- Batch：B3
- App 路由：
  - `/subpackages/project/pages/detail?id=...`
  - `/subpackages/project/pages/material-sheet-detail?sheetId=...`
  - `/subpackages/project/pages/material-sheet-create?projectId=...`
  - `/subpackages/account/pages/help?topic=material-sheet-time`
- 角色：企业负责人（Boss，Company 数据范围）；作业单页面同时覆盖 Designer/Pm 可见的测量上下文
- 环境：微信开发者工具 375×599；H5 390×844；本地 dev 后端真实 Provider
- 业务节点：`material.health`、`material.measure`、`material.order-sheet`

## B3 证据

| 场景 | 截图/路径 | 结果 | 问题 |
| --- | --- | --- | --- |
| 项目材料体检 | `evidence/B3/b3-app-project-material-health.png` | PASS：真实项目显示选材、待下单、待到货、成本不完整、校核项和聚焦行 | 无阻断 |
| 项目材料体检空态 | `evidence/B3/b3-app-project-material-health-empty.png` | PASS：无材料计划时明确“无法确认材料履约是否已开始” | 不伪装完成 |
| 项目材料体检失败态 | `evidence/B3/b3-app-project-material-health-error-h5.png` | PASS：500 时显示“材料业务暂不可确认”与重试 | 无阻断 |
| 作业单时间/取消上下文 | `evidence/B3/b3-app-material-sheet-context.png` | PASS：当前状态、下一步、时间口径、模板必填、未定价与取消边界可见 | 无阻断 |
| 建单页来源说明 | `evidence/B3/b3-app-material-sheet-create-guide.png` | PASS：材料行只来自计划、合同外走增减项、作业单不承载价格 | 无阻断 |
| 帮助主题 | `evidence/B3/b3-app-help-time-semantics.png` | PASS：测量、配送/安装、要求到场/实际到场的区别可搜索 | 无阻断 |

OCR 文本：`evidence/B3/ocr-text.txt`。

## B3 交互与布局

- 微信开发者工具 automator 成功连接并加载 4 个真实页面；
- 项目材料体检“材料计划”动作实际导航到 `/subpackages/project/pages/material-plan?projectId=...`；
- 作业单详情识别到真实 Tile 模板 `measureAt.required=true`，显示“提交请购前必须先填写”；
- 未定价草稿显示“当前采购侧存在未定价或部分结算，成本不完整”，没有展示 0 成本利润；
- H5 390×844 三个业务页面 `scrollWidth = innerWidth = 390`，无横向溢出；
- 正常页面 Console Errors = 0；
- 失败态来自本地网络拦截，不是业务页面自身异常。

## B3 缺陷

| ID | 页面 | 现象 | 期望 | 级别 | 状态 |
| --- | --- | --- | --- | --- | --- |
| — | — | 未发现阻断级页面缺陷 | — | — | — |

## B3 跨端一致性

- Web 与 App 的 `blueprint/check/report` 使用同一 Provider、同一阶段/计数/成本口径；
- App 作业单时间说明引用 `OBJ-008 / DEC-MP-07 / DEC-072 / DEC-073`，没有在前端新增状态机；
- B5 仍需独立验证 App/Web 真机、越权、空态和边界一致性。
