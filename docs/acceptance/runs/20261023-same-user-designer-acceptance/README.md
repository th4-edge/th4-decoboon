# 2026-10-23 18660219918 同用户 App/Web 验收环境

> 环境：本地 dev；未提交、未部署、未连接生产。  
> 状态：`READY_FOR_USER_ACCEPTANCE`

## 同一身份

- 手机号：`18660219918`
- 员工：张伟
- 角色：Designer
- 企业：安徽昭青网络科技
- 原有 `Wx` 凭证保持绑定，openid 未改写；本地新增可用密码与开发令牌作为验收入口。

## Web 同源令牌登录

- Web 在 DEV 环境接受 `?devToken=`，先写入现有 token 存储，再调用 `auth/session/info` 水合同一身份。
- 登录成功后立即用 `router.replace` 清除 URL 中的 token。
- 验收入口文件：`~/.decoboon-local/acceptance-18660219918-links.md`（权限 600）。
- 备份密码登录：`18660219918 / Accept2026!`。

## App 微信开发者工具

- 项目：`dist/dev/mp-weixin`
- 开发版 API：`http://192.168.0.104:8090/api/`
- `project.config.json` 的 `urlCheck=false` 仅用于本地开发项目。
- 已使用同一开发 token 进入 App，存储身份为：张伟 / designer / 安徽昭青网络科技。

## 业务场景

1. 设计与待谈单：客户张伟 · 傅山园；方案 `D1790383380417` 已就绪；报价 `Q1790383677036` 为 Draft。
2. 待签约：客户 `【验收·待签约】周先生`；房屋滨湖云谷3栋1608；报价 `Q-SIGN-DEMO-001`，已接受 ¥128,000，未发起合同。Web 与 App 均已确认可见。
3. 合同与项目：客户王亮；合同 `CC1790371757137` 已生效；项目 `P1790371757143`（王亮梦圆小区）施工中。

## 清理

- 分页临时报价和 Dormant 客户已全部清理，`19900000005` 凭证已恢复。
- 新增待签约场景清理脚本：`~/.decoboon-local/cleanup-18660219918-sign-scenario-20261023.sql`。
- 18660219918 凭证原哈希备份：`~/.decoboon-local/cred-1790215547737662591-before-20261023-same-user-acceptance.tsv`。
