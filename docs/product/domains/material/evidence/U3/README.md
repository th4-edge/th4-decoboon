# U3 · 老板材料风险页、助手语言与材料说明入口

- 日期：2026-10-18
- 性质：UX 回归自测与真实 H5/浏览器渲染证据；不是独立验收
- 范围：
  - Web `MaterialHealthPanel.vue`、`MaterialAssistant.vue`、`BusinessBlueprintCenterView.vue`、`workbenchConfig.ts`、`routes.ts`
  - App `MaterialHealthCard.vue`、`MaterialAssistant.vue`、`subpackages/project/pages/detail.vue`
- 环境：本地 dev，后端 `127.0.0.1:8090`，Web `127.0.0.1:8080`，App H5 `127.0.0.1:8081`
- 账号：Finance `19900000002`、Designer `19900000005`（本地测试密码）
- 项目：`231848057782665216`（荷塘家园业主）

## 1. 页面契约

| 页面 | 首屏第一问 | 首屏第二问 | 首屏第三问 |
| --- | --- | --- | --- |
| 老板/财务项目材料体检 | 最急的是哪条材料 | 卡在谁、下一步是什么 | 点哪里处理 |
| 问材料助手 | 当前情况是什么 | 依据是什么 | 下一步是什么 |
| 材料业务说明 | 这是什么 | 规则来源是什么 | 日常操作去哪里 |

## 2. 改动

### 老板/财务项目材料体检

- Web/App 首屏新增“最急材料”卡：
  - 材料 + 空间；
  - 当前卡点（如“待选材”）；
  - 责任设计师/项目经理；
  - 下一步；
  - 一个处理按钮（去选材 / 去下单 / 查看作业单 / 去结算）。
- App 原“聚焦材料行”改为“最急材料”，增加责任人与处理按钮。
- 校核严重级别从 `P0/P1` 改为“高/中/低”。

### 问材料助手

- 移除首屏技术元数据：`置信度`、`只读工具`、`已审计`、citation ID；
- “证据”改为“依据”；
- 技术词转业务语言：
  - `priceState=Unpriced` → “采购价未填”
  - `costComplete=false` → “成本未完整”
  - `Selection 为空或不是 Selected` → “尚未完成选材”
  - `null` → “暂不显示”
- Designer/PM 的 cost/price/profit 依据按角色过滤；
- 回答元数据标签改为“规则 / 当前情况 / 无权限 / 不支持 / 暂不可确认”。

### 材料说明入口

- Web 蓝图从“经营分析/本店经营/项目核算”移入“帮助与说明”；
- 菜单名从“材料业务蓝图”改为“材料业务说明”；
- 路由面包屑从“业务蓝图中心”改为“材料业务说明”；
- 页面说明改为“用于了解材料流程、字段和规则；日常操作请去项目材料、主材下单或结算”；
- 顶部技术事实“commit”移除，改为“当前视角”；“OBJ/BR/SCN/DEC”改为“已登记业务规则”；
- 页面不再展示 BR/DEC/OBJ/SCN/MET/CAP/GAP/EX 等内部编号和 source refs；
- P1/P2/P3 改为“报价成本价/采购价/结算价”，对象名与 condition 中的英文标识转中文；
- 字段字典默认隐藏，点“查看字段明细（实施/培训用）”才展开。

## 3. 真实渲染结果

| 场景 | 结果 | 证据 |
| --- | --- | --- |
| Web 项目材料体检 1440×900 | PASS：最急材料 + 责任设计/项目 + 下一步 + 去选材 | `screenshots/u3-web-project-risk-1440x900.png` |
| Web 项目材料体检 1024×768 | PASS：无横向溢出 | `screenshots/u3-web-project-risk-1024x768.png` |
| Web 问材料 1440×900 | PASS：无置信度/工具/审计；依据已转业务语言 | `screenshots/u3-web-assistant-1440x900.png` |
| Web 材料业务说明 1440×900 | PASS：帮助与说明入口；无 commit；无 BR/DEC 等编号；字段字典默认隐藏 | `screenshots/u3-web-material-note-1440x900.png` |
| App 项目材料体检 390×844 | PASS：最急材料 + 责任设计师 + 去选材 | `screenshots/u3-app-project-risk-390x844.png` |
| App 问材料 390×844 | PASS：无置信度/工具/审计；无 null/priceState/costComplete | `screenshots/u3-app-assistant-390x844.png` |
| App 问材料 375×667 | PASS：无横向溢出 | `screenshots/u3-app-assistant-375x667.png` |
| App Designer 问材料 | PASS：成本/定价/利润依据被过滤，仅保留履约依据 | `screenshots/u3-app-designer-assistant-390x844.png` |

## 4. 指标

- Web 1440×900/1024×768：`scrollWidth = innerWidth`，无横向溢出；
- App 390×844/375×667：`scrollWidth = innerWidth`，无横向溢出；
- 业务 Console Errors：0；
- 助手文本检查：`置信度`、`只读工具`、`已审计`、`null`、`priceState`、`costComplete` 均不出现。

## 5. 命令

App：

```bash
npm run type-check
npm run check:ui
npm run check:subpackages
npm run build:mp-weixin
```

Web：

```bash
npm run type-check
npm run build
```

全部 PASS。

## 6. 未闭环

- U4：真实设计师/财务/PM/老板无培训可用性走查未执行；
- 助手仍是单轮、确定性规则问答，未做多轮对话与自动执行；
- 助手建议问题仍来自固定题库，尚未按单据状态动态生成；
- 本轮为真实 H5/浏览器自测，不等同于独立验收或真机验收。
