# 待我设计 + 设计工作台 UI 升级验收包

> 日期：2026-10-29  
> 状态：`DEV_VERIFIED_PENDING_USER_ACCEPTANCE`  
> 风险级别：L1（仅 Web 页面结构、视觉与展示层调整）

## 1. 范围

- `/designer/design`：待我设计任务队列与当前房屋工作面。
- `/designer/design/:designId`：设计方案工作台。
- 目标：减少重复信息、收拢唯一主行动、让设计分项成为首屏主工作面、改善响应式顺序和可访问性。

## 2. 明确未改

- 未修改后端 Provider、接口字段或请求参数。
- 未修改前端 API 调用、权限判断、状态推进、保存/自动保存、金额公式、表单语义或路由行为。
- 未新增、删除或重命名业务动作。
- 未使用生产数据；验收截图来自本地 Vite + 本地 mock Provider。

## 3. 页面变化

### 3.1 待我设计

- 统一 `PageHeader`，移除英文眉题和长说明。
- 以客户、手机号、当前房屋、面积/户型、当前方案组成当前设计对象条。
- 客户状态与流失处置进入命名明确的折叠区，不再抢占设计主任务。
- 设计输入只保留量房结论与操作，删除与对象条重复的房屋摘要。
- 右栏收拢为“当前判断 → 下一步 → 唯一主行动 → 次要动作 → 下一套”。
- 单列时顺序调整为：待设计队列 → 当前判断与主行动 → 对象工作面。

### 3.2 设计工作台

- 顶部只保留方案上下文、状态和当前状态对应的主行动。
- 四张同权关注卡和独立下一步卡收拢为一条当前判断带。
- 设计分项表前移到首屏主工作面。
- 原始结构、量房信息、图纸与参考图收拢到默认折叠的“设计依据与资料”。
- 设计项金额只展示 Provider 返回的 `calculatedAmount`；`priceSource=unavailable` 时显示“暂不可确认”。
- 自动保存后使用 Provider 返回的新金额和 `priceSource` 更新展示，不在前端重新求和。

## 4. 页面高度对比

| 页面 | 宽度 | 调整前 | 调整后 | 变化 |
| --- | ---: | ---: | ---: | ---: |
| 待我设计 | 1440 | 2281px | 1619px | -29.0% |
| 待我设计 | 1280 | 2827px | 1979px | -30.0% |
| 待我设计 | 1024 | 3504px | 2599px | -25.8% |
| 设计工作台 | 1440 | 3225px | 2403px | -25.5% |
| 设计工作台 | 1280 | 3282px | 2403px | -26.8% |
| 设计工作台 | 1024 | 2418px | 1238px | -48.8% |

补充：390px 下待我设计 2975px、设计工作台 1518px；两页均无整页横向溢出。

## 5. 验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| `npm run type-check` | PASS | 构建日志 |
| `npm run check:ai-flavor` | PASS | 门禁输出 |
| `npm run check:identifier-exposure` | PASS | 门禁输出 |
| `npm run check:client-copy` | PASS | 门禁输出 |
| `npm run check:brand-theme` | PASS | 门禁输出 |
| `npm run type-check` + `npx vite build` | PASS（最终代码） | `/tmp/zdq-pending-design-vite-build.log` |
| `npm run build` | 本页改动后首次 PASS；最终复跑被并行修改的 `StoreCustomerView.vue` 全局分页/文案门禁拦截 | `/tmp/zdq-pending-design-final-build.log` |
| 1440/1280/1024 真实渲染 | PASS | `screenshots/`、`evidence/round2-evidence.json` |
| 390 补充渲染 | PASS（无整页溢出） | `screenshots/` |
| 页面主行动数量 | 待我设计 1 个；工作台页面级 1 个 | `evidence/round2-evidence.json` |
| Console | 0 error（拦截 mock 未实现的未读通知计数后） | `evidence/round2-evidence.json` |
| 主行动进入工作台 | PASS | `evidence/interaction-evidence.json` |
| 键盘焦点与减少动效 | PASS | `evidence/accessibility-motion-evidence.json` |
| 交互目标尺寸 | 可见按钮均 ≥ 24px 命中区 | `evidence/round2-evidence.json` |

## 6. 视觉预检

- 本地视觉桥：`qwen2.5vl:7b`，仅作为预检，不替代用户验收。
- 两轮截图与评审已保存：`round1b` 基线修正版、`round2` 复核版。
- 机器评审未发现卡片墙、英文眉题、装饰性 2.5D/3D 或模板化 KPI；仍需用户对视觉气质、密度和设计行业匹配度做最终判断。

## 7. 未解决项

- 本轮使用 mock Provider，未接生产租户数据，不宣称真实业务状态与长数据已完成独立验收。
- 390px 下设计分项表保留局部横向滚动，以保证列语义不压缩；整页横向溢出为 0。
- 当前工作区同时存在其他任务的 `StoreCustomerView.vue` 改动，导致全量 `npm run build` 的全局分页和客户端文案门禁失败；该文件不属于本批范围，未修改、未回退。本批最终 `type-check` 与 Vite production build 均通过。
- 用户尚未完成最终视觉验收，因此状态保持 `DEV_VERIFIED_PENDING_USER_ACCEPTANCE`，不标记 `PASS` 或生产验收完成。

## 8. 文件

- `screenshots/`：1440/1280/1024/390 首屏与全页截图。
- `reviews/`：第二轮视觉模型预检原文。
- `evidence/round2-evidence.json`：DOM 几何、主行动、金额字段、焦点目标和 Console 结果。
- `evidence/accessibility-motion-evidence.json`：键盘焦点与 `prefers-reduced-motion`。
- `evidence/interaction-evidence.json`：列表主行动进入工作台的真实路由证据。
