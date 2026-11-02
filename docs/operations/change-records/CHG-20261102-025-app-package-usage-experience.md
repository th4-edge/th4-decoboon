# CHG-20261102-025 · App 套餐详情、企业分区与项目分组体验

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261102-025` |
| 类型 | App/后端体验增强与 Provider 响应扩展 |
| 风险级别 | L2（跨端 Provider 响应、权限裁剪和套餐页面交互；无 DDL、无金额公式变更） |
| 状态 | 开发自测与微信开发者工具验证通过，待独立验收和发布 |
| 负责人 | Codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | 用户 2026-11-02 App 套餐使用反馈；`DEC-083`、`BR-051`、`BR-001`、`BR-002`、`OBJ-010`、`DEC-095` |

## 2. 目标与范围

- 目标：让负责人可查看套餐详情、复用企业分区、通过价格识别相近项目并按类型核对配置。
- 范围内：已发布套餐详情、企业级分区默认加载与全宽展示、分区/分项类型折叠、项目数/参考金额摘要、项目行价格展示、服务端聚合统计。
- 受影响端：App、后端套餐详情/分项分页 Provider；不涉及 Web/Admin 页面。
- 非范围：价格/成本公式、套餐生命周期、角色权限、DDL、Web/Admin 页面和发布。

## 3. 已确认事实

- 套餐管理页草稿不可见问题已修复：App `11a28af`、后端 `522c74a3`，均已进入 `dev/test/prod`；App `1.0.81/181` 开发版已上传，后端发布 `20261102-123251`。
- 当前套餐分区模型 `PackageSection` 已归属企业；本文不改变该归属。
- 当前 App 分区组件原使用横向滚动标签；项目行原只显示本行成本；项目列表原未按类型分组。

## 4. 实现

| 仓库 | 范围 | 主要内容 |
| --- | --- | --- |
| th4-decoboon-app | 套餐管理/详情/配置 | 新增只读套餐详情；列表进入详情；新建套餐默认加载企业分区；分区改全宽卡片；明确当前分区和切换提示；分区/类型分组支持展开关闭；显示项目数和参考金额；项目价格按类型分组；统计缺失时仍按项目行展示并提供就地刷新；成本按权限隐藏；套餐契约增补 |
| th4-decoboon | 套餐详情/类型统计 | `pack/template/info` 返回全企业分区 0/非0项目数、分区/类型统计、分区/类型标准参考金额和参考成本；补齐固定总价字段；非成本角色裁剪行成本快照和参考成本 |

关键文件：

- App：`src/subpackages/quote/pages/package-detail.vue`
- App：`src/subpackages/quote/components/PackageLineGroups/PackageLineGroups.vue`
- App：`src/subpackages/quote/components/SectionTabs/SectionTabs.vue`
- App：`src/subpackages/quote/components/LineCard/LineCard.vue`
- App：`src/subpackages/quote/pages/package-edit.vue`
- 后端：`src/main/java/com/th4/edge/decoboon/pack/context/PackageTypeSummaryContext.java`
- 后端：`src/main/java/com/th4/edge/decoboon/pack/service/templates/info/support/Info.java`
- 后端：`src/main/java/com/th4/edge/decoboon/pack/context/PackageCostContext.java`

## 5. 验证

| 检查 | 结果 |
| --- | --- |
| 代码路径核对 | 已完成 App 套餐管理、套餐配置、分区组件、项目行组件和后端套餐/分区模型核对 |
| 草稿可见性修复核对 | App 提交 `11a28af` 已进入 `dev/test/prod`；后端提交 `522c74a3` 已进入 `dev/test/prod` |
| 发布记录核对 | App 1.0.81 开发版已上传；后端 20261102-123251 已发布 |
| App 套餐契约 | `npm run check:package-config -- --self-test` PASS，19 个负向夹具被拒绝 |
| App 全量门禁 | `npm run verify:mp-weixin` PASS |
| 后端定向回归 | `PackageTypeSummaryContextTest`、`PackageTypeSummaryIntegrationTest`、`PackageCostContextTest`、Provider 注入契约 PASS |
| 后端全量测试 | `mvn -q test` PASS：315 个测试类 / 1224 个用例，0 失败、0 错误、0 跳过 |
| Provider 响应审计 | `audit-provider-response-contract.py --check` PASS，findings=0 |
| 微信开发者工具页面 | 套餐列表、发布详情、企业分区、项目价格与类型分组 4/4 PASS；卫生间有分项时显示 1项/参考 ¥6.80、折叠分组与金额摘要 2/2 PASS |
| 证据 | `docs/acceptance/runs/20261102-app-package-config-experience-dev/`、`docs/acceptance/runs/20261102-app-package-collapse-dev/` |

## 6. 停止条件与下一步

- 当前开发自测完成，不继续扩展 Web/Admin 或套餐关系表。
- 下一步进行独立验收、物理真机测试和发布评审。
- 物理真机、生产发布和独立验收未执行，不得宣称生产可用或完整验收。
- 若后续要新增套餐与分区关系表，必须另立业务/数据评审，补充主源、历史快照、迁移和回滚方案。

