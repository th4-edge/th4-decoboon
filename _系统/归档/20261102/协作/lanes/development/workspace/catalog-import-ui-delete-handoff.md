# 交接：REQ-BIZ-20260926-001-W1 / W2 报价项目库维护可达性 + 项目删除

- request_id: `REQ-BIZ-20260926-001-W1`（UI 可达性）、`REQ-BIZ-20260926-001-W2`（交互优化 + 删除）
- lane: development
- 结论：实现完成并自测；未申报验收通过

## 1. changed_files

后端：`catalog/service/items/delete/{Delete.java,support/Delete.java}`（新增）、`catalog/repository/CatalogImportRowRepository.java`（新增 `findByItem_ItemId`）、`src/test/.../ProviderSecurityAcceptanceTest.java`（路由计数机械同步 340→341 / 328→329）。
App：`src/components/CatalogPicker.vue`（manage 工具栏插槽 + 卡片“更多”操作菜单 + delete 事件）、`src/subpackages/quote/pages/catalog/manage.vue`（导入入口移入插槽 + 删除二次确认与失败原因展示）、`src/domains/catalog/api.ts`（`remove(itemId)`）。
文档：本文件、worklog、`workspace/catalog-delete-verify/**`。

## 2. W1 UI 可达性修复

- 根因：`CatalogPicker` 根 `.cp` 为 `position:fixed; inset:0; z-index:120`，维护页的导入卡片是其兄弟节点，被全屏层覆盖；关闭选择器又会退出维护页。
- 修复：`CatalogPicker` 在固定层内、header 之后渲染 `.cp__manage-toolbar` + `<slot name="manage-toolbar"/>`；维护页把导入工具（标题 + 下载模板 + 批量导入 + 最近导入记录）作为插槽内容注入，结构上不再被覆盖，也不遮挡搜索/分类/列表（插槽位于类型/搜索/列表之上、header 之下，随 header 固定可见）。
- 编译产物证据（`dist/build/mp-weixin`）：
  - `components/CatalogPicker.wxml`：`cp__manage-toolbar ... > <slot name="manage-toolbar"/> </view> <scroll-view class="cp__types" ...>`（插槽在 `.cp` 固定层内，位于类型筛选之前）。
  - `subpackages/quote/pages/catalog/manage.wxml`：`<catalog-picker ... u-s="{{['manage-toolbar']}}" ...>` 内嵌 `<view class="cm__import" slot="manage-toolbar">`，含 `批量导入报价项目库` 标题与两个按钮；页面根节点已无独立 `cm__import` 兄弟卡片。

## 3. W2 交互优化与删除

- 卡片层级：卡片主体点击 = 编辑；底部「更多」= `uni.showActionSheet`（启用/停用、删除项目）；移除冗余铅笔按钮；保留新建/编辑/启停/搜索/分类筛选/批量导入。
- 删除确认：`uni.showModal`（标题“删除报价项目”，确认按钮“删除”）后才调用后端；成功 `toast('项目已删除')` 并 `pickerRef.refresh()`；失败展示后端业务原因原文（含“可改为停用”指引），不使用“网络异常”兜底。
- 后端 `catalog/item/delete`：`Security` + `CatalogContext.requireMaintainer`（不分角色放宽）；`findByItemIdAndCompany_ComId` 取数（跨企业/不存在 404）；引用检查 9 类；导入批次行受控解除；主项目 + 类型明细同事务删除。

## 4. provider_api_contract

| 路由 | 入参 | 返回 | 说明 |
| --- | --- | --- | --- |
| `catalog/item/delete` | `itemId` | `CatalogItem` | 无引用硬删除；引用存在返回 `400`（含引用方与“可改为停用”建议）；不存在/跨企业 `404`；重复删除稳定 `404` |

## 5. migration_changes

无（未新增列或表；导入批次行 `item_id` 置空复用既有可空外键）。

## 6. tests_and_results

- `mvn -o -q test`：109 suites / 365 tests / 0 failures / 0 errors / 0 skipped（含 `ProviderSecurityAcceptanceTest` 路由计数同步 340→341 / 328→329，待 acceptance 复核）。
- 删除能力真实 HTTP+DB 探针（`workspace/catalog-delete-verify/probe-output.txt`）：
  - D1/D2 无引用新建项目删除成功，`catalog_item` 与 `catalog_general_detail` 均为 0 行。
  - D3 重复删除 → `404 报价项目不存在或无权访问`（稳定失败）。
  - D4 被设计分项/报价引用的真实项目 → `400 该项目已被设计分项引用，无法删除；如不再使用，可改为停用`。
  - D5 不存在/跨企业项目 → `404` 统一拒绝。
  - D6 导入批次行关联项目删除成功：导入行 `item_id` 置空、`error_code=DELETED`、批次审计保留。
  - D7 撤销剩余批次清理，本地库恢复 550 条基线。

## 7. app_verification

- `npm run type-check` PASS、`npm run build:mp-weixin` PASS、`npm run check:subpackages` PASS（主包 729697 B、最大分包 540733 B、总包 2571475 B；14 个分包 / 110 页）。
- 未完成：微信开发者工具**自动化**视觉验证（`miniprogram-automator` 无法附着本机 IDE：IDE 未开启服务端口导致 `reLaunch` 超时；H5 登录仅微信手机号快捷登录，无法脚本注入 BOSS 会话）。已改用编译产物结构证据（见 §2）替代；真机/DevTools 视觉确认需 acceptance 或用户手动执行。

## 8. risks

- 「更多」使用原生 `uni.showActionSheet`，其样式由微信客户端控制；如需自定义样式需另做组件（本轮未做）。
- 删除为硬删除且仅按引用拦截：若未来新增其它引用表（如新的供应链对象）需同步加入 `CatalogImportReferenceContext` 引用清单。
- 导入批次行解除关联后，批次 `importedRows` 统计仍保留历史值，批次明细中该行标记 `DELETED`；如需列表口径区分需产品确认。
- 路由计数测试由 development lane 机械同步，需 acceptance 复核。

## 9. acceptance_needed

- 真机/DevTools 视觉复验：BOSS 进入维护页直接可见“批量导入报价项目库 / 下载 V1.0 模板”；卡片「更多」菜单、删除确认与删除后刷新；被引用项目删除失败提示。
- 复验删除边界：跨企业、重复删除、导入行关联解除、9 类引用逐一阻断。
- 复核 `ProviderSecurityAcceptanceTest` 路由计数同步。
