# 18660219918 财务受邀注册测试环境 · 开发验证证据

> 状态：`ACCEPTED_DEV_VERIFIED`  
> 风险级别：L3（本地身份、权限、凭证和邀请数据）  
> 变更记录：`CHG-20261029-020-local-boss-phone-detach-finance-invite.md`

## 1. 测试思路

保留原 Boss 员工和权限，把 Boss 的手机号与微信身份迁到本地测试手机号：

```text
原 Boss
手机号：18660219918
角色：Boss
微信 openId：已绑定 186

迁移后 Boss
手机号：19900009918
角色：Boss
凭证：Password
原密码继续有效
真实微信 openId：已释放

新邀请
手机号：18660219918
角色：Finance
企业：昭青装饰
门店：总店
邀请状态：Pending
```

这样当前 Boss 仍可通过 Web 密码登录，同时 `18660219918` 可以重新通过微信授权并接受 Finance 邀请。

## 2. 数据库变更结果

| 对象 | 变更前 | 变更后 |
| --- | --- | --- |
| 员工 `张伟` | 手机号 `18660219918`，Boss Active | 手机号 `19900009918`，Boss Active |
| 微信凭证 | phone=186，type=Wx，绑定真实 openId | phone=199，type=Password，原真实 openId 已释放 |
| 加入限制 | phone=186 | phone=199 |
| Finance 邀请 | 无 | 186 / Finance / 总店 / Pending |

真实微信 openId 和 `18660219918` 当前不再命中任何旧凭证。

## 3. Boss 登录

Web 验证：

```text
手机号：19900009918
密码：原 Web 密码，未变更
结果：登录 /workbench
标题：工作台 | 昭青装饰
身份：张伟 / 企业负责人
```

原 Boss 的 password hash 未修改。

## 4. Finance 邀请

```text
inviteId：1793271807543993360
企业：昭青装饰
门店：总店
被邀请人：张伟
手机号：18660219918
岗位：财务
邀请人：张伟
状态：待接受
有效期至：2026-11-05 19:03:27
```

公开邀请详情接口已验证返回：

```text
company=昭青装饰
role=Finance
store=总店
status=Pending
inviter=张伟
```

Web 成员与组织页已能看到该待接受邀请：

- `screenshots/organization-finance-invite-pending-1440.png`

## 5. 微信开发者工具操作

### 5.1 清缓存

在微信开发者工具中清除小程序的本地数据缓存，避免继续使用之前 Boss 的 token/session。

### 5.2 添加编译模式

启动页面：

```text
pages/invite/accept
```

启动参数：

```text
inviteId=1793271807543993360
```

完整页面路径：

```text
pages/invite/accept?inviteId=1793271807543993360
```

### 5.3 授权手机号

确认页面显示：

```text
昭青装饰邀请你加入
岗位：财务
门店：总店
邀请人：张伟
状态：邀请有效
```

点击“手机号快捷登录并加入”，微信授权手机号必须是：

```text
18660219918
```

如果授权的是其他手机号，后端会按规则拒绝。

### 5.4 成功后验证

预期：

```text
微信授权成功
→ 接受 Finance 邀请
→ 新员工记录出现在昭青装饰 / 总店
→ 角色 Finance
→ 进入财务工作台
```

Boss 不受影响，仍通过 Web 使用：

```text
19900009918 + 原密码
```

## 6. 接受后需要回读的数据

用户完成微信授权后，需要核对：

- 新员工 `18660219918` 是否唯一；
- 新员工角色是否为 Finance、状态是否 Active；
- 是否归属总店；
- 旧 Boss 是否仍为 `19900009918 / Boss / Active`；
- 新微信凭证是否绑定新 Finance 员工；
- 邀请状态是否 Accepted；
- Web Finance 菜单、App Finance 首页是否可进入。

这些需要在用户完成微信授权后继续验证，当前尚不能宣称完整通过。

## 7. 回滚

备份目录：

```text
~/.decoboon-local/20261029-finance-invite-186/
```

文件：

- `before.sql`：变更前相关表完整备份，权限 600。
- `before-selected.tsv`：变更前关键行摘要。
- `rollback-before-accept.sql`：尚未接受邀请时的快速回滚。
- `invite-info.txt`：邀请 ID、路径和有效期。

如果邀请已经接受，必须使用完整备份恢复，不能只执行快速回滚脚本。

## 8. 当前结论

- Boss 手机号迁移：PASS
- 原微信 openId 释放：PASS
- Boss Web 密码登录：PASS
- Finance 邀请创建：PASS
- Web 待接受邀请列表：PASS
- 微信开发者工具受邀注册成功：PASS，Finance 员工已创建
- 接受后未进入首页：根因与修复见 `CHG-20261029-022`
