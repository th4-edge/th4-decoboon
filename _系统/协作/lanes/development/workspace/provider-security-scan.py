#!/usr/bin/env python3
"""Static Provider Security scan for th4-decoboon.

Usage: python3 provider-security-scan.py <backend-root> [output-csv]
"""

import csv
import pathlib
import re
import sys


PUBLIC_ROUTES = {
    "auth/session/login",
    "auth/session/password-login",
    "auth/session/password-setup",
    "auth/session/register",
    "auth/session/dev-owner-login",
    "identity/customer-invite/accept",
    "identity/customer-invite/info",
    "invite/invitation/accept",
    "invite/invitation/info",
    "invite/invitation/reject",
    "order/callback",
    "file/private/download",
    "platform/auth/login",
    "site/lead/submit",
    "site/lead/douyin/submit",
    "customer/shareview/info",
}


def scan(root: pathlib.Path):
    source = root / "src/main/java"
    rows = []
    for path in source.rglob("*.java"):
        text = path.read_text(errors="ignore")
        if '@Service("' not in text:
            continue
        relative = path.relative_to(source)
        if ".support." not in str(relative).replace("/", "."):
            continue
        parts = relative.with_suffix("").parts
        class_name = parts[-1]
        interface_parts = parts[:-1]
        if interface_parts and interface_parts[-1] == "support":
            interface_parts = interface_parts[:-1]
        interface = source.joinpath(*interface_parts, class_name + ".java")
        interface_text = interface.read_text(errors="ignore") if interface.exists() else ""
        for match in re.finditer(r'@Service\("([^"]+)"\)', text):
            route = match.group(1)
            security = "Security" in interface_text
            public = route in PUBLIC_ROUTES
            rows.append({
                "route": route,
                "security": "yes" if security else "no",
                "public_whitelist": "yes" if public else "no",
                "status": "protected" if security else ("public" if public else "violation"),
                "implementation": str(path.relative_to(root)),
                "contract": str(interface.relative_to(root)) if interface.exists() else "",
            })
    return sorted(rows, key=lambda row: row["route"])


def main():
    root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    output = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else None
    rows = scan(root)
    if output:
        output.parent.mkdir(parents=True, exist_ok=True)
        with output.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
            writer.writeheader()
            writer.writerows(rows)
    protected = sum(row["status"] == "protected" for row in rows)
    public = sum(row["status"] == "public" for row in rows)
    violations = [row for row in rows if row["status"] == "violation"]
    print(f"total={len(rows)} protected={protected} public={public} violations={len(violations)}")
    for row in violations:
        print(f"VIOLATION {row['route']} {row['implementation']}")
    return 1 if violations else 0


if __name__ == "__main__":
    raise SystemExit(main())
