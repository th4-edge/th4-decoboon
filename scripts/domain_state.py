#!/usr/bin/env python3
"""Initialize, inspect, validate, and update a recoverable business-domain packet."""
from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

PHASES = [
    "INTAKE",
    "SCENARIOS_READY",
    "BIZ_ALIGNED",
    "BLUEPRINT_READY",
    "PLAN_READY",
    "DEV_IN_PROGRESS",
    "DEV_VERIFIED",
    "UI_REVIEWED",
    "RECONCILED",
    "ACCEPTED",
    "RELEASED",
    "LEARNED",
    "CYCLE_CLOSED",
]
PHASE_INDEX = {phase: index for index, phase in enumerate(PHASES)}
ARTIFACT_KEYS = [
    "runEnvelope", "charter", "scenarios", "businessSync", "blueprint", "requirements",
    "plan", "implementation", "uiReview", "acceptance", "release", "reconciliation",
]
DOMAIN_RE = re.compile(r"^[a-z0-9][a-z0-9-]*$")


def now() -> str:
    return datetime.now(ZoneInfo("Asia/Shanghai")).isoformat(timespec="seconds")


def root_from(value: str | None) -> Path:
    return Path(value or (Path.cwd() / "docs/product/domains")).resolve()


def domain_dir(root: Path, domain: str) -> Path:
    if not DOMAIN_RE.fullmatch(domain):
        raise SystemExit("domain must match ^[a-z0-9][a-z0-9-]*$")
    return root / domain


def read_json(path: Path) -> dict:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: dict) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def require_state(root: Path, domain: str) -> tuple[Path, dict]:
    folder = domain_dir(root, domain)
    state_path = folder / "state.json"
    if not state_path.exists():
        raise SystemExit(f"state not found: {state_path}")
    try:
        state = read_json(state_path)
    except json.JSONDecodeError as exc:
        raise SystemExit(f"invalid state.json: {exc}") from exc
    return folder, state


def cmd_init(args: argparse.Namespace) -> int:
    root = root_from(args.root)
    folder = domain_dir(root, args.domain)
    templates = root / "_template"
    if not templates.is_dir():
        raise SystemExit(f"template directory not found: {templates}")
    if folder.exists() and not args.force:
        raise SystemExit(f"domain already exists: {folder} (use --force to overwrite)")
    folder.mkdir(parents=True, exist_ok=True)
    created = now()
    replacements = {
        "{{DOMAIN}}": args.domain,
        "{{TITLE}}": args.title,
        "{{NORTH_STAR}}": args.north_star,
        "{{OWNER}}": args.owner,
        "{{CREATED_AT}}": created,
    }
    for source in sorted(templates.iterdir()):
        if not source.is_file():
            continue
        content = source.read_text(encoding="utf-8")
        for key, value in replacements.items():
            content = content.replace(key, value)
        (folder / source.name).write_text(content, encoding="utf-8")
    state_path = folder / "state.json"
    state = read_json(state_path)
    state["history"] = [{
        "at": created,
        "phase": "INTAKE",
        "batch": "B0",
        "note": "domain initialized by scripts/domain_state.py",
    }]
    write_json(state_path, state)
    print(f"initialized: {folder}")
    print("phase: INTAKE")
    print("next: complete 00-charter.md and 01-scenarios.md, then submit business questions")
    return 0


def cmd_show(args: argparse.Namespace) -> int:
    folder, state = require_state(root_from(args.root), args.domain)
    print(f"domain: {state.get('domain')} | title: {state.get('title')}")
    print(f"northStar: {state.get('northStar')}")
    print(f"phase: {state.get('phase')} | batch: {state.get('batch')}")
    print(f"nextAction: {state.get('nextAction')}")
    print(f"openDecisions: {len(state.get('openDecisions', []))}")
    print(f"blockers: {len(state.get('blockers', []))}")
    print("artifacts:")
    for key in ARTIFACT_KEYS:
        relative = state.get("artifacts", {}).get(key, "")
        path = folder / relative if relative else folder
        print(f"  {key:14} {'OK' if relative and path.exists() else 'MISSING'}  {relative}")
    history = state.get("history", [])
    if history:
        print("recent history:")
        for item in history[-5:]:
            print(f"  {item.get('at')} {item.get('phase')} {item.get('batch')} {item.get('note')}")
    return 0


def scan_placeholders(folder: Path) -> list[str]:
    findings: list[str] = []
    for path in folder.glob("*"):
        if not path.is_file() or path.suffix not in {".md", ".json"}:
            continue
        text = path.read_text(encoding="utf-8")
        if "TODO" in text:
            findings.append(f"TODO: {path.name}")
        if "{{" in text:
            findings.append(f"unresolved template placeholder: {path.name}")
    return findings


def cmd_validate(args: argparse.Namespace) -> int:
    root = root_from(args.root)
    folder, state = require_state(root, args.domain)
    errors: list[str] = []
    warnings: list[str] = []
    required = ["schemaVersion", "domain", "title", "northStar", "phase", "batch", "artifacts", "history"]
    for key in required:
        if key not in state:
            errors.append(f"state missing key: {key}")
    if state.get("domain") != args.domain:
        errors.append(f"state domain mismatch: {state.get('domain')} != {args.domain}")
    if state.get("phase") not in PHASES:
        errors.append(f"unknown phase: {state.get('phase')}")
    for key in ARTIFACT_KEYS:
        relative = state.get("artifacts", {}).get(key)
        if not relative:
            errors.append(f"artifact mapping missing: {key}")
        elif not (folder / relative).exists():
            errors.append(f"artifact missing: {key} -> {relative}")
    blueprint_path = folder / state.get("artifacts", {}).get("blueprint", "03-blueprint.json")
    if blueprint_path.exists():
        try:
            read_json(blueprint_path)
        except json.JSONDecodeError as exc:
            errors.append(f"invalid blueprint JSON: {exc}")
    findings = scan_placeholders(folder)
    warnings.extend(findings)
    for warning in warnings:
        print(f"WARN {warning}")
    for error in errors:
        print(f"ERROR {error}")
    print(f"phase={state.get('phase')} batch={state.get('batch')}")
    if errors or (args.strict and warnings):
        print("RESULT: FAIL")
        return 1
    print("RESULT: PASS" + (" (strict)" if args.strict else " (warnings allowed)"))
    return 0


def cmd_log(args: argparse.Namespace) -> int:
    folder, state = require_state(root_from(args.root), args.domain)
    if args.phase:
        if args.phase not in PHASES:
            raise SystemExit(f"unknown phase: {args.phase}")
        state["phase"] = args.phase
    if args.batch:
        state["batch"] = args.batch
    if args.next_action is not None:
        state["nextAction"] = args.next_action
    state.setdefault("history", []).append({
        "at": now(),
        "phase": state.get("phase"),
        "batch": state.get("batch"),
        "note": args.note,
    })
    state["updatedAt"] = now()
    write_json(folder / "state.json", state)
    print(f"logged: {args.domain} {state.get('phase')} {state.get('batch')}")
    return 0


def cmd_next_prompt(args: argparse.Namespace) -> int:
    folder, state = require_state(root_from(args.root), args.domain)
    phase = state.get("phase")
    batch = state.get("batch")
    next_action = state.get("nextAction") or "读取领域包并判断下一批"
    decisions = state.get("openDecisions", [])
    blockers = state.get("blockers", [])
    print(f"继续 {args.domain}。先读取 state.json、run-envelope.md、00-charter.md、10-reconciliation.md 和 open decisions。")
    print("")
    print(f"当前阶段：{phase}")
    print(f"当前批次：{batch}")
    print(f"下一动作：{next_action}")
    print(f"未决业务事项：{decisions if decisions else '无'}")
    print(f"阻塞项：{blockers if blockers else '无'}")
    print("")
    print("本轮只做 state.json 的 nextAction 对应的一个垂直切片。")
    print("执行前先核对 run-envelope.md 的自主权限和人工门禁。")
    print("完成后必须更新 state.json、history、06-implementation.md 和 10-reconciliation.md。")
    print("命中人工门禁时，停止该分支，写入决策包；不要跳过业务裁决，不要正式发布小程序。")
    print("")
    if phase in {"ACCEPTED", "RELEASED", "LEARNED"}:
        print("当前阶段已到收口阶段；如无新的用户指令，只报告结果，不继续扩张。")
    else:
        print("完成后输出：本批结果、证据、未闭环项、下一批提示词、门禁判断。")
    return 0


def cmd_set_phase(args: argparse.Namespace) -> int:
    folder, state = require_state(root_from(args.root), args.domain)
    old = state.get("phase")
    if args.phase not in PHASES:
        raise SystemExit(f"unknown phase: {args.phase}")
    old_index = PHASE_INDEX.get(old)
    new_index = PHASE_INDEX[args.phase]
    if old_index is not None and new_index > old_index + 1:
        print(f"WARN phase skip: {old} -> {args.phase}; gate evidence must be recorded in state/artifacts")
    state["phase"] = args.phase
    if args.batch:
        state["batch"] = args.batch
    if args.next_action is not None:
        state["nextAction"] = args.next_action
    state.setdefault("history", []).append({
        "at": now(),
        "phase": args.phase,
        "batch": state.get("batch"),
        "note": args.note,
    })
    state["updatedAt"] = now()
    write_json(folder / "state.json", state)
    print(f"phase: {old} -> {args.phase}; batch={state.get('batch')}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", help="domain packet root; default: docs/product/domains")
    sub = parser.add_subparsers(dest="command", required=True)

    init = sub.add_parser("init", help="create a domain packet from _template")
    init.add_argument("--domain", required=True)
    init.add_argument("--title", required=True)
    init.add_argument("--north-star", required=True)
    init.add_argument("--owner", default="codex")
    init.add_argument("--force", action="store_true")
    init.set_defaults(func=cmd_init)

    show = sub.add_parser("show", help="show recovery state")
    show.add_argument("--domain", required=True)
    show.set_defaults(func=cmd_show)

    validate = sub.add_parser("validate", help="validate state and artifacts")
    validate.add_argument("--domain", required=True)
    validate.add_argument("--strict", action="store_true")
    validate.set_defaults(func=cmd_validate)

    log = sub.add_parser("log", help="append a history entry")
    log.add_argument("--domain", required=True)
    log.add_argument("--note", required=True)
    log.add_argument("--phase", choices=PHASES)
    log.add_argument("--batch")
    log.add_argument("--next-action")
    log.set_defaults(func=cmd_log)

    next_prompt = sub.add_parser("next-prompt", help="print a recovery-aware next-batch prompt")
    next_prompt.add_argument("--domain", required=True)
    next_prompt.set_defaults(func=cmd_next_prompt)

    phase = sub.add_parser("set-phase", help="advance or record a phase")
    phase.add_argument("--domain", required=True)
    phase.add_argument("--phase", required=True, choices=PHASES)
    phase.add_argument("--batch")
    phase.add_argument("--next-action")
    phase.add_argument("--note", default="phase updated")
    phase.set_defaults(func=cmd_set_phase)
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
