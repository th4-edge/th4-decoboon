# 2026-10-24 设计师五阶段 App/Web 统一 · 独立验收

> 验收对象：`design-quote / DQ-X2`  
> 结论：**ACCEPTED**  
> 性质：独立只读黑盒验收；未修改产品实现。  
> 环境：本地 dev 后端 `127.0.0.1:8090`、Web `127.0.0.1:8080`、微信开发者工具自动化 `ws://127.0.0.1:9430`。

## 验收范围

- 五阶段名称与顺序：`待我跟进 → 待我量房 → 待我设计 → 待我谈单 → 待我签约`；
- App/Web 标题与筛选文案一致；
- 搜索关键词由服务端执行，不能在当前已加载数组内过滤；
- 筛选值与五个既有 Provider 契约一致；
- App 分页为下拉刷新 + 触底加载，不展示页码；Web 保留分页组件；
- 五阶段页面无旧名称、无接口失败伪成功；
- 全程只读，无报价、合同、项目、客户写操作。

## 独立黑盒结果

| 层级 | 检查 | 结果 | 证据 |
| --- | --- | --- | --- |
| API | 五 Provider × 全部筛选、分页 total/current/size、每页 ≤20、P95≤500ms | **103/103 PASS** | `api-results.json` |
| API | 谈单关键词 `周先生` 服务端返回空；签约关键词 `王亮` 仅返回王亮 | PASS | `api-results.json` |
| App | 五入口顺序、五页标题、筛选、旧名称/页码排除、关键词与筛选语义 | **24/24 PASS** | `app-results.json` |
| App | 7 张原生页面截图 | PASS | `screenshots/{followup,measure,design,quote,sign,quote-search,sign-search}.png` |
| Web | 同一设计师账号 1440×900 五页标题、筛选与控制台 | **6/6 PASS** | `web-results.json`、`screenshots/web-*.png` |

## 构建与门禁

- App `npm run type-check`：PASS。
- App `npm run check:pagination`：PASS。
- App `npm run check:navigation`：PASS。
- App `npm run build:mp-weixin:release`：PASS；主包 1,034,871 B，最大分包 726,052 B，总包 4,032,308 B。
- App 分包：主包 14 页，17 个分包 / 175 页，PASS。
- Web `npm run build`：PASS。
- Web `check:permissions`、`check:workbenches`、`check:workspaces`：PASS。
- 后端全量（DQ-X1 干净回归）：**1098/1098 PASS**。
- DQ-X2 未修改 Provider、权限、状态机、金额或数据库。

## 缺陷

- P0：0。
- P1：0。
- 首次重跑 App 自动审计时出现一次页面切换竞态（固定等待导致 `sign-workbench` 尚未成为当前页）；改用显式等待目标路径后 24/24 稳定通过，属于验收脚本稳定性问题，不是产品缺陷。

## 验收结论

`DQ-X2` 满足名称、顺序、筛选、关键词、分页边界和 App/Web 同源要求，独立验收 **PASS**，可标记 `ACCEPTED`。

本次不包含小程序上传、正式提审或生产发布；正式版仍由用户本人操作。
