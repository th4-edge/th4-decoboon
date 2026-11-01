# 报价分项与套餐复制 · 开发自测

- 状态：`DEV_VERIFIED`，不是独立验收。
- App：`feature/package-quote-copy-20261101` / `77e35f9`
- 后端：`feature/package-quote-copy-20261101` / `3e585db6`
- 环境：微信开发者工具 iPhone 16；本地 8094；昭青科技业务闭环审计企业；Boss `13910002001`
- 数据：本地审计报价 `242598657067581440`；源套餐 `242604641710243840`

## 验证结论

1. 报价调整页每条有效报价行显示“复制”。
2. Boss 复制报价分项后，服务端创建一条 `Add / DesignOnly / Confirmed` 新行，报价行 4→5，金额重新计算。
3. 已扣减或未包含的报价行不显示复制入口，后端也拒绝复制。
4. 套餐管理页显示“复制套餐”；复制后生成新编码、V1、Draft、current=false 的独立套餐，名称带“（副本）”。
5. 新套餐完整复制源套餐 4 个项目行；原套餐和历史报价不变。
6. Designer 调用套餐复制返回 403。
7. 测试报价行已删除；测试套餐副本已清理；源套餐与源报价恢复。

## 证据

- `evidence/api.json`：API 正向与权限负向。
- `evidence/ui.json`：微信页面复制闭环。
- `01-quote-copy-action.png`：报价分项复制入口。
- `02-quote-copy-result.png`：复制后的新报价行。
- `03-package-copy-action.png`：套餐复制入口。
- `04-package-copy-result.png`：复制后进入独立草稿。
