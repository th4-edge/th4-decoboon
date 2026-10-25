# 独立验收命令与运行口径

## 后端

```bash
SPRING_PROFILES_ACTIVE=dev \
SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3307/decoboon_accept?...' \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD='' \
SERVER_PORT=8090 \
java -cp "target/classes:$(cat logs/runtime-classpath.txt)" com.th4.edge.Application
```

默认 `mvn spring-boot:run` 的失败日志见 `logs/backend-start.log`，是无关测试编译问题；主代码 direct Java 启动日志见 `logs/backend-direct-java.log`。

## 18 用例

```bash
python3 docs/acceptance/runs/20261023-emg-v2-independent-acceptance/run_acceptance.py
```

## Web

```bash
npm run dev -- --host 127.0.0.1 --port 8085
npm run check:permissions
npm run check:workbenches
```

页面通过真实 BOSS/店长登录、DOM 快照和截图取证，原生日志见 `logs/web-vite.log`。

## App

```bash
npm run check:navigation
npm run build:mp-weixin
EMG_ACCEPT_PHONE=13990000002 EMG_ACCEPT_PASSWORD=<由本地注入> \
  node docs/acceptance/runs/20261023-emg-v2-independent-acceptance/app/app-page-review.mjs
```

微信开发者工具项目根目录为 `前端代码/th4-decoboon-app/dist/dev`，自动化端口以当次 `cli auto` 输出为准。

## SQL

只读复核使用本地 MySQL socket；合成测试数据只写入 `decoboon_accept`，未操作生产。所有查询和清理快照在 `sql/`。
