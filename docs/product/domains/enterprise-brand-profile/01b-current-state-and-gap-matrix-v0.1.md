# 装企信息与品牌投影 · 当前状态与缺口矩阵 v0.1

> 基线：本地集成版 1.0.73，后端 `ce5f295`、Web `ef56107`、App `f2fb91e`。  
> 性质：代码事实盘点，不把当前实现当作正式业务规则。

## 1. 当前承载对象

| 概念/字段 | 当前承载 | 类型 | 维护入口 | 当前消费者 | 结论 |
| --- | --- | --- | --- | --- | --- |
| 企业全称 | `Company.title` | 主源候选 | App/Web 企业资料 | 业主服务方、邀请、Company 投影 | 存在 |
| 企业简称 | `Company.name` | 主源候选 | App/Web 企业资料 | 会话/部分企业卡片 | 存在；与对客品牌名关系未定义 |
| 企业 Logo | `Company.logo` | 品牌资产候选 | 后端支持，前端未见上传入口 | 邀请卡片、业主 Vendor | 入口和资产语义不完整 |
| 品牌色 | `Company.color` | 品牌配置 | 后端支持，前端未见编辑入口 | 业主 Vendor | 存在 |
| 企业地址 | `Company.addr` | 地址候选 | App/Web 企业资料 | 企业资料、默认单据地址 | 语义不明确 |
| 服务区域 | `Setting.serviceArea` | 服务信息 | App/Web 企业资料 | 业主 Vendor、分享页 | 存在，自由文本 |
| 企业客服 | `Setting.serviceTel` | 联系通道 | App/Web 企业资料 | 业主 Vendor、分享页 | 存在 |
| 售后专线 | `Setting.afterSaleTel` | 联系通道 | App/Web 企业资料 | 业主 Vendor | 存在 |
| 税号 | `Setting.uscc` | 法定信息候选 | App/Web 企业资料 | 业主 Vendor | 存在；与 DocumentProfile.creditCode 重复 |
| 资质 | `Setting.qualification` | 信任信息候选 | App/Web 企业资料 | 业主 Vendor | 仅文本，无附件/有效期 |
| 简介 | `Setting.intro` | 品牌/服务信息 | App/Web 企业资料 | 业主 Vendor、分享页 | 存在 |
| 叙事主品牌 | `Setting.brandPrimary` | 展示策略 | App/Web 企业资料 | 未见完整消费链 | 策略字段，当前消费不足 |
| 平台存证署名 | `Setting.custodianCredit` | 展示策略 | App/Web 企业资料 | 业主分享/旅程 | 存在 |
| 门店名称 | `Store.name` | 门店组织事实 | 门店维护入口 | 员工/业务归属 | 存在 |
| 门店地址 | `Store.addr` | 门店经营地址 | 门店维护入口 | 未见 Owner Vendor 消费 | 存在但未进入客户服务投影 |
| 门店品牌色 | `Store.color` | 品牌覆盖候选 | 门店维护入口 | 使用范围待核 | 存在但业务语义未定义 |
| 单据法定名称 | `DocumentProfile.legalName` | 重复主源候选 | `document/profile/save` | 报价/合同/凭证/文件名 | 与 Company.title 重复 |
| 单据信用代码 | `DocumentProfile.creditCode` | 重复主源候选 | `document/profile/save` | 单据渲染待核 | 与 Setting.uscc 重复 |
| 单据法人 | `DocumentProfile.legalRepresentative` | 单据资料 | `document/profile/save` | 当前渲染未使用 | 无完整消费者 |
| 单据地址 | `DocumentProfile.address` | 重复主源候选 | `document/profile/save` | 报价 PDF | 与 Company.addr 重复 |
| 单据热线 | `DocumentProfile.serviceHotline` | 联系通道 | `document/profile/save` | 合同/报价/凭证 | 与 Setting.serviceTel 重复 |
| 单据开户行/账号 | `DocumentProfile.bankName/bankAccount` | 敏感交易信息 | `document/profile/save` | 凭证/财务场景待核 | 受众和展示规则未冻结 |
| 单据 Logo | `DocumentProfile.logo` | 重复资产候选 | `document/profile/save` | 当前 PDF 渲染未使用 | 存在但未闭环 |
| 企业/门店地图点位 | 未发现经纬度字段 | 缺失 | 无 | 无 | GAP |
| 企业信息版本/快照 | 未发现独立品牌快照对象 | 缺失/部分 | 无 | 文档归档文件 | 仅有文件级归档，缺数据级版本 |

## 2. 当前表面覆盖

| 表面 | 当前事实 | 缺口 |
| --- | --- | --- |
| App 企业资料 | 可维护全称、简称、地址、服务区域、客服/售后、税号、资质、简介、品牌署名 | 未见 Logo、品牌色、地图点位维护 |
| Web 企业资料 | 可维护全称、简称、地址、对外服务字段和署名 | 未见 Logo、品牌色、地图点位维护 |
| App 业主首页 | 显示 `providerName` 和平台 `JzBrandLogo` | 未使用 `Company.logo`/Vendor.logo，装企品牌识别不完整 |
| 业主分享页 | 显示企业名称、简介、服务区域、客服、存证署名 | 未见装企 Logo、地址、地图、资质 |
| 邀请卡片 | 使用 `invite.company.logo` 作为分享图片 | 仅单点使用，未形成统一投影 |
| 报价 PDF | 使用 `legalName`、地址、热线 | 未使用 Logo、税号、法人、资质 |
| 合同 PDF | 使用 `legalName`、热线 | 未使用地址、Logo、税号、法人；主体信息不完整 |
| 凭证 PDF | 使用 `legalName`、热线 | 未使用地址、Logo、开户信息按模板状态不统一 |
| 文档归档/重打 | 文件级归档，重打读取旧文件 | 数据级品牌快照和来源版本缺失 |
| Excel/导出 | 报价工作簿、材料作业单等有导出 | 未见统一装企品牌抬头/Logo/地址规范，需专项对账 |

## 3. P0 级缺口

- `BRAND-GAP-01`：`Company` 与 `DocumentProfile` 双主源/重复字段。
- `BRAND-GAP-02`：装企 Logo 前端维护入口缺失，且业主首页用平台 Logo 替代企业 Logo。
- `BRAND-GAP-03`：PDF 渲染未统一使用企业 Logo、地址和法定身份。
- `BRAND-GAP-04`：地址语义混淆，无结构化地址和地图点位。
- `BRAND-GAP-05`：品牌主档变更后，缺乏数据级历史快照边界。

## 4. P1 级缺口

- `BRAND-GAP-06`：`Store.addr` 未进入客户服务方投影，多门店客户不知道服务门店位置。
- `BRAND-GAP-07`：税号、法人、开户行/账号的受众和脱敏规则不清。
- `BRAND-GAP-08`：资质只有自由文本，无文件、有效期、核验状态。
- `BRAND-GAP-09`：品牌色和门店色可维护但使用范围不清。
- `BRAND-GAP-10`：导出/打印/分享的字段白名单未按文件类型定义。
- `BRAND-GAP-11`：企业信息完整度缺少统一状态和缺失恢复路径。

## 5. 不能误判的事项

- “字段存在”不等于“可作为对外事实”；
- “PDF 能生成”不等于“企业品牌投影完整”；
- “文件已归档”不等于“数据级快照已建立”；
- “平台 Logo 正常显示”不等于“装企品牌正常显示”；
- “门店有地址”不等于“客户能看到、能导航、能识别服务主体”；
- “企业信息可编辑”不等于“内部、业主、合同、导出消费了同一事实”。
