# 逐角色测试感受与客观评价

## 业务员 Marketer

`FIXTURE_ONLY`。客户归属、来源、预算与负责人数据存在；未从空客户开始执行跟进、量房邀约和状态流转。

## 店长 StoreManager

`PASS_READ_ONLY_AFTER_FIX`。本店客户 1、待开工项目 1、台账 4 条；跨店被拒绝。财务报告修复前曾因 JSON 截断显示伪 0。台账“更正/删除”动作待业务裁决。

## 团队负责人 TeamLeader

`NOT_TESTED_BY_RULE`。账号存在，但现行业务规则禁止登录；不作为可操作角色。

## 设计师 Designer

`FIXTURE_ONLY`。方案、报价和项目设计负责人事实存在；未完成设计工作台按钮操作、方案锁定与版本变更。

## 项目经理 Pm

`FIXTURE_ONLY`。项目 8 阶段、2 任务存在；未执行任务推进、日报、验收或整改。

## 履约协同 ProjectSupport

`NOT_TESTED`。本测试企业未单独走履约协同账号。

## 财务 Finance

`PASS_READ_ONLY_AFTER_FIX`。财务首页、项目财务、应收、收款、应付、付款、台账和项目成本完整性均可读；金额与 DB 一致；付款进度标签已纠正。高危冲正、退款、结算关闭未执行。

## 企业负责人 Boss

`PASS_READ_ONLY`。App 首页展示签约 ¥2,496、回款 ¥749、在建 1；Web 财务工作台与 App/DB 金额一致。企业级客户/项目入口可见。

## 业主

`NOT_TESTED`。未发送邀请、未做微信授权、未代替业主验收。
