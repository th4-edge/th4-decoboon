# 璟阳里5-804 报价打印失败分析

## 结论

报价 PDF 实际已经成功生成并归档。失败发生在接口响应序列化阶段：打印归档 Provider 把完整 `Quote` 关联对象放入响应白名单，导致报价继续展开客户、房屋、企业、门店、员工、设计方案和 68 条报价行，最终生成约 19MB 的递归 JSON，App 无法正常解析。

## 复现证据

- 报价：`Q1789927519108` / `227490321095720960`
- 原接口：`document/quote/build`
- 原接口已成功创建归档：`227491937140080640`
- PDF 对象大小：`93423` 字节
- 原响应大小：约 `19MB`
- 原响应 JSON 解析失败位置：约 `18,498,310` 字符
- 原响应结尾出现大量递归对象闭合括号，不是合法 JSON

## 根因

打印相关 Provider 的 `@Properties` 包含：

`quote / project / reprintOf / generatedBy`

但没有为这些关联对象声明嵌套白名单。框架序列化时继续遍历完整对象图，产生响应膨胀和递归结构。

## 修复

将以下三个 Provider 的响应收口为 App 真正使用的打印归档字段：

- `document/quote/build`
- `document/reprint/save`
- `document/record/page`

现在只返回：

`documentId / type / audience / businessNo / templateVersion / path / sha256 / fileName / contentType / size / generatedAt / status / url / urlExpiresAt`

不再返回报价、项目、重打原记录和生成人完整关联。

## 验证

- 修复后 `document/quote/build` 响应：`1140` 字节。
- 业务字段完整，`documentId`、`fileName`、`size`、`url` 正常。
- 短期地址下载结果：HTTP 200、`application/pdf`、`93422` 字节。
- PDF 文件：合法 PDF 1.5，3 页。
- `document/record/page` 响应：`4493` 字节，JSON 正常。
- 新增 `DocumentResponseContractTest`，防止 `quote/project/reprintOf/generatedBy` 再次进入打印响应。
