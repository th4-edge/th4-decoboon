# 员工业主视角预览 · 真实四 Tab 复验

- 日期：2026-10-21
- 环境：`decoboon-test` + 微信开发者工具 `2.02.2609231 RC`
- 测试企业：`ZDQ造数M`
- 测试项目：`235520576473530368`（王芳（造数1100） · 望江府1栋102）
- 测试账号：BOSS `13910000100`
- 后端：`http://127.0.0.1:18092/api/`

## 结论

`owner-preview` 已从错误的“直达 journey 二级页”修正为进入业主真实首页，并复用业主端四个主页面：

| Tab | 复用页面 | 结果 |
| --- | --- | --- |
| 我的家 | `pages/index/index` | PASS |
| 进度 | `pages/project/list` | PASS |
| 动态 | `pages/dynamic/index` | PASS |
| 我的 | `pages/mine/index` | PASS |

- 自动化契约与四页面渲染：15/15 PASS
- 实际点击四 Tab：PASS
- 预览横幅、员工工号与时间水印：PASS
- 首页内部成本/供应商/利润文案：未出现
- 动态页写汇报入口：预览模式不出现
- “我的”项目计数：限制为当前预览项目
- 预览上下文跨 Tab 保留：PASS

## 证据

- `results.json`：结构化结果
- `01-owner-home.png` 至 `05-owner-home-return.png`：四 Tab 与返回首页截图
- 契约脚本：`scripts/verify-owner-preview-contract.mjs`
- 自动化脚本：`scripts/verify-owner-preview-four-tabs.mjs`

## 手动验收状态

微信开发者工具已停留在业主预览“我的家”首页，底部可直接点击：

`我的家 / 进度 / 动态 / 我的`

本记录只证明开发自测与自动化走查通过，不替代用户本人最终验收。
