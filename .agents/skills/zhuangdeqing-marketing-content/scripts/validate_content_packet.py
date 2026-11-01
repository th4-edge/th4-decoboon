#!/usr/bin/env python3
"""Validate a ZHUANGDEQING marketing-content packet."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

REQUIRED = {
    "id": str,
    "type": str,
    "status": str,
    "scenarioRefs": list,
    "audience": dict,
    "contentGoal": str,
    "familiarProblem": str,
    "currentWorkaround": str,
    "productMechanism": str,
    "evidenceRefs": list,
    "boundaries": list,
    "deliverables": list,
    "channelHandoffs": list,
    "learning": dict,
}
STATUSES = {"intake", "fact_checked", "drafted", "review_ready", "handed_off", "measured", "learned", "blocked"}


def validate(packet: dict) -> list[str]:
    errors: list[str] = []
    for key, typ in REQUIRED.items():
        if key not in packet:
            errors.append(f"missing field: {key}")
        elif not isinstance(packet[key], typ):
            errors.append(f"{key} must be {typ.__name__}")
    if packet.get("status") not in STATUSES:
        errors.append(f"invalid status: {packet.get('status')}")
    if not packet.get("scenarioRefs"):
        errors.append("scenarioRefs must not be empty")
    if not packet.get("evidenceRefs"):
        errors.append("evidenceRefs must not be empty")
    if not packet.get("boundaries"):
        errors.append("boundaries must not be empty")
    audience = packet.get("audience")
    if isinstance(audience, dict):
        if not audience.get("role"):
            errors.append("audience.role is required")
        if not audience.get("companyFit"):
            errors.append("audience.companyFit is required")
    learning = packet.get("learning")
    if isinstance(learning, dict) and learning.get("decision") not in {None, "keep", "adjust", "extend", "stop"}:
        errors.append("learning.decision must be keep/adjust/extend/stop")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("packet")
    args = parser.parse_args()
    path = Path(args.packet)
    packet = json.loads(path.read_text())
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
