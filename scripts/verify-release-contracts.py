#!/usr/bin/env python3
"""Reject release archives missing critical cross-client API contracts."""
from __future__ import annotations

import io
import json
import sys
import tarfile
import zipfile
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parent.parent
MANIFEST = ROOT_DIR / "docs/operations/release-contracts.json"


def fail(message: str) -> None:
    print(f"[release-contract] FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def load_boot_jar(archive: Path) -> tuple[str, bytes]:
    if not archive.is_file():
        fail(f"archive not found: {archive}")
    with tarfile.open(archive, "r:gz") as tar:
        boot_members = [
            member
            for member in tar.getmembers()
            if member.isfile()
            and "/boot/" in f"/{member.name}"
            and member.name.endswith(".jar")
            and not Path(member.name).name.startswith("._")
        ]
        if len(boot_members) != 1:
            fail(f"expected exactly one boot JAR, found {len(boot_members)}")
        member = boot_members[0]
        extracted = tar.extractfile(member)
        if extracted is None:
            fail(f"cannot read boot JAR: {member.name}")
        return member.name, extracted.read()


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: verify-release-contracts.py <release.tar.gz>")
    if not MANIFEST.is_file():
        fail(f"contract manifest not found: {MANIFEST}")

    try:
        contracts = json.loads(MANIFEST.read_text(encoding="utf-8"))["contracts"]
    except (KeyError, json.JSONDecodeError) as exc:
        fail(f"invalid contract manifest: {exc}")

    jar_name, jar_bytes = load_boot_jar(Path(sys.argv[1]))
    try:
        with zipfile.ZipFile(io.BytesIO(jar_bytes)) as jar:
            names = set(jar.namelist())
            problems: list[str] = []
            for contract in contracts:
                class_name = contract["class"]
                if class_name not in names:
                    problems.append(f"{contract['name']}: missing class {class_name}")
                    continue
                bytecode = jar.read(class_name)
                for required in contract["requiredStrings"]:
                    if required.encode("utf-8") not in bytecode:
                        problems.append(f"{contract['name']}: missing marker {required} in {class_name}")
            if problems:
                fail(f"{jar_name}: " + "; ".join(problems))
    except (KeyError, TypeError, zipfile.BadZipFile) as exc:
        fail(f"invalid boot JAR {jar_name}: {exc}")

    print(f"[release-contract] PASS: {len(contracts)} critical contracts in {jar_name}")


if __name__ == "__main__":
    main()
