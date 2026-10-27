#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  echo "[release-baseline] FAIL: $*" >&2
  exit 1
}

if [ -n "$(git status --porcelain --untracked-files=all)" ]; then
  fail "worktree must be clean before release build/deploy"
fi

HEAD_SHA="$(git rev-parse HEAD)"
REMOTE_REFS="$(git ls-remote --heads origin prod test dev)" || fail "cannot read origin branches"

for branch in prod test dev; do
  REMOTE_SHA="$(printf '%s\n' "$REMOTE_REFS" | awk -v ref="refs/heads/$branch" '$2 == ref {print $1}')"
  [ -n "$REMOTE_SHA" ] || fail "origin/$branch not found"
  [ "$REMOTE_SHA" = "$HEAD_SHA" ] || fail "HEAD $HEAD_SHA is not synced with origin/$branch $REMOTE_SHA"
done

echo "[release-baseline] PASS: origin/prod=origin/test=origin/dev=HEAD=$HEAD_SHA"
