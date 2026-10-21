# 业主端产品化 UI V2 · 技术交互规范

- 版本：v2.0
- 日期：2026-10-21
- 状态：冻结，可作为研发输入
- 适用端：微信小程序
- 不涉及：Web 业主预览、后端业务状态机变更、正式版发布

## 1. 技术目标

1. 把当前高保真设计沉淀为可复用的 Owner UI 组件层。
2. 业主真实会话与员工只读预览复用同一页面组件树。
3. 五类业务详情页成为正式页面，不再是摘要卡片或临时弹层。
4. 不改变现有业务事实、权限和金额口径。
5. 为后续多机型、真机、体验版验收建立稳定技术边界。

## 2. 总体架构

```text
CustomerJourney API
  ├─ customer/project/journey/info       (Owner)
  └─ project/owner-preview/info          (EmployeeOwnerPreview)
                │
                ▼
        useOwnerJourney(projectId)
                │
                ▼
          OwnerPageShell
        ├─ PreviewChrome (only preview)
        ├─ OwnerPageHeader / Hero
        ├─ OwnerSection *
        ├─ OwnerFactGrid
        ├─ OwnerSignoffCard
        ├─ OwnerDocumentList
        ├─ OwnerEmptyState
        └─ OwnerTabBar / detail safe-bottom
```

### 2.1 单一页面原则

- 员工预览入口 `owner-preview` 只做权限校验与跳转，不维护业务 UI。
- 真实业主与员工预览都进入同一业主页面路由。
- 所有详情页只维护一份 `.vue` 文件。
- 禁止 `preview-owner-*.vue`、`employee-owner-*.vue` 等平行页面。

### 2.2 只读模式

统一只读状态：

```ts
const previewMode = computed(() => ownerPreview.active)
const readOnly = computed(() => previewMode.value || !!journey.value?.previewMeta?.readOnly)
```

只允许 `OwnerPageShell` 渲染预览横幅和水印，其他业务组件不得感知“员工/业主”差异，只能读取业务数据。

例外：需要隐藏写动作时，必须由共享能力判断封装：

```ts
const canWrite = computed(() => !readOnly.value && actionAllowed(key))
```

禁止在页面模板中散落 `if (preview)` 来改变业务卡片样式。

## 3. 路由与参数契约

### 3.1 主路由

| 语义 | 路由 |
| --- | --- |
| 首页 | `pages/index/index` |
| 进度 | `pages/project/list` |
| 历程 | `subpackages/project/pages/journey` |
| 我的钱 | `subpackages/project/pages/owner-payment` |
| 家档案 | `subpackages/project/pages/owner-archive` |
| 预览入口 | `subpackages/project/pages/owner-preview` |

### 3.2 详情路由

| 页面 | 路由 |
| --- | --- |
| 报价 | `subpackages/project/pages/owner-quote` |
| 合同 | `subpackages/project/pages/owner-contract` |
| 选材 | `subpackages/project/pages/owner-selection` |
| 材料计划 | `subpackages/project/pages/owner-material-plan` |
| 增减项 | `subpackages/project/pages/owner-change` |
| 验收 | `subpackages/project/pages/owner-acceptance` |

### 3.3 Query 契约

```text
projectId=<id>             必填
preview=1                  仅员工预览
```

规则：

- `preview=1` 必须配合 `projectId`。
- 页面 `onLoad` 必须先读取并验证 query，再请求数据。
- 无 `projectId` 进入可解释错误态，不发起空请求。
- 详情页返回时保留 `preview` 和 `projectId`，不得回落到员工业务页。

## 4. 数据契约

### 4.1 数据来源

| 模式 | API |
| --- | --- |
| 业主 | `customer/project/journey/info` |
| 员工预览 | `project/owner-preview/info` |

两者必须返回同一 `CustomerJourney` 结构：

```ts
interface CustomerJourney {
  projectId
  projectNo?
  projectName
  projectStatus?
  projectStatusName?
  progress
  currentStageName?
  propertyAddress?
  asOf?
  sections
  actions
  openFeedbackCount
  pendingActionCount
  vendor?
  team?
  previewMeta? // 仅员工预览
}
```

唯一允许的业务投影差异：

- `actions=[]`
- `previewMeta`
- 预览不返回 feedback 分区（既有 P0 边界）

### 4.2 详情事实 keys

UI 不直接访问内部实体。后端通过 `facts` / `items` / `lines` / `media` 投影以下事实：

| 详情 | 关键事实 |
| --- | --- |
| 报价 | `version`、`amount`、`subtotal`、`tax`、`acceptedAt`、`acceptedBy`、`registrationSource` |
| 合同 | `signedAmount`、`currentAmount`、`signedAt`、`effectiveAt`、`registeredBy` |
| 选材 | `selectionVersion`、`selectionStatus`、`designerName`、`confirmedAt`、结构化选材字段 |
| 材料计划 | `planStatus`、`quantity`、`brand`、`spec`、`requiredAt`、`arrivedAt` |
| 增减项 | `pendingNotice`、`approvedAt`、`requestedBy`、`effectiveAmount`、明细行 |
| 验收 | `acceptanceStatus`、`confirmedAt`、整改责任与截止日期 |

没有事实时：

- 不渲染伪时间。
- 不渲染伪附件。
- 显示“待同步”或“尚未形成”。
- `已签认/已生效` 只能来自后端事实，不得前端根据页面类型推断。

## 5. 组件 API

### 5.1 `OwnerPageShell`

```ts
props: {
  pageClass?: string
  bottom?: 'none' | 'safe' | 'tabbar'
}
slots: default, fixed
```

职责：

- 统一 owner 背景色、左右安全边距、底部留白。
- 预览模式时渲染 `OwnerPreviewBanner` 和 `OwnerPreviewWatermark`。
- 不负责业务事实。

### 5.2 `OwnerFactHero`

```ts
props: {
  variant?: 'project' | 'document'
  eyebrow?: string
  title: string
  subtitle?: string
  address?: string
  progress?: number
  stageName?: string
  statusName?: string
  amount?: number | string
  code?: string
  meta?: string
  showProgress?: boolean
}
```

- `project`：首页/进度/档案项目层。
- `document`：报价/合同/选材/材料/增减项详情。
- `document` 不使用深绿渐变大卡，改为暖纸色单据 Hero。

### 5.3 `OwnerSection`

```ts
props: {
  title?: string
  subtitle?: string
  actionText?: string
  density?: 'normal' | 'compact'
  tone?: 'default' | 'muted' | 'warning'
}
emits: action
```

所有详情页的正文区必须使用它，不允许页面重新发明卡片标题样式。

### 5.4 `OwnerSignoffCard`

```ts
props: {
  tone: 'offline' | 'record' | 'approval' | 'owner'
  title: string
  description: string
  operator?: string
  time?: string
  source?: string
}
```

语义映射：

| tone | 文案 |
| --- | --- |
| offline | 客户线下确认，员工登记 |
| record | 记录型事实，不需要业主确认 |
| approval | 企业审批生效，业主端只读同频 |
| owner | 需要业主本人确认 |

## 6. 交互时序

### 6.1 打开详情

```text
用户点击卡片
→ 生成携带 projectId / preview 的详情 URL
→ 详情页 onLoad 校验登录和 query
→ 设置页面加载态
→ useOwnerJourney.load(projectId)
→ 渲染 Hero、事实、明细、空态
→ 页面顶部统一安全区
```

### 6.2 返回

- 详情页返回必须保留来源页上下文。
- 预览模式返回项目详情或业主预览首页，不可返回员工普通详情后串身份。
- 从主 Tab 使用 `redirectTo`，避免 Tab 栈无限增长。
- 详情页使用 `navigateTo` + 系统返回。

### 6.3 展开/收起

- 报价空间分组使用页面内展开状态。
- 版本切换保持当前页面滚动位置。
- 空态和错误态不出现可点击的业务动作。

### 6.4 附件查看

- 附件只允许来自后端投影。
- 图片使用预览器；PDF/文档使用统一文档入口。
- 没有真实附件时显示“材料待同步”，不显示假按钮。

## 7. 动作权限矩阵

| 动作 | 业主真实 | 员工预览 | 实现 |
| --- | --- | --- | --- |
| 查看业务事实 | 允许 | 允许 | 同一组件 |
| 展开报价/变更明细 | 允许 | 允许 | 同一组件 |
| 查看图片/全景 | 允许 | 允许 | 同源只读 |
| 阶段验收确认/驳回 | 允许 | 禁止 | `readOnly` 拦截 |
| 分享 | 允许 | 禁止 | `hideShareMenu` + 不渲染入口 |
| 提交意见/上传 | 允许 | 禁止 | `readOnly` 拦截 |
| 导出/复制 | 不提供 | 不提供 | 不新增能力 |
| 返回员工项目 | 不显示 | 显示 | `OwnerPageShell` |

动作口径：

- `actions/pendingActionCount` 只表示业主必须处理的真实确认事项，例如待确认阶段验收；不得把“提出意见或投诉”“查看记录”等常驻服务能力算作待办。
- 意见与投诉是独立服务入口，不占用首页“此刻先看这一件事”的焦点卡；业主首页和装修历程可提供只读记录与提交入口。
- 按 `T12-BIZ-20261021-001`，员工预览 P0 不展示业主反馈详情，也不下发业主写动作；该隐私边界属于已裁决差异，不用空数据伪装。

## 8. 状态与错误

| 场景 | UI |
| --- | --- |
| 请求中 | 页面骨架 + 预览横幅保持 |
| 无项目 | 错误态，返回项目 |
| 404/403 | 不暴露对象是否存在，统一“暂不可确认” |
| 空数据 | 具体业务说明和形成条件 |
| 部分字段空 | 已同步字段正常展示，缺失字段逐项说明 |
| 网络失败 | 保留上次成功内容，顶部提示 + 重试 |
| 会话过期 | 重新登录，不丢失 preview 参数 |
| 预览权限撤销 | 立即退出预览，不保留业主详情缓存 |

## 9. 性能与安全

- 一次详情页请求复用同一 `CustomerJourney`，不在页面内逐项查库。
- 详情页只展示已投影字段，不在前端拼内部业务查询。
- `preview` 只影响安全层和动作能力，不影响业务数据组装结构。
- 预览不得写业主已读、不得创建 CustomerAccess、不得写业务状态。
- 预览审计沿用现有 `OwnerPreviewOpened` 最小审计。
- 不记录滚动、点击、停留和页面行为轨迹。

## 10. 代码边界

必须修改或新增：

```text
src/uni.scss
src/styles/tokens.ts
src/components/owner/OwnerPageShell.vue
src/components/owner/OwnerFactHero.vue
src/components/owner/OwnerSection.vue
src/components/owner/OwnerFactGrid.vue
src/components/owner/OwnerSignoffCard.vue
src/components/owner/OwnerDocumentList.vue
src/components/owner/OwnerTabBar.vue
src/components/owner/OwnerFactList.vue
src/components/owner/OwnerPriceGroups.vue
src/subpackages/project/pages/owner-quote.vue
src/subpackages/project/pages/owner-contract.vue
src/subpackages/project/pages/owner-selection.vue
src/subpackages/project/pages/owner-material-plan.vue
src/subpackages/project/pages/owner-change.vue
src/subpackages/project/pages/owner-archive.vue
src/config/index.ts
src/pages.json
```

不修改：

- 后端业务状态机。
- 金额、成本、权限计算公式。
- 业主微信授权链路。
- 正式版提审发布流程。

## 11. 研发门禁

### Gate A：结构

- 业主/预览同一路由同一页面。
- 无预览专用业务组件。
- `previewMeta` 只控制安全层与动作。

### Gate B：数据

- 五类详情页均有真实投影。
- 无记录的字段显示“待同步/尚未形成”。
- 增减项审批中不显示金额。
- 材料到场不出现确认按钮。

### Gate C：交互

- 返回、刷新、展开、图片预览、错误重试可用。
- 预览模式不下发写动作。
- 详情深链携带 `projectId` 与 `preview`。

### Gate D：渲染

- 390×844、375×812、430×932 渲染。
- 无横向溢出、无导航遮挡、无文字截断。
- 业主与预览截图并排对比，除安全层外一致。

### Gate E：验收

- `npm run type-check`
- `npm run check:ui`
- `npm run check:owner-preview`
- `npm run verify:mp-weixin`
- 微信开发者工具真实员工预览和深链回归
- 真实业主账号或用户确认的只读证据
