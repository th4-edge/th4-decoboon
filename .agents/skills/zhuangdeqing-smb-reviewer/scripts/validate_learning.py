#!/usr/bin/env python3
"""Validate a ZHUANGDEQING SMB reviewer learning packet.

The validator enforces evidence discipline only. It does not judge whether a
business claim is semantically correct.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


ALLOWED_CLAIM_STATUS = {"hypothesis", "draft", "approved", "implemented"}
ALLOWED_CONFIDENCE = {"low", "medium", "high"}
ALLOWED_PROMOTION = {"SCN", "BR", "SOL", "MET", "CAP", "GAP", "NONE"}
REQUIRED_SOURCE = {
    "source_id",
    "title",
    "publisher",
    "type",
    "published_at",
    "accessed_at",
    "locator",
    "excerpt",
    "can_prove",
    "cannot_prove",
    "interest_risk",
}
REQUIRED_CLAIM = {
    "claim_id",
    "claim",
    "scope",
    "status",
    "confidence",
    "source_refs",
    "internal_refs",
    "promotion_target",
    "limitations",
    "conflicts",
}


def is_non_empty_string(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def is_string_list(value: Any, *, allow_empty: bool = True) -> bool:
    return (
        isinstance(value, list)
        and (allow_empty or bool(value))
        and all(is_non_empty_string(item) for item in value)
    )


def validate(packet: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(packet, dict):
        return ["root: packet must be a JSON object"]

    for key in ("request_id", "target_profile", "sources", "claims"):
        if key not in packet:
            errors.append(f"root: missing {key}")

    if not is_non_empty_string(packet.get("request_id")):
        errors.append("root.request_id: must be a non-empty string")
    if not is_non_empty_string(packet.get("target_profile")):
        errors.append("root.target_profile: must be a non-empty string")

    sources = packet.get("sources")
    claims = packet.get("claims")
    if not isinstance(sources, list) or not sources:
        errors.append("root.sources: must be a non-empty list")
        sources = []
    if not isinstance(claims, list) or not claims:
        errors.append("root.claims: must be a non-empty list")
        claims = []

    source_ids: set[str] = set()
    for index, source in enumerate(sources):
        prefix = f"sources[{index}]"
        if not isinstance(source, dict):
            errors.append(f"{prefix}: must be an object")
            continue
        missing = sorted(REQUIRED_SOURCE - source.keys())
        if missing:
            errors.append(f"{prefix}: missing {', '.join(missing)}")
            continue
        source_id = source.get("source_id")
        if not is_non_empty_string(source_id):
            errors.append(f"{prefix}.source_id: must be a non-empty string")
        elif source_id in source_ids:
            errors.append(f"{prefix}.source_id: duplicate {source_id}")
        else:
            source_ids.add(source_id)
        for key in ("title", "publisher", "type", "published_at", "accessed_at", "locator", "excerpt", "interest_risk"):
            if not is_non_empty_string(source.get(key)):
                errors.append(f"{prefix}.{key}: must be a non-empty string")
        for key in ("can_prove", "cannot_prove"):
            if not is_string_list(source.get(key), allow_empty=False):
                errors.append(f"{prefix}.{key}: must be a non-empty string list")

    claim_ids: set[str] = set()
    for index, claim in enumerate(claims):
        prefix = f"claims[{index}]"
        if not isinstance(claim, dict):
            errors.append(f"{prefix}: must be an object")
            continue
        missing = sorted(REQUIRED_CLAIM - claim.keys())
        if missing:
            errors.append(f"{prefix}: missing {', '.join(missing)}")
            continue

        claim_id = claim.get("claim_id")
        if not is_non_empty_string(claim_id):
            errors.append(f"{prefix}.claim_id: must be a non-empty string")
        elif claim_id in claim_ids:
            errors.append(f"{prefix}.claim_id: duplicate {claim_id}")
        else:
            claim_ids.add(claim_id)

        for key in ("claim", "scope"):
            if not is_non_empty_string(claim.get(key)):
                errors.append(f"{prefix}.{key}: must be a non-empty string")

        status = claim.get("status")
        if status not in ALLOWED_CLAIM_STATUS:
            errors.append(
                f"{prefix}.status: must be one of {sorted(ALLOWED_CLAIM_STATUS)} "
                "(this reviewer cannot self-promote to verified)"
            )

        confidence = claim.get("confidence")
        if confidence not in ALLOWED_CONFIDENCE:
            errors.append(
                f"{prefix}.confidence: must be one of {sorted(ALLOWED_CONFIDENCE)}"
            )

        promotion_target = claim.get("promotion_target")
        if promotion_target not in ALLOWED_PROMOTION:
            errors.append(
                f"{prefix}.promotion_target: must be one of {sorted(ALLOWED_PROMOTION)}"
            )

        source_refs = claim.get("source_refs")
        if not is_string_list(source_refs, allow_empty=False):
            errors.append(f"{prefix}.source_refs: must be a non-empty string list")
            source_refs = []
        for ref in source_refs:
            if ref not in source_ids:
                errors.append(f"{prefix}.source_refs: unknown source {ref}")

        internal_refs = claim.get("internal_refs")
        if not is_string_list(internal_refs):
            errors.append(f"{prefix}.internal_refs: must be a string list")
            internal_refs = []

        if status in {"approved", "implemented"} and not internal_refs:
            errors.append(
                f"{prefix}: external-only evidence cannot be promoted above hypothesis/draft"
            )

        for key in ("limitations", "conflicts"):
            if not is_string_list(claim.get(key)):
                errors.append(f"{prefix}.{key}: must be a string list")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("packet", type=Path, help="JSON learning packet to validate")
    args = parser.parse_args()

    try:
        packet = json.loads(args.packet.read_text(encoding="utf-8"))
    except OSError as exc:
        print(f"ERROR: cannot read {args.packet}: {exc}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as exc:
        print(f"ERROR: invalid JSON: {exc}", file=sys.stderr)
        return 1

    errors = validate(packet)
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print(
        f"OK: {args.packet} "
        f"({len(packet['sources'])} sources, {len(packet['claims'])} claims)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
