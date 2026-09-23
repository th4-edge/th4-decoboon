# 单据模板与报价 PDF 归档开发交接

- request_id：`REQ-20260920-DEV-DOCUMENT-QUOTE-PDF`
- lane：`development`
- 状态：`implementation_done_self_tested_pending_independent_acceptance`
- 输入：`APP-EXP-001～006`、历史导入到 PDF 归档实施路线 P0/P1、合同打印基线 v1
- 边界：只实现平台基线、企业模板继承/复制/受控配置、企业抬头资料、报价客户版 PDF、私有归档和重打复制；不做电子签、材料/财务/合同 PDF 渲染或生产迁移。

## 1. 业务场景

### 场景 A：设计师生成客户版报价打印稿

1. 设计师完成报价后从 App 报价详情点击“报价打印稿”。
2. 服务端按当前员工数据范围校验报价，读取企业模板或平台行业基线。
3. 客户版 PDF 只输出销售价、项目名称、品牌/规格、面积和合计，固定不读取成本、利润或内部结算字段。
4. 文件写入企业私有对象路径，记录模板版本、SHA-256、生成人、业务编号和首次打印轨迹，并返回短期签名地址。

### 场景 B：客户原件丢失需要重打

1. 员工填写重打原因，服务端只允许当前数据范围内的原归档记录进入重打。
2. 系统从私有存储读取原 PDF 字节，复制到新对象键，不覆盖原归档。
3. 新归档记录引用原记录，复用内容摘要并新增打印轨迹，保留重打人员、时间和原因。

## 2. 服务端接口

| route | 能力 |
| --- | --- |
| `document/template/page` | 返回企业模板；未配置时返回平台行业基线 |
| `document/template/save` | 从基线复制或更新企业模板；字段键和样式范围受控，版本递增 |
| `document/profile/info` | 返回企业单据抬头资料；未维护时从企业名称、地址和 Logo 形成默认值 |
| `document/profile/save` | 仅 BOSS 维护法定名称、信用代码、法人、地址、热线、开户行和账号 |
| `document/quote/build` | 校验报价数据范围，渲染客户版 PDF，写入私有存储并建立归档/打印轨迹 |
| `document/record/page` | 按报价读取归档记录并签发短期访问地址 |
| `document/reprint/save` | 复制原 PDF 到新对象键，建立重打归档和审计轨迹 |

## 3. 受控模板配置

- 字段配置只接受平台冻结键；未知键返回 400。
- 样式只接受 `primaryColor`、`fontSize`、`rowSpacing`；颜色必须为六位十六进制，字号 9–14，行距 0–6。
- 企业模板仅记录企业副本；平台基线不写入租户表。
- PDF 归档保存模板版本和文件摘要，模板后续更新不回写历史文件。

## 4. 变更文件

- 后端：`src/main/java/com/th4/edge/decoboon/document/**`
- 后端公共文件：`src/main/java/com/th4/edge/decoboon/common/file/configure/FileScope.java`
- 项目仓储：`src/main/java/com/th4/edge/decoboon/project/repository/ProjectRepository.java`
- 测试：`src/test/java/com/th4/edge/decoboon/document/**`、`ProviderSecurityAcceptanceTest.java`
- App：`src/domains/document/**`、`src/subpackages/quote/pages/detail.vue`、`package.json`、`scripts/verify-document-contract.mjs`
- 迁移：`docs/design/migrations/41-单据模板与PDF归档.sql`（尚未执行）

## 5. 验证证据

- `mvn -o -q test`：57 suites / 173 tests / 0 failures / 0 errors。
- 新增测试覆盖：平台基线继承、受控字段/样式、客户版 PDF 不读取 `getCost()`、私有归档首次打印、重打复制不覆盖原对象、路由必须登录。
- App：`npm run verify:mp-weixin` PASS；主包 518411 B，总包 1586692 B。

## 6. 风险与待验收

- 迁移 41 未在生产或目标 MySQL 执行；只通过 H2 JPA 建表验证实体映射。
- OpenPDF 使用 `STSong-Light` 非嵌入中文字体；需要真机/微信开发者工具验证 PDF 打开和中文渲染。
- 尚未完成真实 OSS 上传、签名过期、跨企业归档访问和 App 下载分享联调。
- 本批不宣称独立验收通过。

## 7. 建议验收步骤

1. 在隔离库执行迁移 41，准备 BOSS、设计师、跨企业账号及一份带成本和客户金额的报价。
2. BOSS 调用模板页确认返回 5 类平台基线，保存一版报价模板并确认未知字段/非法样式被拒绝。
3. 设计师在 App 报价详情生成打印稿，打开 PDF 确认无成本列，归档路径、摘要、生成人和首次打印记录正确。
4. 在另一企业账号访问同一报价和归档，确认 404/403；无 token 确认 401。
5. 使用当前员工重打并填写原因，确认原文件不变、新记录引用原记录、打印序号和原因可追溯。
