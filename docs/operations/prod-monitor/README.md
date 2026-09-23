# 生产后端监控

## 一键巡检

```bash
bash scripts/prod-monitor.sh status
bash scripts/prod-monitor.sh collect "30 minutes ago"
```

采集结果写入 `docs/operations/incidents/YYYY-MM-DD/<timestamp>/`，并按错误指纹更新 `registry.json`。

退出码：

- `0`：无可处理异常。
- `10`：发现高置信代码缺陷。
- `20`：发现基础设施、外部依赖或未知异常。
- `2`：采集失败。

## 受控修复

```bash
bash scripts/prod-monitor.sh snapshot-source
mvn -o -q -Dtest=<RegressionTest> test
mvn -o -q -Dtest='!ProviderSecurityAcceptanceTest,!ProductionReleaseBaselineAcceptanceTest' test
bash scripts/build-release.sh
bash scripts/deploy-prod.sh --release-dir dist/releases/<timestamp>
bash scripts/prod-monitor.sh mark-fixed <incident-id> \
  --release /opt/decoboon/releases/<version> \
  --note "根因、测试、发布与冒烟结果"
```

出现启动或健康检查失败时，部署脚本会自动切回上一版本。

## 配置

可使用环境变量覆盖：

- `DECOBOON_PROD_HOST`
- `DECOBOON_PROD_USER`
- `DECOBOON_PROD_SSH_KEY`
- `DECOBOON_ENV_FILE`
- `DECOBOON_SERVICE`
- `DECOBOON_API_URL`
