# AI 设计生成功能 · 微信开发者工具验证记录

日期：2026-09-26  
验证人：Codex（Development Lane）  
状态：UI 入口 PASS；真实出图 BLOCKED

## 1. 验证环境

- 微信开发者工具项目：`/private/tmp/th4-decoboon-app-ai/dist/dev/mp-weixin`
- App 分支：`feature/design-ai-generation-app`
- 后端工作区：`/private/tmp/th4-decoboon-ai-design`
- 本地后端：`http://192.168.0.104:8090/api/`
- 测试企业：麦鸿装饰
- 测试角色：设计师 董夏鹏（`19900000005`）
- 证据目录：`docs/acceptance/20260926-ai-generation-devtools/raw/`

## 2. 已验证的真实页面路径

### PASS：设计师设计工作台

- 页面：`subpackages/design/pages/workbench`
- 真实页面显示 11 位设计客户。
- 已有方案的客户卡片显示 `AI 生成`。
- 证据：
  - `raw/screenshots/S01-design-workbench.png`
  - `raw/accessibility/S01-design-workbench.json`

### PASS：方案列表 AI 聚焦

从设计工作台点击 `AI 生成` 后进入：

- 页面：`subpackages/design/pages/list`
- 参数包含：`customerId`、`propertyId`、`focusDesign=1`
- 页面显示提示：`请选择要生成效果图的设计方案`
- 方案卡片显示 `AI 生成`。
- 证据：
  - `raw/screenshots/S02-design-list-ai-focus.png`
  - `raw/accessibility/S02-design-list-ai-focus.json`

### PASS：AI 效果图生成页

从方案卡片的 `AI 生成` 进入：

- 页面：`subpackages/design/pages/ai-generation`
- 页面标题：`AI 效果图生成`
- 显示目标方案、面积、户型、返回方案、刷新。
- 当前方案缺少 CAD/户型图，正确显示门禁：
  `缺少CAD或户型图片，请先返回方案页上传CAD图片或户型图，再发起AI生成。`
- 证据：
  - `raw/screenshots/S03-ai-generation-page.png`
  - `raw/accessibility/S03-ai-generation-page.json`

### PASS：方案详情 AI 效果图入口

- 页面：`subpackages/design/pages/scheme`
- 方案资料底部显示：
  - `CAD 图纸`：`点击上传 CAD 图纸`
  - `AI 效果图`
- 从方案详情点击 `AI 效果图` 后真实进入 `subpackages/design/pages/ai-generation`。
- 证据：
  - `raw/screenshots/S04-scheme-detail-base.png`
  - `raw/accessibility/S04-scheme-detail-base.json`
  - `raw/screenshots/S05-ai-generation-from-scheme.png`
  - `raw/accessibility/S05-ai-generation-from-scheme.json`

## 3. 为什么当前正常 App 看不到 AI 元素

正常 App 工作区当前仍在 `dev` 分支，AI 页面和入口代码位于独立分支：

- App：`feature/design-ai-generation-app`
- 后端：`feature/design-ai-context-prompt`

因此用户打开正常 App 构建时，设计工作台、方案列表和方案详情没有 AI 入口，属于构建分支不一致，不是页面局部渲染丢失。

## 4. 真实出图阻塞项

当前本地环境无法完成点击 `开始生成` 后的真实出图，原因有两个：

1. 当前方案没有 CAD 或户型图，AI 生成页按设计门禁阻断。
2. 本地正在运行的后端没有 AI 生成路由：
   - 调用 `design/generation/page`
   - 返回 `canNotFoundHandler`
   - 提示：`找不到对应的处理服务:design/generation/page`
3. 本地数据库未发现 AI 生成任务/版本相关表，说明迁移 63 尚未在该本地数据库执行。

因此本轮不能声明“效果图真实生成 PASS”。

## 5. 使用顺序（功能分支部署后）

1. 设计师登录 App。
2. 进入底部 `设计` 页签，打开 `设计工作台`。
3. 选择已有方案的客户，点击 `AI 生成`。
4. 在方案列表点击目标方案卡片上的 `AI 生成`。
5. 若提示缺少 CAD/户型图，返回方案详情。
6. 在方案详情 `方案资料` 底部点击 `CAD 图纸`，从微信选择并上传 CAD/户型图。
7. 等待保存和 CAD 预览转换。
8. 点击 `AI 效果图` 回到生成页。
9. 选择 `精准共创` 或 `概念探索`，点击 `开始生成`。
10. 生成过程中页面自动刷新；完成后查看效果图版本、Prompt，并由设计师确认版本回写封面/区域。

## 6. 下一步研发动作

- 将后端 AI 功能分支切换到本地运行实例并执行迁移 63 的隔离验证。
- 更新 `MIGRATION_END=63` 并完成 Provider 路由基线收口。
- 合并/进入可验证构建后，用带 CAD/户型图的测试方案完成一次真实四图生成。
- 完成后再进行独立验收，当前功能不能对外表述为已上线。
