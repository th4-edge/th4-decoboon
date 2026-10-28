# 设计方案与设计报价审视 · 发布记录

## DQ-X1 / DQ-X2 生产发布（2026-10-24）

- 状态：**已上线**
- 变更级别：功能为 L1；生产补执行迁移 131，数据库变更按 L3 留痕。
- 用户确认：2026-10-24 明确确认发版。
- 发布范围：只发布 `DQ-X1 + DQ-X2`，排除当前工作区其他未验收改动和迁移 124–131。

### 代码版本

| 工程 | 分支 | 发布提交 |
| --- | --- | --- |
| th4-decoboon | `dev/test/prod` | `e627bbb` |
| th4-decoboon-web | `dev/test/prod` | `4cd9238` |
| th4-decoboon-app | `dev/test/prod` | `efe2bae` |

### 生产产物

| 端 | 版本 / 发布目录 | 上一稳定回滚点 |
| --- | --- | --- |
| 后端 | `/opt/decoboon/releases/20261024-030000` | `/opt/decoboon/releases/20261022-131500` |
| Web | `/opt/decoboon-web/releases/20261024-030500` | `/opt/decoboon-web/releases/20261023-193138` |
| App | `1.0.67 / 167` 已上传；正式版由用户本人提审发布 | 微信公众平台回退体验版 |

- 后端 Jar SHA-256：`7c0855787509ac79327fcaba2a1e5529bdfc84c7c0800fc059eead44fd7269d4`。
- Web `dist/index.html` SHA-256：`56f08d35fde6f2c01942e98c3c9f1976e550f380b82567414751e5029f8cf67e`。
- App `app.json` SHA-256：`c118cd61c18bf4a80abe36720900e3a09f2acb08561297bfcb0efcfb913d93ff`。
- App 上传信息：`docs/operations/releases/upload-info-1.0.67.json`。
- App 上传总包：4,412,286 B；主包 1,195,115 B。

### 验证

- 后端全量：927 tests / 0 failures / 0 errors / 0 skipped。
- 后端定向：跟进分页、量房分页、设计分页、谈单分页、签约分页、风险规则与 Provider 安全清单 PASS。
- Web：`type-check`、生产构建、权限、工作台、工作空间、客户端文案门禁 PASS。
- App：`type-check`、生产环境/合规门禁、微信生产构建、分包检查 PASS。
- 隔离候选联调：使用候选后端 `8091` + 候选 Web `8081`，同账号完成五阶段标题、筛选、谈单/签约搜索冒烟 PASS。
- 生产健康：`/api/auth/session/info` 返回 `tokenIsUndefined`；五个业务 Provider 均返回鉴权响应而非 404。
- 生产 Web：`/`、`/login` 和五个设计师路由 HTTP 200；`/v3/` HTTP 301。
- 生产后端服务 `decoboon` active，`RELEASE_INFO.health_http=200`。

### 未覆盖 / 后续

- 生产环境未使用真实账号执行登录后的写入动作，避免污染生产数据。
- App 体验版需用户/管理员在微信公众平台设置为体验版；正式审核与发布由用户本人完成。
- Codex 未点击“提交审核”或“发布正式版”，未代填审核资料。

### 回滚

后端：

```bash
bash scripts/rollback-prod.sh /opt/decoboon/releases/20261022-131500
```

Web：

```bash
ssh root@120.55.15.159 \
  "ln -sfn /opt/decoboon-web/releases/20261023-193138 /opt/decoboon-web/current && nginx -t && systemctl reload nginx"
```

App：在微信公众平台回退体验版；正式版未由 Codex 提交或发布。
