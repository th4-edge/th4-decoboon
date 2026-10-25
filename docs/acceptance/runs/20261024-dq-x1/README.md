# DQ-X1 待我谈单查询整改：开发侧 HTTP 与页面验证

- 日期：2026-10-24
- Domain：`design-quote`
- Batch：`DQ-X1`
- 环境：本地 dev，Web `127.0.0.1:8080`，后端 `127.0.0.1:8090`，MySQL 8.0.24
- 性质：开发侧验证，不替代独立验收

## 1. 验证范围

- 数据库分页、状态筛选、关键词筛选、服务端页大小上限；
- Boss 全企业、店长本店、设计师本人数据范围；
- Finance / Marketer / Pm 越权拒绝；TeamLeader 登录门禁；
- 当前页 ID 批量预取，无逐行客户/房屋查询；
- Web 待我谈单真实页面、翻页和角色入口。

## 2. 自动化结果

执行：

```bash
node docs/acceptance/runs/20261024-dq-x1/run-http-acceptance.mjs
```

结果：**17/17 PASS**，详见 `http-results.json`。

关键结果：

| 项目 | 结果 |
| --- | --- |
| Boss 当前任务 | 22，分 2 页 |
| 店长本店任务 | 22 |
| 设计师本人任务 | 14 |
| Draft 筛选 | 13 |
| `size=500` 服务端结果 | 被限制为 100 |
| Finance / Marketer / Pm | 403 |
| TeamLeader | 登录门禁 403 |
| 10 次接口 P95 | 38.6ms |

范围校验通过数据库独立计数与返回 ID 反查：店长返回的当前页报价均属于本店，设计师返回的当前页报价均由本人创建。

## 3. 页面证据

| 场景 | 结果 | 截图 |
| --- | --- | --- |
| 设计师待我谈单 | 14 个任务，第 1/1 页，控制台 0 error | `screenshots/designer-quotes-1440.png` |
| Boss 待我谈单 | 22 个任务，第 1/2 页，控制台 0 error | `screenshots/boss-quotes-1440.png` |
| Boss 翻页 | 第 2/2 页，总数保持 22，控制台 0 error | `screenshots/boss-quotes-page2-1440.png`、`boss-quotes-page2-1280.png` |
| 店长待我谈单 | 22 个任务，第 1/2 页，控制台 0 error | `screenshots/store-manager-quotes-1440.png` |
| Finance 直链 | 跳转 `/forbidden`，控制台 0 error | `screenshots/finance-forbidden-1440.png` |

## 4. 验证中修复的实际缺陷

1. 设计师分支曾把 `storeUnscoped=false + storeId=null` 传入 SQL，导致本人任务为 0；修正为非店长不做门店收窄。
2. `CustomerHeroCard.vue` 引用未定义的 `$danger-600`，阻断 Boss 页面；改为已有 `$danger-500`。
3. `DesignerMeasureTasksView.vue` 把 `number | null` 传入字符串房屋 ID 属性；改为显式 `String()` / `undefined`。

## 5. 结论

- 页面、HTTP、定向测试、干净全量回归与 Web production build 全部通过，`DQ-X1` 进入 `RECONCILED`。
- 干净全量回归：**1098/1098 PASS**，0 failure / 0 error / 0 skipped。
- 尚不能标记 `ACCEPTED`：仍需独立验收人员复核分页事实、权限边界、N+1 查询结构和页面证据。
