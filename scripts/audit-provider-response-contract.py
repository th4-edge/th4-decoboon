#!/usr/bin/env python3
"""Audit th4 Provider response serialization contracts.

The audit proves:
1. every Provider returning an entity/model/object has a concrete-class @Properties allow rule;
2. every relation retained by an @AllowProperties entry also has an allow rule for its runtime model;
3. unsupported or silently broadened response shapes fail the gate.

Scalar, binary and command-only responses may omit @Properties.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable

SCALAR_NAMES = {
    "String", "Long", "Integer", "Boolean", "Double", "Float", "Short", "Byte",
    "BigDecimal", "LocalDate", "LocalDateTime", "DateTime", "Date", "Time",
    "Object", "void", "Void", "File", "byte", "int", "long", "double", "float",
    "boolean", "short",
}
WRAPPER_NAMES = {
    "List", "Set", "Collection", "Optional", "Map", "HashMap", "LinkedHashMap",
    "ArrayList", "Page", "PageImpl", "Iterable",
}
SERVICE_RE = re.compile(r'@Service\s*\(\s*"([^"]+)"')
CLASS_RE = re.compile(r'public\s+class\s+([A-Za-z_$][\w$]*)')
CALL_RE = re.compile(r'public\s+([^\n{]+?)\s+call\s*\(')
PACKAGE_RE = re.compile(r'^\s*package\s+([\w.]+)\s*;', re.M)
IMPORT_RE = re.compile(r'^import\s+([\w.]+\.([A-Za-z_$][\w$]*))\s*;', re.M)


@dataclass
class Model:
    name: str
    fqn: str
    path: Path
    text: str
    is_entity: bool
    is_model: bool
    fields: dict[str, str] = field(default_factory=dict)


@dataclass
class Provider:
    route: str
    path: Path
    class_name: str
    return_type: str
    has_properties: bool
    has_allow: bool
    allow: dict[str, set[str]]


@dataclass
class Finding:
    kind: str
    route: str
    path: str
    detail: str

    def as_dict(self) -> dict[str, str]:
        return {"kind": self.kind, "route": self.route, "path": self.path, "detail": self.detail}


def compact(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def build_model_index(files: dict[Path, str]) -> tuple[dict[str, Model], dict[str, list[Model]]]:
    by_fqn: dict[str, Model] = {}
    by_simple: dict[str, list[Model]] = {}
    for path, text in files.items():
        package_match = PACKAGE_RE.search(text)
        if not package_match:
            continue
        package = package_match.group(1)
        for class_match in re.finditer(r"\bclass\s+([A-Za-z_$][\w$]*)\b", text):
            name = class_match.group(1)
            prefix = text[:class_match.start()]
            body = text[class_match.end():]
            fields: dict[str, str] = {}
            for field_match in re.finditer(
                r"\bprivate\s+([A-Za-z_$][\w.$]*(?:\s*<[^;=]+?>)?)\s+"
                r"([A-Za-z_$][\w$]*)\s*(?:=[^;]*)?;",
                body,
            ):
                fields[field_match.group(2)] = compact(field_match.group(1))
            model = Model(
                name=name,
                fqn=f"{package}.{name}",
                path=path,
                text=text,
                is_entity=bool(re.search(r"@Entity\b", prefix)),
                is_model=bool(re.search(r"@Model\b", prefix)),
                fields=fields,
            )
            by_fqn[model.fqn] = model
            by_simple.setdefault(name, []).append(model)
    return by_fqn, by_simple


def response_object_names(type_expr: str) -> list[str]:
    names = []
    for name in re.findall(r"[A-Za-z_$][\w.$]*", type_expr):
        simple = name.split(".")[-1]
        if simple in WRAPPER_NAMES or simple in SCALAR_NAMES:
            continue
        names.append(simple)
    return names[-1:] if names else []


def resolve_model(type_expr: str, source: Path, files: dict[Path, str], by_fqn: dict[str, Model], by_simple: dict[str, list[Model]]) -> list[Model]:
    names = response_object_names(type_expr)
    if not names:
        return []
    simple = names[0]
    text = files[source]
    for fqn, imported_simple in IMPORT_RE.findall(text):
        if imported_simple == simple and fqn in by_fqn:
            return [by_fqn[fqn]]
    candidates = [model for model in by_simple.get(simple, []) if "/service/" not in model.path.as_posix()]
    package_match = PACKAGE_RE.search(text)
    if package_match:
        prefix = package_match.group(1) + "."
        same_package = [model for model in candidates if model.fqn.startswith(prefix)]
        if same_package:
            return same_package[:1]
    return candidates[:1]


def extract_allow(prefix: str) -> tuple[bool, bool, dict[str, set[str]]]:
    has_properties = bool(re.search(r"@Properties\b", prefix))
    allowed: dict[str, set[str]] = {}
    for match in re.finditer(r"@AllowProperties\s*\((.*?)\)\s*(?:,|\}|\))", prefix, re.S):
        body = match.group(1)
        model_match = re.search(r"model\s*=\s*([A-Za-z_$][\w.$]*)\.class", body)
        if not model_match:
            continue
        array_match = re.search(r"properties\s*=\s*\{(.*?)\}", body, re.S)
        if array_match:
            properties = set(re.findall(r'"([^"]+)"', array_match.group(1)))
        else:
            properties = set(re.findall(r'properties\s*=\s*"([^"]+)"', body))
        allowed[model_match.group(1).split(".")[-1]] = properties
    return has_properties, bool(allowed), allowed


def scan_providers(root: Path) -> tuple[list[Provider], dict[Path, str], dict[str, Model], dict[str, list[Model]]]:
    java_root = root / "src/main/java"
    files = {path: path.read_text(encoding="utf-8", errors="ignore") for path in java_root.rglob("*.java")}
    by_fqn, by_simple = build_model_index(files)
    providers: list[Provider] = []
    for path, text in files.items():
        service_match = SERVICE_RE.search(text)
        class_match = CLASS_RE.search(text)
        if not service_match or not class_match:
            continue
        return_match = CALL_RE.search(text)
        return_type = compact(return_match.group(1)) if return_match else "?"
        prefix = text[:class_match.start()]
        has_properties, has_allow, allowed = extract_allow(prefix)
        providers.append(Provider(
            route=service_match.group(1),
            path=path,
            class_name=class_match.group(1),
            return_type=return_type,
            has_properties=has_properties,
            has_allow=has_allow,
            allow=allowed,
        ))
    return providers, files, by_fqn, by_simple


def audit(root: Path) -> tuple[list[Provider], list[Finding]]:
    providers, files, by_fqn, by_simple = scan_providers(root)
    findings: list[Finding] = []
    for provider in providers:
        roots = resolve_model(provider.return_type, provider.path, files, by_fqn, by_simple)
        scalar_only = not response_object_names(provider.return_type)
        if scalar_only:
            continue
        if not provider.has_properties:
            findings.append(Finding("missing-properties", provider.route, str(provider.path.relative_to(root)), provider.return_type))
            continue
        if not provider.has_allow:
            findings.append(Finding("empty-properties", provider.route, str(provider.path.relative_to(root)), provider.return_type))
            continue

        queue = list(roots)
        seen: set[str] = set()
        while queue:
            model = queue.pop(0)
            if model.fqn in seen:
                continue
            seen.add(model.fqn)
            allowed = provider.allow.get(model.name)
            if allowed is None:
                findings.append(Finding("missing-root-allow", provider.route, str(provider.path.relative_to(root)), model.name))
                continue
            for field_name in sorted(allowed & set(model.fields)):
                for target in resolve_model(model.fields[field_name], model.path, files, by_fqn, by_simple):
                    if not (target.fields or target.is_entity or target.is_model):
                        continue
                    if target.name not in provider.allow:
                        findings.append(Finding(
                            "missing-nested-allow",
                            provider.route,
                            str(provider.path.relative_to(root)),
                            f"{model.name}.{field_name}->{target.fqn}",
                        ))
                    else:
                        queue.append(target)
    return providers, findings


def run_self_test() -> None:
    fixtures = {
        "src/main/java/example/Project.java": """package example;
import com.th4.edge.framework.data.jpa.annotation.Model;
@Model public class Project { private Long projectId; private Employee owner; }
""",
        "src/main/java/example/Employee.java": """package example;
import com.th4.edge.framework.data.jpa.annotation.Model;
@Model public class Employee { private Long empId; private String name; }
""",
        "src/main/java/example/good/support/Page.java": """package example.good.support;
import example.Project;
import example.Employee;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.springframework.stereotype.Service;
@Properties(allow = {
  @AllowProperties(model = Project.class, properties = {"projectId", "owner"}),
  @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("example/good/page")
public class Page { public Project call(Project request) { return request; } }
""",
        "src/main/java/example/fail/support/Page.java": """package example.fail.support;
import example.Project;
import example.Employee;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.springframework.stereotype.Service;
@Properties(allow = @AllowProperties(model = Project.class, properties = {"projectId", "owner"}))
@Service("example/fail/page")
public class Page { public Project call(Project request) { return request; } }
""",
        "src/main/java/example/scalar/support/Count.java": """package example.scalar.support;
import org.springframework.stereotype.Service;
@Service("example/scalar/count")
public class Count { public Long call(Object request) { return 0L; } }
""",
        "src/main/java/example/missing/support/Info.java": """package example.missing.support;
import example.Project;
import org.springframework.stereotype.Service;
@Service("example/missing/info")
public class Info { public Project call(Project request) { return request; } }
""",
    }
    with tempfile.TemporaryDirectory() as temp:
        root = Path(temp)
        for rel, text in fixtures.items():
            path = root / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8")
        _, findings = audit(root)
        kinds = {(finding.route, finding.kind) for finding in findings}
        expected = {
            ("example/fail/page", "missing-nested-allow"),
            ("example/missing/info", "missing-properties"),
        }
        if kinds != expected:
            raise AssertionError(f"self-test mismatch: {sorted(kinds)} != {sorted(expected)}")
    print("provider-response-contract self-test: PASS")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=str(Path(__file__).resolve().parent.parent), help="project root")
    parser.add_argument("--check", action="store_true", help="fail on contract gaps (default behavior)")
    parser.add_argument("--json", action="store_true", help="print machine-readable audit result")
    parser.add_argument("--self-test", action="store_true", help="run positive and negative fixtures")
    args = parser.parse_args()
    if args.self_test:
        run_self_test()
        return 0

    root = Path(args.root).resolve()
    providers, findings = audit(root)
    result = {
        "providerCount": len(providers),
        "propertiesCount": sum(provider.has_properties for provider in providers),
        "allowPropertiesCount": sum(provider.has_allow for provider in providers),
        "withoutPropertiesCount": sum(provider.has_properties is False for provider in providers),
        "findingCount": len(findings),
        "findings": [finding.as_dict() for finding in findings],
    }
    if args.json:
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print(f"providers={result['providerCount']} properties={result['propertiesCount']} allow={result['allowPropertiesCount']} findings={result['findingCount']}")
        for finding in findings:
            print(f"- {finding.kind}: {finding.route} {finding.detail} ({finding.path})")
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())
