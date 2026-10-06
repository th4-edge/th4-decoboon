#!/usr/bin/env bash
set -euo pipefail

RUN='/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20261007-单BOSS一人装企全链路'
: "${SINGLE_BOSS_PASSWORD:?请通过环境变量 SINGLE_BOSS_PASSWORD 提供仅本地使用的测试口令}"

HASH=$(SINGLE_BOSS_PASSWORD="$SINGLE_BOSS_PASSWORD" python3 - <<'PY'
import bcrypt
import os
raw = os.environ['SINGLE_BOSS_PASSWORD'].encode()
print(bcrypt.hashpw(raw, bcrypt.gensalt(rounds=10, prefix=b'2a')).decode())
PY
)

python3 - "$RUN/raw/sql/setup-single-boss.sql" "$HASH" <<'PY' | mysql -h127.0.0.1 -uroot -pmysqladmin decoboon
from pathlib import Path
import sys
sql = Path(sys.argv[1]).read_text()
print(sql.replace('__PASSWORD_HASH__', sys.argv[2]))
PY
