#!/usr/bin/env python3
"""Reject Spring Boot repackaged application JARs in production release archives.

The th4 framework loads JWT audience classes from its own ClassLoader. A repackaged
Spring Boot JAR puts application classes under BOOT-INF/classes and splits them from
external lib/th4-framework.jar, causing auth/session/info to fail as tokenIsUndefined.
"""
from __future__ import annotations

import io
import sys
import tarfile
import zipfile
from pathlib import Path


def fail(message: str) -> None:
    print(f"[release-artifact] FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: verify-release-boot-jar.py <release.tar.gz>")

    archive = Path(sys.argv[1])
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
        jar_bytes = extracted.read()

    try:
        with zipfile.ZipFile(io.BytesIO(jar_bytes)) as jar:
            names = set(jar.namelist())
            manifest = jar.read("META-INF/MANIFEST.MF").decode("utf-8", errors="replace")
    except (KeyError, zipfile.BadZipFile) as exc:
        fail(f"invalid boot JAR {member.name}: {exc}")

    main_class = next(
        (line.removeprefix("Main-Class:").strip() for line in manifest.splitlines() if line.startswith("Main-Class:")),
        "",
    )
    problems: list[str] = []
    if main_class != "com.th4.edge.Application":
        problems.append(f"Main-Class must be com.th4.edge.Application, got {main_class or '<missing>'}")
    if any(name.startswith("BOOT-INF/classes/") for name in names):
        problems.append("must not contain BOOT-INF/classes (Spring Boot repackage artifact)")
    if "com/th4/edge/Application.class" not in names:
        problems.append("missing com/th4/edge/Application.class at JAR root")
    if "org/springframework/boot/loader/launch/PropertiesLauncher.class" in names:
        problems.append("must not contain Spring Boot loader launcher")

    if problems:
        fail("; ".join(problems))

    print(f"[release-artifact] PASS: thin boot JAR {member.name} ({len(jar_bytes)} bytes, Main-Class={main_class})")


if __name__ == "__main__":
    main()
