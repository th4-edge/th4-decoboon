#!/usr/bin/env python3
"""Fast, read-only watch for the ZHUANGDEQING domain delivery program."""
from __future__ import annotations

import argparse
import hashlib
import json
import socket
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[1]
DOMAINS = ROOT / "docs/product/domains"
BOARD = DOMAINS / "PROGRAM-BOARD.json"
APP = ROOT.parent.parent / "前端代码/th4-decoboon-app"
WEB = ROOT.parent.parent / "前端代码/th4-decoboon-web"
TZ = ZoneInfo("Asia/Shanghai")


def now() -> datetime:
    return datetime.now(TZ)


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def state_digest(domain: str) -> dict:
    path = DOMAINS / domain / "state.json"
    if not path.exists():
        return {"domain": domain, "exists": False}
    state = read_json(path)
    return {
        "domain": domain,
        "exists": True,
        "phase": state.get("phase"),
        "batch": state.get("batch"),
        "updatedAt": state.get("updatedAt"),
        "nextAction": state.get("nextAction"),
        "openDecisions": len(state.get("openDecisions", [])),
        "blockers": state.get("blockers", []),
    }


def git_paths(repo: Path, paths: list[str]) -> list[str]:
    if not repo.exists():
        return ["<missing-repo>"]
    try:
        proc = subprocess.run(
            ["git", "-C", str(repo), "status", "--porcelain", "--", *paths],
            check=False,
            text=True,
            capture_output=True,
        )
        if proc.returncode != 0:
            return [f"<git-error:{proc.returncode}>"]
        return [line for line in proc.stdout.splitlines() if line.strip()]
    except OSError as exc:
        return [f"<git-os-error:{exc.__class__.__name__}>"]


def port_open(port: int) -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.settimeout(0.2)
        return sock.connect_ex(("127.0.0.1", port)) == 0


def state_age_minutes(value: str | None) -> int | None:
    if not value:
        return None
    try:
        then = datetime.fromisoformat(value)
        if then.tzinfo is None:
            then = then.replace(tzinfo=TZ)
        return max(0, int((now() - then).total_seconds() // 60))
    except ValueError:
        return None


def fingerprint(payload: dict) -> str:
    raw = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def build_result() -> dict:
    board = read_json(BOARD)
    material = state_digest("material")
    contract = state_digest("contract-change")
    backend_dirty = git_paths(ROOT, ["src/main", "src/test"])
    app_dirty = git_paths(APP, ["src", "pages.json"])
    web_dirty = git_paths(WEB, ["src"])
    ports = {str(port): port_open(port) for port in (3306, 8090, 8080)}
    lease = board.get("activeLease") or {}
    leased_domain = lease.get("domain")
    leased_state = material if leased_domain == "material" else contract if leased_domain == "contract-change" else {}
    age = state_age_minutes(leased_state.get("updatedAt"))
    stale_after = int(lease.get("staleReviewAfterHours") or 3) * 60
    stale = bool(lease.get("status") == "ACTIVE" and age is not None and age >= stale_after)
    code_clean = not (backend_dirty or app_dirty or web_dirty)
    leased_released = leased_state.get("phase") in {"RELEASED", "LEARNED"}
    if not lease or lease.get("status") not in {"ACTIVE", "STALE"}:
        action = "DEEP_CHECK"
        reason = "no active domain lease"
    elif stale:
        action = "NOTIFY_STALE"
        reason = f"active lease state unchanged for {age} minutes"
    elif leased_released and code_clean:
        action = "RELEASE_ELIGIBLE"
        reason = f"{leased_domain} released and all code worktrees are clean"
    elif leased_released and not code_clean:
        action = "WAIT_WORKSPACE"
        reason = f"{leased_domain} released but code worktrees still dirty"
    else:
        action = "WAIT"
        reason = "active domain has not reached release boundary"

    key = {
        "programStage": board.get("currentProgramStage"),
        "lease": {
            "domain": lease.get("domain"),
            "owner": lease.get("ownerThreadId"),
            "status": lease.get("status"),
            "stateUpdatedAt": lease.get("lastDomainStateUpdatedAt"),
        },
        "material": material,
        "contract": contract,
        "backendDirty": backend_dirty,
        "appDirty": app_dirty,
        "webDirty": web_dirty,
        "ports": ports,
        "blockers": board.get("blockers", []),
        "nextProgramAction": board.get("nextProgramAction"),
        "action": action,
    }
    digest = fingerprint(key)
    previous = (board.get("programSupervisor") or {}).get("lastFingerprint")
    return {
        "checkedAt": now().isoformat(timespec="seconds"),
        "changed": digest != previous,
        "fingerprint": digest,
        "previousFingerprint": previous,
        "recommendation": {"action": action, "reason": reason},
        "lease": {
            "domain": lease.get("domain"),
            "ownerThreadId": lease.get("ownerThreadId"),
            "status": lease.get("status"),
            "codeWriteAllowed": lease.get("codeWriteAllowed"),
            "stateUpdatedAt": leased_state.get("updatedAt"),
            "stateAgeMinutes": age,
            "staleAfterMinutes": stale_after,
            "stale": stale,
        },
        "material": material,
        "contractChange": contract,
        "workspace": {
            "codeClean": code_clean,
            "backendDirtyCount": len(backend_dirty),
            "appDirtyCount": len(app_dirty),
            "webDirtyCount": len(web_dirty),
            "backendDirty": backend_dirty[:12],
            "appDirty": app_dirty[:12],
            "webDirty": web_dirty[:12],
        },
        "ports": ports,
        "blockers": board.get("blockers", []),
        "nextProgramAction": board.get("nextProgramAction"),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", action="store_true", help="emit JSON")
    parser.add_argument("--quiet", action="store_true", help="no output when unchanged")
    args = parser.parse_args()
    try:
        result = build_result()
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2
    if args.quiet and not result["changed"]:
        return 0
    if args.json:
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print(f"changed={result['changed']} action={result['recommendation']['action']}")
        print(f"reason={result['recommendation']['reason']}")
        print(f"lease={result['lease']}")
        print(f"material={result['material']}")
        print(f"contract={result['contractChange']}")
        print(f"workspace={result['workspace']}")
        print(f"ports={result['ports']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
