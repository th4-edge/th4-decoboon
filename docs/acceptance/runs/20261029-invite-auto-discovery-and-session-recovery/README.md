# 邀请手机号展示、登录后邀请发现与会话恢复 · 开发验证证据

> 状态：`DEV_VERIFIED_PENDING_USER`  
> 风险级别：L2（后端邀请查询 + App 登录/会话恢复）  
> 变更记录：`CHG-20261029-022-invite-auto-discovery-and-session-recovery.md`

## 1. 问题

1. 员工邀请页“手机号”为空：后端公开邀请详情有意不返回完整 `telNo`，但前端直接读取 `invite.telNo`。
2. 用户完成手机号授权后没有进入装得清：
   - 后端已真实创建 Finance 员工并接受邀请；
   - App 本地会话仍可能停留在 `sessionType=user`、`role=null`；
   - App 启动时只在已有角色时才刷新身份，因此用户一直无法升级到 Finance；
   - 邀请成功后的首页跳转也只有 `reLaunch`，没有 tabBar 兜底。

## 2. 修复

### 2.1 邀请页手机号

- `Invite` 增加非持久化 `maskedTelNo`。
- `invite/invitation/info` 返回脱敏手机号，不返回完整号码。
- 邀请页显示 `maskedTelNo || telNo || 待确认`。

实测：

```text
maskedTelNo=186****9918
```

### 2.2 登录后主动发现邀请

新增 Provider：

```text
invite/invitation/pending
```

只使用当前登录凭证中**服务端已验证的手机号**查询：

- 待接受员工邀请；
- 待接受业主邀请；
- 完整手机号不返回，只返回脱敏号码。

App 行为：

```text
手机号授权 / 已有未加入会话
→ 查询 pending entry invites
→ 1 条：直接进入对应员工邀请页或业主入口
→ 多条：进入“待加入邀请”选择页
→ 0 条：进入未加入企业页
```

当同一手机号同时存在员工邀请和业主邀请时，不静默选择第一条，而是让用户显式选择。

### 2.3 接受后会话恢复

- App 启动时只要有 token 就刷新 `auth/session/info`，不再要求本地已有 role。
- 后端确认员工身份后立即升级本地会话。
- 如果当前仍停留在登录、未加入企业、员工邀请、邀请发现或业主入口页，自动进入企业首页。
- 邀请接受成功后的首页跳转增加 tabBar `switchTab` 兜底。

## 3. 当前真实数据状态

`18660219918` 实际已在 `2026-10-29 19:15:27` 完成邀请接受：

| 身份 | 手机号 | 角色 | 状态 | 门店 |
| --- | --- | --- | --- | --- |
| 张伟 | 19900009918 | Boss | Active | 总店 |
| 张伟 | 18660219918 | Finance | Active | 总店 |

员工邀请：

```text
inviteId=1793271807543993360
status=Accepted
```

这说明当时失败点是“后端加入成功，前端会话/落点未恢复”，不是邀请没有生效。

## 4. 验证

后端：

```text
PendingEntryInviteProviderTest PASS
  - 员工邀请优先返回
  - 业主邀请同时返回
  - 手机号脱敏
  - 已加入员工不再返回待加入邀请

invite/invitation/pending 路由注册并返回成功
invite/invitation/info 返回 maskedTelNo=186****9918
```

App：

```text
npm run type-check          PASS
npm run check:invite-flow   PASS
npm run check:registration  PASS
npm run check:client-copy   PASS
npm run check:navigation    PASS
npm run build:mp-weixin     PASS
npm run check:subpackages   PASS
```

证据：

- `evidence/invite-info-masked.json`
- `evidence/final-identity-state.json`

## 5. 当前可做验证

当前 186 已经是 Finance 员工，因此再次普通微信登录应直接进入 Finance 首页，不再经过邀请接受。

在微信开发者工具中：

1. 重新编译最新小程序；
2. 保留或重新完成微信登录；
3. App 启动会刷新当前 token 对应身份；
4. 应进入 Finance 工作台；
5. 若仍停留登录/未加入页面，重新打开小程序一次，新的启动刷新逻辑会恢复身份。

## 6. 后续邀请发现场景

要验证“用户先授权、系统自动发现新邀请”的成功链路，需要一个**尚未加入企业的测试手机号**：

```text
新建待接受邀请
→ 该手机号先普通微信登录
→ 系统自动发现邀请
→ 自动进入邀请页
→ 接受后进入企业
```

当前 186 已完成接受，所以该场景需用新的测试手机号或重置邀请测试数据后再验。

## 7. 回滚

- 后端邀请发现：移除 `invite/invitation/pending` Provider 与 App 分流。
- 手机号展示：恢复邀请页不显示手机号。
- 会话恢复：恢复 App 仅在本地已有 role 时刷新身份。
- 测试身份数据使用 CHG-020 备份恢复。
