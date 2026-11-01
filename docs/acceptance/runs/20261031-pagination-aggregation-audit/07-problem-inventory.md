# 分页与前端汇总问题盘点

> 日期：2026-10-31
> 范围：th4-decoboon 后端、th4-decoboon-app、th4-decoboon-web
> 结论：存量技术债较多，现有前端分页门禁只防止新增固定大页，尚未自动阻止前端业务汇总。

## 1. 扫描结果

| 检查 | 结果 |
| --- | ---: |
| Web 固定大页调用 `size >= 50` | 51 处 / 46 个文件值组合 |
| App 固定大页调用 `size >= 50` | 38 处 / 33 个文件值组合 |
| Web `reduce` / `filter().length` 可疑表达式 | 92 处 |
| App `reduce` / `filter().length` 可疑表达式 | 117 处 |
| 后端 `/page` Provider | 108 |
| 后端 `/report` Provider | 44 |
| 后端硬编码 `PageRequest.of(0, >=100)` | 5 |
| 后端 `.subList()` | 39 |
| 返回 `List<T>` 的 `/list` Provider | 15 |

`reduce/filter().length` 扫描包含布局行数、表单输入预览等允许场景，必须逐项分类，不能直接等同业务缺陷。

## 2. 典型高风险页面

### App 首页材料汇总

`pages/index/index.vue` 先请求材料计划工作台列表，再对列表执行：

```text
materialBenches.reduce(readyToOrder)
materialBenches.reduce(pendingArrival)
materialBenches.reduce(delayed)
```

风险：项目量增长后，首页材料指标依赖列表是否完整返回。

### App 报价/选材/收款列表

- 报价列表从列表数据计算有效报价金额；
- 选材工作台从项目列表计算未选、已下单数量；
- 应收/应付选择器直接对当前候选列表计算总额和逾期额。

风险：截断后金额偏小，且口径无法与后端报表对账。

### Web 增减项与合同

- 增减项列表对 `customerConfirmed` 项做金额汇总；
- 项目合同页对合同行和变更金额做前端合计；
- 主材选材看板对项目列表做未选/已下单汇总。

风险：合同金额、变更金额和选材进度容易出现跨端不一致。

### 后端项目报告

`project/reports/page` 和无类型分支使用：

```text
PageRequest.of(0, 500)
→ Java filter
→ List.subList
→ new PageImpl
```

风险：500 条以后静默截断；每次分页请求加载固定大页；筛选和分页未真正下推数据库。

## 3. 现状判断

- Web/App 都已有 `check:pagination`，但当前是存量基线棘轮，不会修复历史调用；
- 前端没有统一门禁禁止 `items.reduce(...)` 生成业务金额/计数；
- 后端 `Page<T>` 返回形式上正确，但部分 Provider 实现仍是内存分页；
- 选择器和小列表、单张单据有限明细需要单独分类，不能机械要求所有 `List<T>` 都改 Page；
- 必须优先处理金额、成本、利润、应收应付和经营看板。

## 4. 处理建议

1. 先用本规范完成接口分类和指标 registry 对账；
2. 金额类页面优先补 report/summary Provider；
3. 增长列表改为数据库分页；
4. 选择器改为服务端关键词搜索 + 分页；
5. 删除前端业务 reduce；
6. 每批做同筛选下 `totalElements == report.count` 和金额对账；
7. 再做 10 倍数据量和 P95 验收。

## 5. 规范入口

`docs/design/33-后端分页与聚合接口强制规范.md`
