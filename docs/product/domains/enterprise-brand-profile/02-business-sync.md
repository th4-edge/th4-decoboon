# 装企信息与品牌投影 · 业务智能体同步

## 咨询请求

- 请求编号：`REQ-BIZ-20261029-001`
- 发起角色：product-planning
- 业务问题：装企法定主体、品牌、Logo、地址、地图、联系方式和单据抬头的主源、受众、快照和投影边界。
- 涉及对象：Company、Store、DocumentProfile、Property、Project、CustomerJourney.Vendor、DocumentRecord、BrandAsset、ServiceLocation。
- 涉及状态：主档有效/版本、Logo 有效/停用、文档快照冻结。
- 涉及权限：BOSS 企业治理；店长门店信息边界；业主只读；财务单据资料。
- 涉及金额/成本：不直接涉及金额，但影响合同、凭证和银行资料展示。
- 候选方案：见 `01a-company-information-concept-map-v0.1.md` 与 `REQ-BIZ-20261029-001`。

## 业务答复

- 答复编号：待生成
- 结论状态：`BIZ_TRIAGE_PENDING`
- 证据：待补
- 标准口径：待业务智能体答复
- 例外：待业务智能体答复
- 不可以怎样理解：当前代码字段不等于正式业务事实
- 权限与数据范围：待裁决
- 金额/成本/指标：不改变金额公式，但银行/税务字段需要受控展示
- 页面/API/迁移影响：待 BIZ_ALIGNED 后形成蓝图

## 未决项

| GAP/问题 | 需要谁裁决 | 影响 | 临时处理 | 状态 |
| --- | --- | --- | --- | --- |
| 法定名称、品牌名、简称、门店名展示层级 | zhuangdeqing-business | 业主、合同、导出 | 不进入编码 | OPEN |
| DocumentProfile 主源/投影/快照定位 | zhuangdeqing-business | 单据抬头、历史文件 | 保留现状只读盘点 | OPEN |
| 品牌快照冻结时点 | zhuangdeqing-business + 用户 | 历史合同和业主页 | 不使用当前主档覆盖旧文件 | OPEN |
| 地址语义和地图点位 | zhuangdeqing-business + 用户 | 门店导航、业主展示 | 不新增坐标字段 | OPEN |
| Logo/品牌色/门店覆盖 | zhuangdeqing-business + 用户 | 品牌露出、打印、邀请 | 不使用平台 Logo 冒充企业 Logo | OPEN |
| 税号/法人/银行信息/资质受众 | zhuangdeqing-business | 合同、凭证、业主知情权 | 不扩大展示 | OPEN |
| 导出/打印必须包含的字段 | zhuangdeqing-business + 用户 | PDF/Excel/打印模板 | 不做字段承诺 | OPEN |

## 同步状态

- 上次同步：2026-10-29 提交 T05
- 业务知识卡版本/更新时间：待答复
- 蓝图是否 stale：是
- 需要重新同步的节点：全部

## 2026-10-29 用户补充决策

`USER-DEC-20261029-003` 已确认：

```text
Company = 企业事实唯一根
DocumentProfile = 单据投影/生成时快照（不再作为独立企业主档）
快速入驻 + 我的→企业信息渐进完善
缺字段允许展示占位，但占位不写入事实、不进入合同/凭证/对外承诺
```

后续 T06/T12 只需裁决字段语义、占位/阻断门禁、地址/地图、Logo 资产、品牌快照和输出字段白名单。
