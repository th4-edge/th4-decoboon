# 企业运营标准资产 · 需求与验收条件

| ID | 用户故事 | 前置条件 | 权限 | 数据/状态 | 页面/交互 | API/事实 | 验收 | 来源 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-EOP-001 | 内部员工可读取五类资产的统一生命周期契约 | 已登录有效内部员工 | 企业范围 | 平台契约，不含企业经营数据 | 供 App/Web 统一展示 | `standard-asset/lifecycle/list` | 返回恰好五类资产 | `DEC-083` |
| REQ-EOP-002 | 前端可按资产展示不同状态，而非同一状态机 | 契约已返回 | 同上 | 五类状态集合与业务语义匹配 | 状态标签/可编辑判断 | 只读投影 | 套餐/计划/凭证为三态，报价为 Active/Inactive | `BR-051` |
| REQ-EOP-003 | 前端可区分修订型和不可变版本型资产 | 同上 | 同上 | Revision / ImmutableVersion | 版本展示 | 只读投影 | 报价为 Revision，其余四类为 ImmutableVersion | `DEC-083` |
| REQ-EOP-004 | 前端可区分企业自建与平台基线复制 | 同上 | 同上 | CompanyOnly / BaselineCopy | 来源提示 | 只读投影 | 报价/套餐无基线；计划/凭证可复制 | `BR-051` |
| REQ-EOP-005 | 前端可展示业务引用和快照最小字段 | 同上 | 同上 | source fields / snapshot fields | 历史来源说明 | 只读投影 | 每类资产均有来源字段和快照字段 | `BR-051` |
| REQ-EOP-006 | 契约不承载金额或成本计算 | 请求契约 | 同上 | 无金额字段 | 无 | 只读投影 | 反射/结构测试确认无金额字段 | `T12-BIZ-20261022-004` |
| REQ-EOP-007 | 未登录或非内部员工不可读取契约 | 无有效员工 | 拒绝 | 安全边界 | 异常恢复 | 403 或框架等价响应 | Provider 继承 Security 并校验员工 | `AGENTS.md` |
| REQ-EOP-010 | 店长创建套餐草稿并配置项目行 | BOSS/店长；企业未发布套餐 | 维护角色 | Draft；version=1；code 自动生成 | App 套餐管理 | `pack/template/save` | 新建后不可用于报价 | `DEC-083` §3 |
| REQ-EOP-011 | 发布套餐后禁止原地修改 | Draft 且至少一个项目行 | BOSS/店长 | Draft→Published；currentPublished=true；快照 JSON/hash | App 发布按钮 | `pack/template/publish` | 已发布内容不可编辑；发布版本永久保留 | `DEC-083` §3 |
| REQ-EOP-012 | 从当前版本创建下一版 | 当前企业最新版本 | BOSS/店长 | 新 Draft；version+1；复制行/差项/说明 | App 创建下一版 | `pack/template/build` | 旧发布版本仍可用于历史，新报价继续使用旧发布版直到新版本发布 | `DEC-083` §3 |
| REQ-EOP-013 | 停用当前发布套餐 | currentPublished=true | BOSS/店长 | Published→Disabled；currentPublished=false | App 停用 | `pack/template/disable` | 新报价拒绝；历史报价/合同/项目不变 | `DEC-083` §3 |
| REQ-EOP-014 | 新报价绑定套餐来源版本 | 选择当前发布套餐 | 报价角色 | 记录 templateId/code/version/snapshotHash 和行值快照 | App/Web 报价选择器 | `quote/save` | 草稿、历史或停用版本不能用于新报价 | `BR-051` §3 |
| REQ-EOP-015 | 存量套餐迁移不伪造旧版本 | 存量 pack_template | 系统迁移 | version=1、Published、current=true；line snapshot 回填；quote 来源回填 | 不涉及 | 迁移 124 + rollback | 迁移和回滚在隔离 MySQL 验证 | `T12-BIZ-20261022-004` |
| REQ-EOP-016 | App 只选择当前发布套餐 | 套餐选择器 | 全部内部角色只读选择 | 仅 Published + currentPublished | App 选择器/报价确认 | `pack/template/list` | 草稿/历史/停用不出现在新报价选择器 | 用户 2026-10-22 双端裁决 |
| REQ-EOP-020 | 付款/施工模板版本行 | 草稿创建 | BOSS/店长 | 同 code 多版本，currentEnabled 唯一 | App 管理 | `build/publish/disable` | 旧版本不可原地改 | `DEC-083` §4 |
| REQ-EOP-021 | 显式平台升级 | 当前企业版本有来源基线 | BOSS/店长 | 新 Draft 保留来源 code/version | App 升级按钮 | `*/build` + baselineCode | 平台升级不自动同步 | `BR-051` §4 |
| REQ-EOP-022 | 无默认不静默选择 | 合同解析无 code | 系统 | 必须有 current+default，否则 400 | 不涉及 | `resolve*Plan` | 不按排序/基线兜底 | `DEC-083` §4 |
| REQ-EOP-023 | 企业默认唯一 | 发布不同 code 默认版本 | BOSS/店长 | 全企业最多一个 current default | App 模板列表 | `publish*` | 新版本继承同 code 默认时清理其他默认 | `DEC-083` §4 |
| REQ-EOP-024 | 施工总工期快照 | 合同选择施工模板 | 系统 | 合同保存 sourceTotalDays 与阶段快照 | App/Web 合同详情 | `ContractConstructionPlan` | 120 天声明与阶段权重 100 并存 | `DEC-048` |
| REQ-EOP-025 | 多版本回滚保留证据 | 回滚迁移 127 | DBA | 归档非保留版本和明细后恢复旧唯一键 | 不涉及 | rollback 127 | 回滚成功且归档表有数据 | `T12-BIZ-20261022-004` |
| REQ-EOP-026 | 平台升级入口可达 | 企业模板来源版本低于平台 | BOSS/店长 | API 返回 sourceBaselineCode/version | App 显示升级按钮 | view model + App | 入口可识别并创建升级草稿 | 用户双端裁决 |
| REQ-EOP-030 | 报价项目有效变更递增 revision | 已存在且可维护的报价项目 | BOSS/店长/财务 | 名称、规格、计价、售价、成本、公式、工艺、状态或业务明细变化 | App/Web 编辑 | `catalog/item/update` | revision 只递增一次，并返回新 revision | `DEC-083` §2、`BR-051` §2 |
| REQ-EOP-031 | 每次有效变更保存不可变审计 | 有效变更已提交 | 系统 | `catalog_item_revision` 保存 revision、类型、操作人、时间和完整业务快照/hash | 历史入口可读取 | `CatalogRevisionContext` | 成功提交与审计同事务；审计不更新、不删除 | `BR-051` §2–3 |
| REQ-EOP-032 | 新报价行保存报价项目来源修订 | 选择 Active 报价项目生成新报价行 | 报价角色 | `catalogSourceId/code/revision` 与实际使用值快照 | App/Web 报价 | `QuoteLine` 来源字段 | 新报价行来源与当时项目 revision 一致 | `DEC-083` §2.6 |
| REQ-EOP-033 | 无业务变化的保存不产生虚假修订 | 请求值与当前业务值一致 | 维护角色 | revision 与审计均不增加 | 页面可提示已保存 | `catalog/item/update` | 金额仅 scale 不同、sort/sourceType 变化不算有效变更 | `DEC-083` §2.5 |
| REQ-EOP-034 | 并发基于旧 revision 提交返回冲突 | 两个写请求携带同一期望 revision | 维护角色 | item 级 Redisson 锁后开独立事务；旧 revision 返回 409 | 提示刷新后重试 | `CatalogMutationContext` | 锁后读到新 revision；无静默覆盖和裸 1062 | `AGENTS.md`、`DEC-029`、`BR-051` §2 |
| REQ-EOP-035 | 已引用项目禁止物理删除 | 项目被设计/报价/套餐/合同/材料等引用 | BOSS/店长/财务 | 拒绝删除并提示停用；未引用项目删除前保留 Delete 审计 | App 删除 | `catalog/item/delete`、`CatalogImportReferenceContext` | 引用存在时不删项目和审计；历史引用不丢失 | `DEC-083` §2.4、`DEC-079` |
| REQ-EOP-036 | 历史接口按角色裁剪成本快照 | 内部员工读取修订历史 | 全部内部角色可读；成本仅 BOSS/店长/财务 | 非成本角色不返回 snapshotJson 中的成本内容 | Web/App 历史 | `catalog/item/revision/page` | 非成本角色拿不到 `costPrice`，但仍可看 revision 元数据 | `DEC-079`、`BR-051` §6 |
| REQ-EOP-037 | 修订历史企业隔离与只读 | 已登录内部员工 | 企业范围 | 只按登录员工企业 + itemId 查询，跨企业返回空/拒绝 | Web/App 历史 | `catalog/item/revision/page` | 无跨企业快照泄漏；接口无写能力 | `AGENTS.md`、`DEC-083` §6 |
| REQ-EOP-038 | 存量迁移与回滚保留审计事实 | 存量 catalog/quote 数据 | DBA | revision=1、Create 审计、hash、报价来源回填；回滚归档审计和来源字段 | 不涉及 | 迁移 128 + rollback | MySQL 8.4 迁移/回滚、hash、唯一键和归档验证通过 | `T12-BIZ-20261022-004` |
| REQ-EOP-039 | App/Web 同 revision 与同异常语义 | 同一报价项目在两端口打开 | 两端同权限 | 列表/详情显示 R{n}；维护端携带 revision；历史可刷新重试 | App/Web | 两端 catalog API/model | 旧 revision 均提示冲突；非成本角色同一裁剪语义 | 用户 2026-10-22 双端裁决、`DEC-083` §8 |

## 非功能要求

- 性能：固定五条内存投影，不查询业务表。
- 可访问性：后续页面必须用文本表达状态和来源，不只依赖颜色。
- 异常恢复：读取失败显示可重试错误，不得伪装空列表。
- 审计/幂等：EOP-V1 只读；EOP-V4 有效变更才递增 revision，无变化保存不新增审计，并发旧 revision 返回 409。
- 跨端一致性：App/Web 使用同一 Provider 语义。

## 明确不做

- 报价项目库不改成发布型模板，不引入平台行业报价项目基线。
- 不改变销售价、标准成本、合同金额、应收应付和凭证金额公式。
- 不实现套餐默认、平台套餐基线和凭证历史回读；分别由 `GAP-063`、EOP-V5 管理。
- 不实现套餐默认或平台套餐/报价项目基线。
