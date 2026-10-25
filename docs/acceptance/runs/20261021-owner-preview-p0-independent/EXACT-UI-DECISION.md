# 员工预览与业主端 UI 完全一致 · 实施记录

> **SUPERSEDED（2026-10-21）**：本记录把 `journey.vue` 二级页误当成业主主界面。
> 真实业主主界面为四 Tab：`我的家 / 进度 / 动态 / 我的`。
> 本文件不再作为“UI 完全一致”的验收依据，替代证据见
> `docs/acceptance/runs/20261021-owner-preview-four-tabs-independent/`。

日期：2026-10-21
来源：用户最新指令“改为员工看到的 UI 和业主端 UI 完全一致”

## 实现方式

- `owner-preview.vue` 不再维护独立三 Tab 摘要页，只作为权限校验和跳转入口
- 跳转到同一个业主端页面：
  `subpackages/project/pages/journey?preview=1`
- `journey.vue` 同时服务：
  - 真实业主：`customer/project/journey/info`
  - 员工预览：`project/owner-preview/info`
- 两端复用同一套：
  - 项目 Hero
  - 完整纵向业务分区
  - 图片查看
  - 服务团队
  - 空态、刷新和错误态

## 预览安全层

员工预览仅增加此前已确认的只读安全层：

- 固定只读横幅
- 固定水印
- 底部“业主操作需业主本人在业主端完成”
- “返回员工项目”
- 关闭微信分享菜单
- 业主动作列表不渲染
- 分享卡片不渲染
- feedback 分区继续按 P0 业务边界不返回

## 结论

- 页面结构、内容布局和业务事实展示：**已复用同一 `journey.vue`，UI 一致**
- 业主专属动作、分享和 feedback：仍按既有 P0 决策关闭
- 若要求连业主确认、分享、反馈等动作入口也完全一致，将改变既有业务权限与数据边界，需另行走业务裁决

## 验证

- `exact-ui-app-results.json`：15/15 PASS
- 入口最终路径：`subpackages/project/pages/journey`
- 未出现原三 Tab 预览 UI
- 横幅、水印、团队区块正常
- 无业主动作列表、无分享卡片
- 返回员工项目 PASS

截图：`migration/55-exact-ui-owner-page-in-preview.png`
App verify：`migration/58-exact-ui-app-verify.log`
