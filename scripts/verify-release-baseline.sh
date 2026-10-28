#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:---deploy}"

case "$MODE" in
  --build|--deploy) ;;
  *)
    echo "usage: verify-release-baseline.sh [--build|--deploy]" >&2
    exit 2
    ;;
esac

fail() {
  echo "[release-baseline] FAIL: $*" >&2
  exit 1
}

if [ -n "$(git status --porcelain --untracked-files=all)" ]; then
  fail "worktree must be clean before release build/deploy"
fi

HEAD_SHA="$(git rev-parse HEAD)"
REMOTE_REFS="$(git ls-remote --heads origin prod test dev)" || fail "cannot read origin branches"

if [ "$MODE" = "--build" ]; then
  REQUIRED_BRANCHES="test"
else
  REQUIRED_BRANCHES="prod test dev"
fi

for branch in $REQUIRED_BRANCHES; do
  REMOTE_SHA="$(printf '%s\n' "$REMOTE_REFS" | awk -v ref="refs/heads/$branch" '$2 == ref {print $1}')"
  [ -n "$REMOTE_SHA" ] || fail "origin/$branch not found"
  [ "$REMOTE_SHA" = "$HEAD_SHA" ] || fail "HEAD $HEAD_SHA is not synced with origin/$branch $REMOTE_SHA for $MODE"
done

echo "[release-baseline] PASS ($MODE): required branches match HEAD=$HEAD_SHA"
