# 报价项目库保存响应递归

- 状态：fixed
- 分类：code_bug
- 级别：L2（报价成本敏感数据响应）
- 用户报告：2026-10-21
- 修复提交：`01f6e77`
- 修复 release：`/opt/decoboon/releases/20261021-143125`

## 现象

线上 Web 财务在报价项目库编辑分项后点击“保存报价分项”，页面提示“请求失败”。

## 生产证据

- Nginx：`POST /api/catalog/item/update` HTTP 200，响应体约 57 KB。
- 正常单条保存响应约 1.2 KB。
- 生产应用未记录业务异常，失败发生在响应序列化阶段。

## 根因

`catalog/item/save|update` 的响应白名单包含 `maintainer`，但未为 `Employee` 配置嵌套白名单。被邀请加入企业的财务账号存在：

```text
Employee.invite -> Invite.inviter -> Employee.invite -> ...
```

Jackson 序列化超过最大嵌套深度 1000，保存虽然进入接口，但响应无法正常交付。

## 修复

- `Save.java`、`Update.java` 增加：
  `@AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})`
- 不改变报价库权限、成本、销售价、启停或明细同步语义。

## 回归与发布

- 新增带邀请循环关系的财务账号 HTTP 回归。
- 保存和更新均成功，响应体积受控，材料价、人工价和标准成本可回读。
- 后端定向 12/12、全量 896/896 通过。
- 生产 health 200，服务 active；发布后 10 分钟未出现新的报价库保存失败指纹。
