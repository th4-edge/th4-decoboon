#!/usr/bin/env python3
"""Validate a ZHUANGDEQING usage-content packet."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

REQUIRED = {
    "id": str,
    "type": str,
    "status": str,
    "scenarioRefs": list,
    "role": str,
    "surface": str,
    "companyFit": str,
    "prerequisite": list,
    "currentPath": list,
    "action": str,
    "feedback": str,
    "readback": list,
    "exceptions": list,
    "compatibility": dict,
    "contentAssets": list,
    "evidenceRefs": list,
    "productGaps": list,
    "learning": dict,
}
STATUSES = {"intake", "evidence_checked", "compatibility_checked", "drafted", "review_ready", "handed_off", "used", "learned", "blocked"}
COMPAT_TYPES = {"MAPPED", "WORKAROUND_ALLOWED", "RECORD_ONLY", "T05_REQUIRED", "UNSUPPORTED"}


def validate(packet: dict) -> list[str]:
    errors: list[str] = []
    for key, typ in REQUIRED.items():
        if key not in packet:
            errors.append(f"missing field: {key}")
        elif not isinstance(packet[key], typ):
            errors.append(f"{key} must be {typ.__name__}")
    if packet.get("status") not in STATUSES:
        errors.append(f"invalid status: {packet.get('status')}")
    if packet.get("surface") not in {"App", "Web", "App+Web", "Admin"}:
        errors.append("surface must be App, Web, App+Web, or Admin")
    if not packet.get("scenarioRefs"):
        errors.append("scenarioRefs must not be empty")
    if not packet.get("evidenceRefs"):
        errors.append("evidenceRefs must not be empty")
    if not packet.get("currentPath"):
        errors.append("currentPath must not be empty")
    if not packet.get("exceptions"):
        errors.append("exceptions must not be empty")
    compat = packet.get("compatibility")
    if isinstance(compat, dict) and compat.get("type") not in COMPAT_TYPES:
        errors.append("compatibility.type is invalid")
    learning = packet.get("learning")
    if isinstance(learning, dict) and learning.get("decision") not in {None, "keep", "adjust", "extend", "stop", "fix_product"}:
        errors.append("learning.decision is invalid")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("packet")
    args = parser.parse_args()
    packet = json.loads(Path(args.packet).read_text())
    errors = validate(packet)
    if errors:
        print("INVALID")
        for error in errors:
            print(f"- {error}")
        return 1
    print("VALID")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
