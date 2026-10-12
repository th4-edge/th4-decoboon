# 2026-10-12 事故与口径处置：新客户指定设计师 + 量房长文本启动校验

关联：`INC-20261008-b241888433`、`INC-20261007-979f5a015e`、发布记录 `v2026.10.12.1`、`DEC-065`、`ECD-009`、迁移 `70`

## 一、起因（用户报障）

> App 端指定设计师报错，线上店长指定设计师没有成功。

线上取证（nginx + 后端 stdout + MySQL）：

| 时间 | 事实 |
| --- | --- |
| 16:45:34 | 店长王宏文建客户「陈女士」（`cust_id=235321989517017088`，自然到店），`status=New` |
| 16:46:24 | 录入房屋「悠然居 22 栋 206」 |
| 16:46:29–16:47:18 | 微信小程序 7 次 `POST /api/customer/customer/handoff` |
| 同上 | 后端 7 条 `customer/customer/handoff处理失败:客户尚未跟进或已流失，不能指定设计师`（`Handoff.java:100`） |

- 全站当日 `handoff` 请求 7 次，**0 成功**；客户 `last_follow_at` 为空、无跟进记录 → 命中 `CustStatus.New` 门禁。
- 该指纹在 2026-10-08 已出现 15 次（`INC-20261008-b241888433`），当时分类 `unknown`、未闭环。

### 归因

不是接口缺陷，而是**前端入口与后端业务规则不一致**：App 客户详情页「负责设计师」行只按角色权限放开（`detail.vue:137 canChangeDesigner`），后端要求客户状态非 `New/Lost`。Web 端同样只挡了 `Lost`（`StoreCustomerView.vue:220`）。

### 裁决与处置（DEC-065）

用户裁决：**新客户（待首联）允许直接指定设计师**——自然到店客户由店长当场派单，首次跟进由接手的设计师完成，客户状态不随派单推进。

- 代码：`Handoff` 去掉 `New` 门禁；`Lost` 仍拒绝并拆分为独立文案「客户已流失，请先恢复客户后再指定设计师」。
- 测试：新增「店长对 New 客户派单成功且状态不变」「Lost 仍被拒」两条定向测试。
- 口径同步：装得清业务智能体 `DEC-065`、`ANS-BIZ-20261009-002` §4、`BR-033`、公开教程 `ANS-BIZ-20261007-017`、`SCN-001`。
- `INC-20261008-b241888433` 结论：**规则冲突（口径）**，非代码缺陷，随 `DEC-065` 关闭。
- 未改动（本轮明确不动）：`New` 客户仍不能「约定量房」（`Measure.java` 保留「请先完成首次跟进，再约定量房」）；派单后由接手设计师写跟进转入 `Following` 再约量房。

## 二、次生事故：重启即崩（schema validate）

处置上述改动时首次发版失败并触发自动回滚，进一步排查发现**生产一旦重启就无法启动**：

```text
Schema-validation: wrong column type encountered in column [customer_requirements]
in table [db_measure]; found [longtext (Types#LONGVARCHAR)], but expecting [tinytext (Types#CLOB)]
```

### 根因

2026-10-11 15:34 补执行迁移 70（`db_measure` 四列 `tinytext → longtext`，ECD-009），**当时未重启服务**（见运维记录原文「未重启服务」）。实体仍用 `@Lob`（Hibernate 期望 `tinytext`），`ddl-auto: validate` 只在启动时校验 → 潜伏至今，任何重启（发版、机器重启、OOM 重启）都会 502。

- 影响窗口：2026-10-12 19:54–20:12，约 18 分钟不可用（由本人发版触发的重启暴露）。
- 修复：`Measure` 四个长文本字段改为工程既有写法 `@Column(columnDefinition = "longtext")`，与迁移 70 / ECD-009 对齐。
- 验证：部署后 `Started Application in 46.847 seconds`；制品内 `Measure.class` 含 `longtext`。

### 附带发现（阻断后续发版）

dev HEAD `462828d`（DEC-064 第 1 期）新增 `design_template_room.reference_area/reference_perimeter`，但：

1. 迁移 `108-模版区域参考实测面积周长.sql` **未进发布包**——`scripts/build-release.sh` 的 `MIGRATION_END` 仍为 105；
2. 生产库缺列，从 dev 发版会直接 validate 失败。

本次热修因此把基线回退到 `4982c43`，**明确排除** `462828d`。

## 三、必须遵守的预防措施

1. **改列类型/补执行迁移后必须立即重启一次**并确认 `Started Application`，否则问题会潜伏到下一次重启（本次即 18 分钟生产不可用）。
2. 发版前先确认 `MIGRATION_END` 覆盖新增迁移编号，并对目标库做一次「缺列自查」。
3. `@Lob` 与本工程长文本写法不一致时，统一用 `@Column(columnDefinition = "longtext")` + 同步迁移。
4. 前端入口的门禁必须与后端 Provider 的校验一致；规则冲突时以业务裁决（DEC）为准，不留在代码里互相矛盾。

## 四、遗留

1. dev 需 cherry-pick `0aadfdf`、`15ba442`（否则下次从 dev 发版会回退本次热修）。
2. dev HEAD 待补齐迁移 108 进包 + `MIGRATION_END` 提升后方可发版。
3. App/Web 入口引导优化（本轮未做，后端已放开，入口原本已可用）：
   - App 客户详情「负责设计师」在 `New` 状态下可加一句引导「待量房前也可先派单，跟进由接手设计师完成」；
   - 设计调度页「待指派」清单目前只列 `PendingMeasure` 客户，可评估是否纳入 `New` 未派单客户。
