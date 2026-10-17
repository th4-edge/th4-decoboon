# 材料域下一批提示词（B9 体验版与发布准备）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`、`state.json`；
3. 读取 `00-charter.md`、`03-blueprint.json`、`04-requirements.md`、`05-plan.md`、`06-implementation.md`、`07-ui-review.md`、`08-acceptance.md`、`09-release.md`、`10-reconciliation.md`；
4. 读取 B6.6、B7、B8 的 summary、environment、verification-results、ui-metrics、screenshots；
5. 涉及发布、权限、金额/成本时读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 与相关 BR/DEC。

当前状态：B8 助手 UI 独立验收 PASS 110/110；材料域已记录 `ACCEPTED`；B9 为体验版与发布准备。以 `state.json` 为准。

## B9 垂直切片

只做体验版/发布准备与冒烟，不做新业务功能：

1. 核对 B8 验收提交与当前待发布提交是否一致；
2. 后端构建/发布包校验；
3. Web type-check/build 与静态产物校验；
4. App type-check/build:mp-weixin/分包/UI 规范/版本号校验；
5. 上传小程序体验版并验证可打开；
6. 冒烟：材料蓝图、材料体检、App 材料主链入口、问材料项目/作业单/采购/结算上下文；
7. 更新 `09-release.md`、`state.json/history`、`10-reconciliation.md` 和必要证据；
8. 记录版本、提交、上传信息、体验版状态、回滚路径。

## 明确边界

- 只上传开发版/体验版；正式提审、正式发布由用户本人完成；
- 不新增业务规则、DDL/迁移、权限/金额/成本语义；
- 不自动审批/付款/删除/改权限；
- 不把构建通过当作体验版验证通过；
- 发布证据必须来自精确提交和真实页面。

## 退出条件

- 体验版上传成功并有版本/上传信息；
- 真实体验版页面冒烟 PASS；
- 发布记录、回滚路径和未闭环项完整；
- 停在 `RELEASED`（体验版）或写用户决策包；正式版仍未发布。
