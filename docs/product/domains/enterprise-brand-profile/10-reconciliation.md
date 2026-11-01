# 装企信息与品牌投影 · 设计—实现—页面—验收对账

状态：`DEV_VERIFIED / USER_ACCEPTANCE_PENDING`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BRAND-D1-01 | 企业基础与品牌信息维护 | BR-059、USER-DEC-20261029-003/004/005 | Company/Setting 单一主源，现有字段 | Company/Setting + `company/company/update` | App/Web 企业信息 | type-check、契约、构建 | 用户感受验收中 | DEV_VERIFIED |
| BRAND-D1-02 | Logo 小程序投影 | 真实 Logo 优先；无 Logo 用简称首字；品牌色默认平台 Token | Company.logo/color | 企业信息、员工邀请、业主首页、分享页 | 真实文件上传/匿名读取/页面回读 | 上传 200、GET 200、跨端代码契约 | 用户感受验收中 | DEV_VERIFIED |
| BRAND-D1-03 | 品牌色即应用主题色 | USER-DEC-20261029-006 | Company.color → 派生色阶 | App/小程序 page-meta、Web CSS Variables、Element Plus | 主题预览、全局换色、语义色隔离 | App/Web 真实换色数据 + 构建 | 用户感受验收中 | DEV_VERIFIED |
| BRAND-D1-04 | Web 登录前企业品牌恢复 | 仅缓存公开品牌与手机号；不保存密码/token | last-login 本地记录 → 登录页企业标识 | LoginView、CompanyBrandMark、BrandLogo | 登录、退出、再次打开 | 真实浏览器 localStorage/Logo/主题回读 | 用户感受验收中 | DEV_VERIFIED |
| BRAND-D0-03 | 地址与地图 | 地址语义待裁决 | 待 BIZ_ALIGNED | Company.addr、Store.addr、Property.address、Project.siteAddr | 企业资料、业主 | 静态核对 | 未验收 | GAP |
| BRAND-D0-04 | 单据抬头与历史快照 | BR-059 快照/投影规则 | 待 BIZ_ALIGNED | DocumentProfile、DocumentRecord | 报价/合同/凭证 | 静态核对 | 未验收 | GAP |
| BRAND-D0-05 | 实际业务与产品差异 | 不硬凑业务事实 | 待 BIZ_ALIGNED | 兼容台账候选 | 企业资料/入驻 | 未开始 | 未验收 | PENDING |

## 已确认的当前事实

- `Company` 保存企业全称、简称、Logo、品牌色、地址和 `Setting`。
- `Store` 有名称、品牌色和地址。
- `DocumentProfile` 保存法定名称、信用代码、法定代表人、地址、服务热线、开户行/账号和 Logo。
- `CustomerJourney.Vendor` 是业主可见的企业事实投影，来源于 `Company`/`Setting`。
- App 业主首页当前使用平台 `JzBrandLogo`，未使用企业 `Vendor.logo`。
- 报价/合同/凭证 PDF 当前未使用企业 Logo；合同和凭证只使用部分企业字段。
- 未发现 Company/Store 的经纬度或地图点位字段。
- 未发现独立品牌快照/版本对象；现有 `DocumentRecord` 提供文件级归档。

## 本批已映射

- 企业 Logo 文件通过既有 `company/company/update` 的 `logoFile` 字段上传并写回 `Company.logo`。
- 企业品牌色通过现有 `Company.color` 保存，仅提供通过白字对比度的预设。
- App 企业信息、员工邀请、业主首页与业主分享页统一使用 `CompanyBrandMark`。
- 无 Logo 使用企业简称首字；无自定义品牌色使用装得清品牌语义主色。
- 修改影响说明明确：同步当前小程序品牌表面，不改写已生成历史合同与文件。
- `Company.color` 同时作为 App/小程序/Web 的主题输入，派生浅色、深色、渐变、焦点与可读前景色。
- App 构建产物 189 个页面均注入 `page-meta` 主题变量；Web 运行时覆盖品牌 Token 与 Element Plus 主色。

## 未映射

- 法定主体与品牌名的正式展示层级。
- 单据抬头是主源还是快照。
- 地址类型、地图点位和导航规则。
- Logo 资产版本与历史单据冻结规则（D3/D4）。
- 各导出/打印表面的字段白名单。

## 已过期

- 无。

## 未决业务问题

- 待 `REQ-BIZ-20261029-001` T06/T12。

## 本批结论

- 已对齐：Logo/品牌色在现有小程序品牌表面形成维护—保存—回读—展示闭环。
- 已对齐：App/Web 企业信息页使用同一 Provider、同一字段和同一占位规则。
- 已验证：真实图片上传、受约束匿名读取、品牌色回读、Web 真实渲染与小程序页面回读。
- 未对齐：合同/PDF/Excel/打印、门店覆盖、数据级历史快照。
- 下一步：用户感受验收；通过后再启动 BRAND-D3 输出表面字段白名单与快照设计。
