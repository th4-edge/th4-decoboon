# 独立验收回执 · PI-20261022-002

- 验收日期：2026-10-22
- 验收类型：独立只读审查（未修改源码、配置、迁移或问题档案）
- 结论：**PASS**
- P0：0
- P1：0
- P2：1（非阻塞）

## 一、结论

TeamLeader 冻结后的存量角色与技术收口满足 `DEC-082` 与
`T12-BIZ-20261022-003`。允许进入 `DEV_VERIFIED` 与 `ACCEPTANCE` 完成态。

## 二、复核结果

| 验收目标 | 结果 |
| --- | --- |
| 禁止新邀请、接受邀请、改岗为 TeamLeader | PASS |
| 密码登录、微信登录、首次设密拒绝 TeamLeader | PASS |
| 旧令牌、session/info、业务 Provider 拒绝 TeamLeader | PASS |
| 订阅、续费、文件授权豁免路由不能绕过冻结 | PASS |
| App 旧缓存 TeamLeader 会话主动清理 | PASS |
| App/Web 不装载 TeamLeader 业务入口 | PASS |
| Web 邀请/筛选不提供 TeamLeader，历史仍解码为“团队负责人” | PASS |
| BOSS 可改历史 TeamLeader，非 BOSS 不可 | PASS |
| 改岗不改历史团队领导指针，不删除历史数据，无 DDL | PASS |
| 六类目标角色回归 | PASS |

## 三、独立验证

- 后端直接相关测试：42/42 PASS，0 failure / 0 error / 0 skipped。
- App：`npm run check:navigation` + `npm run type-check` PASS。
- Web：`npm run check:workbenches` + `npm run check:workspaces` + `npm run type-check` PASS。
- 研发补充：App `build:mp-weixin` + `check:subpackages` PASS；Web 隔离 production build PASS。

## 四、非阻塞 P2

App 弹窗标题为“账号状态已变更”，与说明中的“岗位状态”措辞不完全一致；正文已明确显示
“团队负责人角色已冻结，请联系企业负责人改为其他岗位”，不影响权限、安全和恢复路径。

## 五、剩余风险

- `file/private/download` 为公开签名下载路由；冻结前签发的短时链接在有效期内仍有效。
- 未在真实旧版 App 升级缓存、真实浏览器深链接和线上 TeamLeader 数据上执行升级回归。
- 当前生产 TeamLeader 为 0；发布后仍需复核计数未回增并保留发布前后对照证据。
