# 合同与增减项履约 · 实现记录

## 本批范围

- Batch：B1
- 垂直切片：业务对齐、真实场景、结构化蓝图、字段与实现现状核对
- 不包含：代码修改、迁移、页面改造、发布

## 变更

- 后端：无新增代码；核对现有 `batch-save`、`range`、`approve`、旧 `save` 路径。
- App：无新增代码；登记发起/详情/工作台入口。
- Web：无新增代码；登记变更工作台入口。
- 迁移：无。
- 配置：无。

## 测试与证据

- 定向测试：尝试执行合同变更相关 7 组测试；当前工作区 Spring 上下文因其他未完成改动缺少 `finance/service/aging/report/support/Report.class` 启动失败，未形成本域测试结论。
- 构建/类型检查：本批仅做文档与 JSON 校验；未把失败环境记作通过。
- 真实页面/真机：本批未做，B3/B4 负责。
- 部署/重启：无。
- 截图/日志：无。

## 风险与未闭环

- 旧 `project/contract-change/save` 与 `contractchange/batchsave` 仍并存；App 报价详情仍有旧调用，必须收敛。
- Web API 已接 range/batch-save/preview，页面真实使用证据待 B3。
- 历史 `Approved`/`CustomerConfirmed` 展示与存量治理待核对；`customer-confirm` 仅可作为历史收口。
- 历史缺目标快照记录必须标记暂不可确认，不得用 0。
- 当前工作区有大量其他任务未提交改动，测试上下文不可用；不能据此宣称本域代码失败或通过。
