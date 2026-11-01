# 装企信息与品牌投影 · 实现记录

## 本批范围

- Batch：`BRAND-D1`
- 垂直切片：Company Logo/品牌色维护、App/Web 企业信息可读性、小程序品牌表面投影
- 不包含：合同/PDF/Excel/打印、地址/地图、数据级历史快照、门店品牌覆盖

## 变更

- 后端：`CompanyBrandContext` 保存受约束图片；`company/company/update` 接收 `logoFile`；Logo 对象键加入受约束匿名读取白名单。
- App：企业信息页分组重做；Logo 上传/移除、品牌色预设、字段示例与影响说明；业主邀请、业主首页、分享页消费企业品牌。
- Web：企业信息页增加品牌预览、Logo 上传/移除、品牌色选择和完整字段帮助。
- 迁移：无。
- 配置：无新增密钥；Web 开发代理补充 `/public`。

## 测试与证据

- 定向测试：`CompanyBrandContextTest`、`AvatarDownloadProviderTest` PASS。
- 构建/类型检查：App/Web type-check 与 build PASS；App 首页/注册/邀请/导航/品牌/UI 契约 PASS；Web 表单/概念/状态/动作/指标/权限/页面/工作台检查 PASS。
- 真实页面/真机：Web 1440 实际渲染，Logo 图片 naturalWidth=256、页面横向溢出 0；微信开发者工具企业信息页回读品牌状态。
- 部署/重启：本地 JDK22 后端 8090、Web 8081、App dev watch/微信开发者工具运行。
- 截图/日志：`docs/acceptance/runs/20261029-company-brand-acceptance/`。

## 风险与未闭环

- 合同/PDF/历史文件仍不使用本次品牌主档，不能对外宣称全场景闭环。
- 地址/地图、门店覆盖、资质附件与历史快照仍待业务裁决。
