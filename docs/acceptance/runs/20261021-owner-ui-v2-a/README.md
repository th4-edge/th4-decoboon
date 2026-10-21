# Owner UI V2-A · 统一视觉内核与业务详情落地

- 日期：2026-10-21
- 批次：`OWNER-UI-V2-A`
- 性质：开发自测 / 构建验证，不替代微信开发者工具视觉审查与独立验收
- 后端：定向测试 PASS
- App：type-check、Owner 契约、预览契约、导航、UI 规范、构建、分包 PASS

## 已落地

1. 新增 Owner 子品牌 Token：森林绿、纸张米色、铜色强调和统一字号/间距/圆角。
2. Owner 页面统一使用暖纸色画布，业务卡片统一米白表面。
3. `OwnerFactHero` 改为浅色单据 Hero，支持 `project` / `document`。
4. `OwnerTabBar` 改为五槽 Owner UI，中央装修历程凸起。
5. 新增 `OwnerSection`、`OwnerFactGrid`、`OwnerSignoffCard`。
6. 报价、合同、选材、增减项详情接入 Owner 签认/审批语义。
7. 新增独立材料计划页 `owner-material-plan`，明确材料到场无需业主确认。
8. 家档案增加材料计划入口，选材详情可跳转材料计划。
9. 后端 `CustomerJourney` 补充登记人、登记时间、要求到场、实际到场等只读事实投影；无 DDL。

## 同源边界

- 业主真实会话与员工预览继续复用同一页面与组件。
- 预览只增加固定横幅、水印、分享关闭、写动作禁用和返回项目。
- 未新增 `preview-owner-*` 或员工专用业务卡片。
- 增减项仍为企业审批生效后只读同频，不新增业主在线确认。
- 材料到场仍不设业主确认。

## 证据

- `00-type-check.log`
- `01-owner-ui-contract.log`
- `02-owner-preview-contract.log`
- `03-navigation.log`
- `04-build-mp-weixin.log`
- `05-subpackages.log`
- `06-backend-targeted-tests.log`
- `07-check-ui.log`

## 尚未完成

- 微信开发者工具真实渲染并截图。
- 业主真实账号与员工预览并排视觉比对。
- 375×812、390×844、430×932 三档视觉门禁。
- 真实空态、错误态、长内容与部分同步状态的页面走查。
- 独立验收与发布。
