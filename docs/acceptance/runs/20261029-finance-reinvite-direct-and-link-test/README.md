# 18660219918 财务二次邀请 · 直接登录与邀请链路双路线验收

> 状态：`READY_FOR_TWO_ROUTE_USER_TEST`    
> 风险级别：L3（本地身份/凭证/邀请测试数据）  
> 环境记录：`CHG-20261029-023-local-finance-reinvite-two-route-test.md`

## 1. 当前测试数据

```text
Boss：张伟 / 19900009918 / Active / 总店
上一轮 Finance：张伟 / 19900008818 / Resigned（保留历史记录）
待接受邀请：
  inviteId：1793273418616909990
  手机号：18660219918
  角色：Finance
  企业：昭青装饰
  门店：总店
  状态：Pending
  有效期至：2026-11-05 19:30:18
```

邀请详情实际返回：

```text
maskedTelNo=186****9918
```

## 2. 本次新增体验

普通微信登录完成手机号授权后：

```text
sessionType=user / 未加入企业
→ 调用 invite/invitation/pending
→ 使用服务端已验证手机号查到 Finance 邀请
→ 进入邀请确认页
→ 页面显示昭青装饰、财务、总店、186****9918
→ 当前手机号已经验证，因此显示“确认加入「昭青装饰」”
```

不会再次弹出手机号授权。

用户仍需点击一次“确认加入”，这是显式加入企业的业务确认，不做静默自动加入。

## 3. 测试路线一：直接搜索/普通登录

### 准备

在微信开发者工具中重新编译最新小程序，并清除小程序本地数据缓存。

### 操作

```text
微信搜索/直接打开装得清
→ 普通登录
→ 授权微信手机号 18660219918
```

### 预期

1. 不通过邀请分享也登录成功；
2. 系统自动发现 Finance 待接受邀请；
3. 进入邀请确认页；
4. 展示：
   - 企业：昭青装饰
   - 岗位：财务
   - 门店：总店
   - 手机号：186****9918
5. 按钮为“确认加入「昭青装饰」”；
6. 再次点击时不重复授权手机号。

### 注意

第一条路线只需确认“系统能否主动发现并展示邀请”，建议先不要点击“确认加入”。  
如果点击确认，邀请会被接受，第二条编译邀请链路将变成“邀请已使用”。

## 4. 测试路线二：编译指定邀请页面

### 准备

第一条路线完成后，清除小程序本地数据缓存。

### 编译模式

启动页面：

```text
pages/invite/accept
```

启动参数：

```text
inviteId=1793273418616909990
```

完整路径：

```text
pages/invite/accept?inviteId=1793273418616909990
```

### 预期

1. 邀请页正常展示“昭青装饰邀请你加入”；
2. 岗位显示财务；
3. 门店显示总店；
4. 手机号显示 `186****9918`；
5. 点击“手机号快捷登录并加入”；
6. 授权手机号为 `18660219918`；
7. 接受邀请成功；
8. 创建新的 Finance 员工；
9. 进入装得清 Finance 首页。

## 5. 接受后应回读

- 新 Finance 员工 `18660219918 / Finance / Active / 总店`；
- 微信凭证重新绑定新 Finance 员工；
- 新邀请状态变为 Accepted；
- Boss `19900009918` 保持 Active；
- 旧 Finance `19900008818` 保持 Resigned；
- Finance 首页、菜单和数据权限正常。

## 6. 编译错误修复

微信开发者工具曾报：

```text
module 'domains/identity/inviteContext.js' is not defined
```

原因是 `App.vue` 顶层新增了对页面级 `inviteContext` 的运行时依赖。已将邀请落点计算内联到 App，重新构建后 dev/build 的 `app.js` 均不再 require `inviteContext.js`。详见 CHG-20261029-024。

## 7. 验证与构建

```text
PendingEntryInviteProviderTest PASS
npm run type-check PASS
npm run check:invite-flow PASS
npm run check:registration PASS
npm run check:navigation PASS
npm run build:mp-weixin PASS
npm run check:subpackages PASS
```

## 8. 回滚

完整备份：

```text
~/.decoboon-local/20261029-finance-reinvite-186/before.sql
```

如测试异常，可使用该备份恢复员工、凭证、邀请、加入限制、成员日志和安全日志。

## 9. 当前结论

- 邀请创建：PASS
- 脱敏手机号接口：PASS
- 直接登录主动发现邀请：实现完成，待路线一真机验证
- 编译邀请链接：待路线二验证
- 最终受邀加入：待用户点击确认或完成邀请页授权
