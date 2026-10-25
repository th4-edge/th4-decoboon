> 说明：该记录是已被用户撤回的“Web 全局合同档案”方案，不再作为当前验收依据。当前方案见 `20261023-project-contract-archive/`。

# 2026-10-23 设计师 Web 合同档案 D1

> 状态：`WITHDRAWN_BY_USER / SUPERSEDED`  
> 环境：本地 dev；未提交、未部署、未连接生产。

## 结果

- `/designer/contract-archive` 已可访问并出现在 Designer「资料与档案」菜单；
- 服务端复用 `project/contract/page`，不要求 `projectId`；
- 数据范围由 `ProjectAccessContext.visibleContract` 裁剪；
- 支持服务端分页、关键词、合同状态、签约起止日期；
- 关键词覆盖合同号、客户、房屋、报价号、项目号/名称；
- 当前设计师样例：王亮 / `CC1790371757137` / 已生效 / 项目王亮梦圆小区；
- 详情抽屉通过 `project/contract/info` 读取当前合同；
- 可跳转客户 360 和项目工地；
- 1440/1024 无横向溢出，console 0 error。

## 验证

```text
mvn -Dtest=DesignerContractArchivePageTest,... test  PASS（12/12）
Web type-check / build / actions / permissions / workbenches / workspaces PASS
```

筛选探针：

```text
keyword=王亮 + status=Effective => 1 条
keyword=不存在合同 + status=Effective => 0 条
status=PendingSign => 0 条
signedFrom=2026-07-01 + signedTo=2026-07-31 => 1 条
signedFrom=2026-08-01 => 0 条
```

## 截图

- `output/playwright/20261023-designer-web-acceptance/designer-contract-archive-1440.png`
- `output/playwright/20261023-designer-web-acceptance/designer-contract-archive-1024.png`

## 性能与检索原理复核

- API：`POST /api/project/contract/page`。
- 列表不再逐行调用 `ProjectContractContext.enrich`，应收、变更、合同行、付款计划、施工计划只在详情 `project/contract/info` 加载。
- 列表使用 EntityGraph 预取客户、房屋、报价、项目、创建人，避免序列化时逐行懒加载。
- MySQL general_log 实测一次列表请求：3 条认证/订阅上下文 SQL + 2 条档案 SQL（count + content），固定 5 条业务 SQL，不随每页 20 条线性增长。
- 关键词规则：合同号/报价号/项目号为前缀匹配；客户名、项目名、小区和地址为包含匹配。
- 当前未引入全文索引：中文包含检索仍可能扫描候选集。当前中小企业数量可接受；规模化前应增加规范化搜索索引或专用检索列，不能把 `LIKE '%关键词%'` 当作长期方案。
