# PD-X1 Web 页面渲染证据

日期：2026-10-23  
性质：**UI 渲染走查，不是真实后端/独立验收证据**。

## 环境

- Vite dev：`http://127.0.0.1:8081`
- Playwright Chromium，1440×900 / 1024×768
- 身份与数据：前端路由拦截使用 Pm 赵强 + 单项目/阶段/任务/日报固定夹具，用于检查页面渲染与交互状态
- Console：0 errors；1 条既有 warning

## 截图

| 文件 | 状态 |
| --- | --- |
| `project-execution-1440x900.png` | 项目履约工作面：阶段、任务、排期/节点动作、最近日报 |
| `project-execution-1024x768.png` | 1024 宽屏布局 |
| `node-evidence-dialog-1440x900.png` | 隐蔽工程节点留证对话框，证据必填、确认按钮初始禁用 |
| `report-dialog-1440x900.png` | 现场日报对话框，阶段/任务/业主可见/文件上传 |

## 已核对

- Web 菜单与路由可达 `/workbench/project-execution`。
- Pm 可见“调整排期 / 节点留证 / 发布日报”。
- Start 节点已留证、Conceal 可留证、Finish 因前置未满足而阻断。
- 节点和日报对话框均可渲染，文件上传为空时主按钮保持禁用。
- 1024 宽屏布局已完成截图走查；未做 DOM 级横向溢出断言。

## 未覆盖

- 真实后端 Provider 数据写入与回读。
- 真实 Pm/Designer/StoreManager/BOSS 会话。
- Designer 只读、StoreManager 可发布等服务端负向。
- 真机、多浏览器、完整可访问性与独立验收。
