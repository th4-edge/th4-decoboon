# 企业运营标准资产 · UI Review

## EOP-V1

- 状态：`NOT_APPLICABLE`
- 原因：仅新增后端只读契约与 Provider。

## EOP-V2

- 状态：`PENDING_REAL_RENDER`
- 已实现：App 套餐管理状态/版本展示，发布、创建下一版、停用；报价选择器只读取当前发布套餐。
- 已通过：App `type-check`、微信小程序构建。
- 未通过/未执行：微信开发者工具真实页面操作、真机、Web 套餐管理页。
- 禁止宣称：App UI 已验收、Web 双端完整。
- 后续：EOP-V6 补齐 Web 页面并执行真实渲染、交互和双端一致性验收。

## EOP-V3

- 状态：`PENDING_REAL_RENDER`
- 已实现：App 付款/施工计划状态/版本、创建下一版、显式平台升级、启用/停用/默认动作。
- 已通过：App type-check、微信小程序构建。
- 未执行：微信开发者工具真实页面、真机、Web 管理页。
- 后续统一由 EOP-V6 完成真实渲染和双端验收。

## EOP-V4

- 状态：`PENDING_REAL_RENDER`
- 已实现：App/Web 报价项目显示 `R{n}`；App 维护页与 Web 详情提供变更历史、加载失败重试和非成本角色裁剪提示；App 保存/启停/删除与 Web 保存携带期望 revision。
- 已通过：App/Web type-check 与生产构建、App/Web 客户端文案门禁。
- 未执行：微信开发者工具真实页面、真机、Web 多分辨率渲染、并发冲突后的实际交互恢复。
- 后续统一由 EOP-V6 完成真实渲染和双端一致性验收；当前不得宣称 UI 已通过。
