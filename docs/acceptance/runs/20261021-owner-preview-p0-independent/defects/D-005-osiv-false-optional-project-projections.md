# D-005 · OSIV=false 下部分可选项目详情投影仍懒加载失败

- 级别：P2（不阻断业主视角预览，但违反关闭 OSIV 也必须正确的工程规范）
- 环境：`decoboon-test`，`spring.jpa.open-in-view=false`
- 已观测：
  - `finance/project/report`：`LazyInitializationException`（Customer/Store）
  - `project/report/page`：`LazyInitializationException`（Store）
  - 更早还观测过 `project/project/page`、`finance/summary/report`
- App 影响：
  - 详情页对这两个结果使用 `.catch(...)` 容错，因此不阻断“业主视角”入口和预览页
  - 后续应修复，避免错误被页面静默吞掉、项目详情局部数据缺失
- 证据：
  - `../migration/45-test-runtime-final.log:748,853,1182,1412`
- 建议：按同类模式为报告/财务投影 Provider 增加只读事务、限定企业范围预取和分页上限，并补 OSIV=false 回归。
