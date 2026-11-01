# 测试环境与数据契约

## 本地 V0.1 基线

- 企业：昭青科技，`com_id=199500000000000001`
- 数据库：`127.0.0.1:3306/decoboon`
- 后端：`http://127.0.0.1:8090/api`
- Web：`http://127.0.0.1:8080`
- 小程序：`th4-decoboon-app/dist/dev/mp-weixin`
- 阶段夹具：`docs/acceptance/runs/20261031-full-chain-stage-fixture/account-and-stage-map.json`
- 财务链：`docs/acceptance/runs/20261031-full-chain-stage-fixture/finance-chain-state.json`

## 角色账号

| 角色 | 手机号 |
| --- | --- |
| Boss | `13910002001` |
| StoreManager | `13910002002` |
| Finance | `13910002003` |
| Designer | `13910002004` |
| Marketer | `13910002005` |
| Pm | `13910002006` |
| Designer2 | `13910002009` |

统一本地合成密码见夹具说明，不得写入执行报告。

## 数据纪律

- 每次写执行使用唯一 `AUDIT-<date>-<run-id>` 前缀；
- 不复用生产客户、供应商、合同、付款或验收数据；
- 不直接 SQL 制造业务终态；SQL 只用于夹具准备和只读对账；
- 破坏性测试前有备份和恢复演练；
- 业主确认必须使用与客户预留手机号匹配的真实授权身份；
- 不发送真实短信、微信、订阅消息、支付或电子签。
