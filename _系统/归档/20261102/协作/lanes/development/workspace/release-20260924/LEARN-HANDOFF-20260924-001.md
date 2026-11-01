---
id: LEARN-HANDOFF-20260924-001
type: post_fix_learning_handoff
status: DEV_FIXED
updated: 2026-09-24
sources:
  - XA-20260924-001
  - XA-20260924-002
  - XA-20260924-003
  - XA-20260924-009
  - DEF-MEASURE-WS-001
  - DEF-MEASURE-WS-005
---

# 修复后学习包入口

## 关联问题

- ISSUE-ID：MH-OWNER-AUDIT-20260924 / BOSS-MEASURE-WORKSPACE-2
- REQ-BIZ：REQ-BIZ-20260924-002（业主整改复验与主动发起边界）
- GAP/DEC：GAP-010 / DEC-009 / BR-008 / SCN-010
- 产品版本/commit：backend `3b1488b`；App `8bf0b29`；RC tag `v20260924-rc1`

## 修复事实

- 问题原因：业主项目详情、首页、旅程直接信任可漂移的 `Project.status/currentStageName`；量房经营报告用客户当前状态和当前时刻截断历史事件。
- 修改范围：业主四条同源投影显式组装；预约量房自然日、历史客户事件、4h 逾期、绩效筛选、业务员入口；发布包迁移范围 24–59。
- changed_files：`rules.md` §5.6、`AGENTS.md`、`ECD-011`、`CustomerJourneyContext.java`、`customer/project/{info,page}`、`Report.java`、`BossMeasureQuery.java`、App owner 页面与 `measure-workbench.vue`、`assessment.vue`、`permission.ts`、`build-release.sh`、迁移 54–59。
- 状态/权限/金额/接口影响：未改金额公式；业主仍只读本人 Active 项目；员工不能代业主确认；4h 与月份/门店筛选跨端统一。
- 历史数据处理：迁移 55 修正应付账期；56 增加整改类型/图片；57 建意见投诉；58 回填设计状态；59 回填 craft；56 已改为可重试。

## 验证证据

- 自动化测试：后端全量 319/319 PASS；量房探针 4/4；Provider 安全 312 路由 PASS。
- 手工复现：微信开发者工具真实业主登录，首页/详情/旅程状态与阶段一致。
- 正向：施工中 / 开工交底跨页一致；整改责任人和截止可见。
- 反向：未知路由/缺少授权标识拒绝；Provider 安全覆盖。
- 越权：ProviderSecurityAcceptanceTest 覆盖。
- 幂等：迁移 56 临时 MySQL 连续执行两次 PASS。
- 跨页面一致性：首页、详情、旅程三处修复；自动断言待补。
- 重启/部署证据：后端 release artifact `dist/releases/20260924-042911/th4-decoboon-1.0.0-build.tar.gz`，SHA256SUMS 校验 PASS；生产部署待执行。

## 学习要求

- 需要学习的事实：对外可见投影必须显式组装；业务历史事件不能按对象当前状态截断。
- 需要修正的旧判断：页面能渲染不等于后端字段已组装；前端兜底会掩盖投影缺陷。
- 需要新增的反例：已签约客户的历史量房；今天已完成/未来时段的预约；4h 边界。
- 防复发规则候选：`ECD-011`；量房 2.0 自然日与历史事实口径。
- 搜索/监控关键词：`currentStageName`、`Project.status`、`84 * 3600`、`ACTIVE_PRE_MEASURE`。

## 学习包

- learning packet path：待学习智能体生成
- validate_learning 命令：`python3 scripts/validate_learning.py <learning-packet.json>`
- 校验结果：待执行
- 仍未解决的问题：整改复验入口、业主主动发起、待我处理展开、真机硬件与支付回调。

## 回执

- 学习智能体结论：待处理
- 可否进入 VERIFIED：仅静态/自测，不可；需 acceptance 独立验证
- 营销可说：装修事实链与状态一致性已修复，待独立验收
- 营销不可说：不得宣称整改复验、业主发起、真机支付已上线
- 必须保留条件：生产迁移与部署未完成，业务边界待裁决
