# FIN-WRITE-D3 开发证据

- 问题：`PI-20261022-016`
- 切片：`FIN-WRITE-D3` 店长凭证生成与跨端动作对齐
- 业务依据：`ANS/T12-BIZ-20261022-010`、`DEC-089`、`BR-055`
- 日期：2026-10-23
- 状态：`DEV_VERIFIED`
- DDL：无
- 独立验收：PENDING

## 1. 变更范围

- `VoucherContext#build` 允许 `StoreManager`，继续使用目标流水数据范围和同店校验。
- `VoucherContext#rebuildForPayment` 允许 `StoreManager`，并按目标 Payment 数据范围读取，防止跨店补录重渲染。
- 凭证分页与报表角色收窄为 BOSS/门店财务/总部财务/店长，移除 PM/设计师，与 Web 路由一致。
- App `roles/guard.ts` 增加凭证列表、模板列表和模板编辑页面级权限规则，敏感页面不再依赖无规则默认放行。
- Web 财务动作策略 `document/voucher/build` 加入 `StoreManager`，范围改为 `Company|Store`。
- 财务写权限注释同步为 Boss/Finance/StoreManager，店长限本店。
- 凭证模板维护原有 `DocumentTemplateContext.requireTemplateMaintainer` 已允许 BOSS/店长/财务，无需调整。

## 2. 验证结果

### 后端定向

```bash
mvn -o -Dtest=FinanceStoreScopeHttpAcceptanceTest#storeManagerCanIssueAndReprintOwnStoreVoucherButNotOtherStore test
```

结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖：

- 店长本店生成凭证成功；
- 同一流水重复生成返回同一 `voucherId/voucherNo`；
- 店长本店凭证分页可见；
- 店长使用他店流水生成凭证返回 403/404；
- 设计师访问凭证分页返回 403。

### 后端相关回归

```bash
mvn -o -Dtest=VoucherBuildAcceptanceTest,DocumentTemplateContextTest test
```

结果：

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Web

```bash
npm run check:actions
npm run check:permissions
npm run build
```

结果：

- 业务动作注册与服务端闭环范围检查通过；
- 业务权限、角色、数据范围和动作角色检查通过；
- `vue-tsc --noEmit`、客户端文案门禁、Vite production build 全部通过。

### App

```bash
npm run type-check
```

结果：PASS。

补充：

```bash
npm run check:action
npm run check:finance-ia
npm run check:navigation
```

全部通过。App `storeManager` 岗位矩阵包含 `edit:finance`；凭证列表/模板列表现在由 `view:finance` 页面规则守卫，模板编辑由 `edit:doc-template` 守卫。

## 3. 未完成

- 未做 Web/App 真实登录态页面截图与交互复核。
- 未做真机 PDF 打开/打印验证。
- 未执行生产发布。
