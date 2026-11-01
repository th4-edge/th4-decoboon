#!/bin/bash
# 装得清后端本地开发启动脚本
# 用法: ./run-dev.sh
#
# 构建采用 classes/lib/config 分离部署结构，直接使用 target/classes 与
# target/lib 启动，避免 thin JAR 的 Main-Class 忽略 loader.path。

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

if [ -f "$PROJECT_DIR/.env.dev.local" ]; then
  # 本地开发基础配置（DB/Redis/微信小程序/存储等）。历史坑：此前只加载 oss 与 wechat-pay，
  # 导致"单独重启后端"时微信小程序配置丢失 → 业主微信登录换码拿不到 openid → 误报"首次登录请授权手机号"。
  set -a
  # shellcheck disable=SC1091
  . "$PROJECT_DIR/.env.dev.local"
  set +a
fi

if [ -f "$PROJECT_DIR/.env.oss.local" ]; then
  # 本地 OSS 密钥文件不入库，存在时自动加载并启用 OSS。
  set -a
  # shellcheck disable=SC1091
  . "$PROJECT_DIR/.env.oss.local"
  set +a
fi

if [ -f "$PROJECT_DIR/.env.wechat-pay.local" ]; then
  # 微信支付证书与密钥文件不入库，存在时自动加载完整支付配置。
  set -a
  # shellcheck disable=SC1091
  . "$PROJECT_DIR/.env.wechat-pay.local"
  set +a
fi

JAR="target/th4-decoboon-1.0.0.jar"
CLASS_PATH="$PROJECT_DIR/target/classes:$PROJECT_DIR/target/lib/*"
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_BIN="$JAVA_HOME/bin/java"
elif [ -x "/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home/bin/java" ]; then
  JAVA_BIN="/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home/bin/java"
else
  JAVA_BIN="$(command -v java)"
fi

if [ ! -f "$JAR" ]; then
  echo "[run-dev] 未找到 $JAR，先执行 mvn package..."
  mvn package -DskipTests -q
fi

echo "[run-dev] 启动 th4-decoboon (dev profile)..."
echo "[run-dev]   main:   com.th4.edge.Application"
echo "[run-dev]   java:   $JAVA_BIN"
echo "[run-dev]   classpath: $CLASS_PATH"
echo "[run-dev]   port:   8090"
echo ""

exec "$JAVA_BIN" -cp "$CLASS_PATH" com.th4.edge.Application --spring.profiles.active=dev
