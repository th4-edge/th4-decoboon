#!/usr/bin/env python3
"""Audit App/Web Provider usage against the single backend service catalog.

This is an inventory and drift gate, not a business approval registry. It proves:
1. every literal Provider route consumed by App/Web is declared by a backend @Service;
2. the observed App/Web client ownership of each route matches a checked-in baseline;
3. dynamic Provider call sites remain visible for manual review.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import tempfile
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

ROOT_DIR = Path(__file__).resolve().parent.parent
DEFAULT_FRONTEND_ROOT = ROOT_DIR.parent.parent / "前端代码"
BASELINE = ROOT_DIR / "docs/design/app-web-provider-contract.json"
REVIEWS = ROOT_DIR / "docs/design/app-web-provider-reviews.json"

SERVICE_RE = re.compile(r'@Service(?:Name)?\s*\(\s*"([^"]+)"\s*\)')
CALL_START_RE = re.compile(
    r"(?<![\w.])(post|get|put|upload|uploadRequest)\s*(?:<[^(\n]*>\s*)?\("
)
ROUTE_RE = re.compile(r"^[a-z][a-z0-9-]*(?:/[a-z0-9-]+)+$")
READ_LEAVES = {
    "page", "list", "info", "report", "summary", "overview", "count", "tree", "enums",
    "preview", "runtime", "health", "default", "workbench",
}
LITERAL_RE = re.compile(r"(['\"])([^'\"\n]+)\1")
DIRECT_TRANSPORT_RE = re.compile(r"(?<![\w.])(uni\.request|axios\.(?:request|get|post|put|delete))\s*\(")
HTTP_IMPORT_RE = re.compile(r"import\s*\{([^}]+)\}\s*from\s*['\"][^'\"]*core/http['\"]")
HTTP_METHODS = {"post", "get", "put", "upload"}
REVIEW_STATUSES = {
    "WEB_FIRST_APPROVED",
    "PLATFORM_SPECIFIC_APPROVED",
    "PARITY_REQUIRED",
    "ROUTE_MISMATCH",
    "WEB_FIRST_PENDING_REGISTRATION",
    "PENDING_REVIEW",
}
CLOSED_WEB_ONLY_STATUSES = {"WEB_FIRST_APPROVED", "PLATFORM_SPECIFIC_APPROVED"}


@dataclass(frozen=True)
class CallSite:
    client: str
    route: str
    callee: str
    file: str
    line: int


@dataclass(frozen=True)
class DynamicCallSite:
    client: str
    callee: str
    file: str
    line: int


@dataclass(frozen=True)
class DirectTransport:
    client: str
    marker: str
    file: str
    line: int


def line_number(text: str, index: int) -> int:
    return text.count("\n", 0, index) + 1


def relative(path: Path, root: Path) -> str:
    return path.relative_to(root).as_posix()


def parse_first_string_argument(text: str, open_paren: int) -> str | None:
    """Return the first argument when it is a plain string/template literal."""
    i = open_paren + 1
    while i < len(text) and text[i].isspace():
        i += 1
    if i >= len(text) or text[i] not in "'\"":
        return None
    quote = text[i]
    i += 1
    escaped = False
    chars: list[str] = []
    while i < len(text):
        char = text[i]
        if escaped:
            chars.append(char)
            escaped = False
        elif char == "\\":
            escaped = True
        elif char == quote:
            return "".join(chars)
        else:
            chars.append(char)
        i += 1
    return None


def domain_roots(client_root: Path, client: str) -> list[Path]:
    roots: list[Path] = []
    standard = client_root / "src/domains"
    if standard.is_dir():
        roots.append(standard)
    if client == "app":
        subpackages = client_root / "src/subpackages"
        if subpackages.is_dir():
            roots.extend(path for path in sorted(subpackages.glob("*/domains")) if path.is_dir())
    return roots


def iter_source_files(root: Path) -> Iterable[Path]:
    for path in sorted(root.rglob("*")):
        if path.is_file() and path.suffix in {".ts", ".vue"}:
            yield path


def imported_http_names(text: str) -> set[str]:
    """Return local aliases imported from the shared HTTP adapter."""
    names: set[str] = set()
    for match in HTTP_IMPORT_RE.finditer(text):
        for part in match.group(1).split(","):
            pieces = part.strip().split()
            if not pieces:
                continue
            original = pieces[0]
            local = pieces[2] if len(pieces) >= 3 and pieces[1] == "as" else original
            if original in HTTP_METHODS:
                names.add(local)
    return names


def strip_typescript_comments(text: str) -> str:
    """Blank comments while preserving newlines and character offsets."""
    def blank(match: re.Match[str]) -> str:
        return "".join("\n" if char == "\n" else " " for char in match.group(0))

    text = re.sub(r"/\*.*?\*/", blank, text, flags=re.S)
    return re.sub(r"//[^\n]*", blank, text)


def scan_backend_routes(root: Path) -> set[str]:
    routes: set[str] = set()
    java_root = root / "src/main/java"
    for path in sorted(java_root.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for match in SERVICE_RE.finditer(text):
            route = match.group(1).strip()
            if ROUTE_RE.fullmatch(route):
                routes.add(route)
    return routes


def scan_client(
    client_root: Path,
    client: str,
    backend_routes: set[str],
) -> tuple[list[CallSite], list[DynamicCallSite], list[DirectTransport]]:
    literal_calls: list[CallSite] = []
    dynamic_calls: list[DynamicCallSite] = []
    roots = domain_roots(client_root, client)
    for domain_root in roots:
        for path in iter_source_files(domain_root):
            text = path.read_text(encoding="utf-8", errors="ignore")
            rel = relative(path, client_root)
            http_names = imported_http_names(text)
            covered_routes: set[str] = set()
            for match in CALL_START_RE.finditer(text):
                callee = match.group(1)
                if callee not in http_names:
                    continue
                literal = parse_first_string_argument(text, match.end() - 1)
                if literal is not None and ROUTE_RE.fullmatch(literal.strip()):
                    route = literal.strip()
                    covered_routes.add(route)
                    literal_calls.append(
                        CallSite(client, route, callee, rel, line_number(text, match.start()))
                    )
                else:
                    dynamic_calls.append(
                        DynamicCallSite(client, callee, rel, line_number(text, match.start()))
                    )

            # Dynamic calls use variables such as `const uri = role ? 'a' : 'b'`.
            # Treat route-shaped string literals in HTTP-consuming files as reachable
            # Provider references so dynamic routing cannot silently become Web-only.
            if http_names:
                cleaned = strip_typescript_comments(text)
                for literal_match in LITERAL_RE.finditer(cleaned):
                    route = literal_match.group(2).strip()
                    if route in backend_routes and ROUTE_RE.fullmatch(route) and route not in covered_routes:
                        literal_calls.append(
                            CallSite(client, route, "routeLiteral", rel, line_number(cleaned, literal_match.start()))
                        )
                        covered_routes.add(route)

    direct: list[DirectTransport] = []
    src_root = client_root / "src"
    if src_root.is_dir():
        for path in iter_source_files(src_root):
            rel = relative(path, client_root)
            if rel == "src/core/http.ts" or any(rel.startswith(relative(root, client_root) + "/") for root in roots):
                continue
            text = path.read_text(encoding="utf-8", errors="ignore")
            for match in DIRECT_TRANSPORT_RE.finditer(text):
                direct.append(
                    DirectTransport(client, match.group(1), rel, line_number(text, match.start()))
                )
    return literal_calls, dynamic_calls, direct


def operation_kind(route: str) -> str:
    leaf = route.rsplit("/", 1)[-1].lower()
    return "read" if leaf in READ_LEAVES else "write"


def build_inventory(backend_root: Path, app_root: Path, web_root: Path) -> dict:
    backend_routes = scan_backend_routes(backend_root)
    app_literal, app_dynamic, app_direct = scan_client(app_root, "app", backend_routes)
    web_literal, web_dynamic, web_direct = scan_client(web_root, "web", backend_routes)

    by_route: dict[str, dict[str, set[str]]] = defaultdict(lambda: {"app": set(), "web": set()})
    for call in [*app_literal, *web_literal]:
        by_route[call.route][call.client].add(call.file)

    routes: list[dict] = []
    unmatched: list[dict] = []
    for route in sorted(by_route):
        app_files = sorted(by_route[route]["app"])
        web_files = sorted(by_route[route]["web"])
        classification = "shared" if app_files and web_files else ("app_only" if app_files else "web_only")
        backend_declared = route in backend_routes
        entry = {
            "route": route,
            "domain": route.split("/", 1)[0],
            "classification": classification,
            "operationKind": operation_kind(route),
            "backendDeclared": backend_declared,
            "appFiles": app_files,
            "webFiles": web_files,
        }
        routes.append(entry)
        if not backend_declared:
            unmatched.append(entry)

    dynamic = sorted(
        [
            {"client": item.client, "callee": item.callee, "file": item.file, "line": item.line}
            for item in [*app_dynamic, *web_dynamic]
        ],
        key=lambda item: (item["client"], item["file"], item["line"], item["callee"]),
    )
    direct = sorted(
        [
            {"client": item.client, "marker": item.marker, "file": item.file, "line": item.line}
            for item in [*app_direct, *web_direct]
        ],
        key=lambda item: (item["client"], item["file"], item["line"], item["marker"]),
    )

    summary = {
        "backendServiceRouteCount": len(backend_routes),
        "clientRouteCount": len(routes),
        "sharedRouteCount": sum(1 for item in routes if item["classification"] == "shared"),
        "appOnlyRouteCount": sum(1 for item in routes if item["classification"] == "app_only"),
        "webOnlyRouteCount": sum(1 for item in routes if item["classification"] == "web_only"),
        "webOnlyWriteRouteCount": sum(
            1 for item in routes
            if item["classification"] == "web_only" and item["operationKind"] == "write"
        ),
        "unmatchedClientRouteCount": len(unmatched),
        "dynamicCallSiteCount": len(dynamic),
        "directTransportCallCount": len(direct),
    }

    return {
        "schemaVersion": 1,
        "description": "Generated by scripts/audit-frontend-provider-contract.py; do not hand-edit.",
        "clients": {
            "app": {"routeCount": len({item.route for item in app_literal})},
            "web": {"routeCount": len({item.route for item in web_literal})},
        },
        "summary": summary,
        "routes": routes,
        "unmatchedClientRoutes": unmatched,
        "dynamicCallSites": dynamic,
        "directTransportCallsOutsideDomains": direct,
    }


def route_signatures(inventory: dict) -> set[tuple[str, str, str]]:
    return {
        (item["route"], item["classification"], item["operationKind"])
        for item in inventory["routes"]
    }


def dynamic_signatures(inventory: dict) -> set[tuple[str, str, str]]:
    return {
        (item["client"], item["callee"], item["file"])
        for item in inventory["dynamicCallSites"]
    }


def describe_route_signature(signature: tuple[str, str, str]) -> str:
    route, classification, kind = signature
    return f"{route} [{classification}/{kind}]"


def diff_inventory(baseline: dict, current: dict) -> list[str]:
    problems: list[str] = []
    if current["unmatchedClientRoutes"]:
        for item in current["unmatchedClientRoutes"]:
            clients = []
            if item["appFiles"]:
                clients.append("App")
            if item["webFiles"]:
                clients.append("Web")
            problems.append(f"client route not declared by backend: {item['route']} ({'/'.join(clients)})")

    old_routes = route_signatures(baseline)
    new_routes = route_signatures(current)
    for signature in sorted(old_routes - new_routes):
        problems.append(f"route removed or ownership changed: {describe_route_signature(signature)}")
    for signature in sorted(new_routes - old_routes):
        problems.append(f"route added or ownership changed: {describe_route_signature(signature)}")

    old_dynamic = dynamic_signatures(baseline)
    new_dynamic = dynamic_signatures(current)
    for signature in sorted(old_dynamic - new_dynamic):
        client, callee, file = signature
        problems.append(f"dynamic call removed: {client} {callee} in {file}")
    for signature in sorted(new_dynamic - old_dynamic):
        client, callee, file = signature
        problems.append(f"dynamic call added or moved: {client} {callee} in {file}")
    return problems


def print_summary(inventory: dict) -> None:
    summary = inventory["summary"]
    print(
        "[provider-contract] "
        f"backend={summary['backendServiceRouteCount']} "
        f"clients={summary['clientRouteCount']} "
        f"shared={summary['sharedRouteCount']} "
        f"app-only={summary['appOnlyRouteCount']} "
        f"web-only={summary['webOnlyRouteCount']} "
        f"web-only-write={summary['webOnlyWriteRouteCount']} "
        f"unmatched={summary['unmatchedClientRouteCount']} "
        f"dynamic={summary['dynamicCallSiteCount']} "
        f"direct-transport={summary['directTransportCallCount']}"
    )

    if inventory["directTransportCallsOutsideDomains"]:
        print("[provider-contract] direct transport outside domain APIs:", file=sys.stderr)
        for item in inventory["directTransportCallsOutsideDomains"]:
            print(
                f"  - {item['client']} {item['marker']} {item['file']}:{item['line']}",
                file=sys.stderr,
            )


def validate_reviews(inventory: dict, review_doc: dict) -> tuple[list[str], dict[str, int], list[str]]:
    """Validate Web-only write review coverage without auto-approving business decisions."""
    problems: list[str] = []
    reviews = review_doc.get("reviews")
    if not isinstance(reviews, list):
        return ["reviews must be an array"], {}, []

    current_web_only_writes = {
        item["route"]
        for item in inventory["routes"]
        if item["classification"] == "web_only" and item["operationKind"] == "write"
    }
    seen: set[str] = set()
    counts: dict[str, int] = {}
    open_routes: list[str] = []
    for item in reviews:
        if not isinstance(item, dict):
            problems.append("review entry must be an object")
            continue
        route = str(item.get("route") or "").strip()
        status = str(item.get("status") or "").strip()
        if not route:
            problems.append("review entry missing route")
            continue
        if route in seen:
            problems.append(f"duplicate review entry: {route}")
        seen.add(route)
        if status not in REVIEW_STATUSES:
            problems.append(f"invalid review status for {route}: {status}")
        if not str(item.get("rationale") or "").strip():
            problems.append(f"review rationale missing: {route}")
        evidence = item.get("evidence")
        if not isinstance(evidence, list) or not [value for value in evidence if str(value).strip()]:
            problems.append(f"review evidence missing: {route}")
        if not str(item.get("nextAction") or "").strip():
            problems.append(f"review nextAction missing: {route}")
        if route not in current_web_only_writes:
            problems.append(f"stale review entry (route is no longer Web-only write): {route}")
        counts[status] = counts.get(status, 0) + 1
        if status not in CLOSED_WEB_ONLY_STATUSES:
            open_routes.append(route)

    for route in sorted(current_web_only_writes - seen):
        problems.append(f"unreviewed Web-only write route: {route}")
    return problems, counts, sorted(open_routes)


def print_review_summary(counts: dict[str, int], open_routes: list[str]) -> None:
    total = sum(counts.values())
    closed = sum(counts.get(status, 0) for status in CLOSED_WEB_ONLY_STATUSES)
    print(f"[provider-contract] reviews={total} approved={closed} open={len(open_routes)}")
    for status in sorted(counts):
        print(f"  - {status}: {counts[status]}")


def write_baseline(inventory: dict) -> None:
    if inventory["unmatchedClientRoutes"]:
        for item in inventory["unmatchedClientRoutes"]:
            print(f"[provider-contract] FAIL: {item['route']} is not declared by backend", file=sys.stderr)
        raise SystemExit(1)
    BASELINE.parent.mkdir(parents=True, exist_ok=True)
    BASELINE.write_text(
        json.dumps(inventory, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"[provider-contract] baseline written: {BASELINE}")


def resolve_frontend_root(value: str | None) -> Path:
    configured = value or os.environ.get("DECOBOON_FRONTEND_ROOT")
    root = Path(configured).expanduser().resolve() if configured else DEFAULT_FRONTEND_ROOT.resolve()
    if not (root / "th4-decoboon-app").is_dir():
        raise SystemExit(f"[provider-contract] App repo not found under: {root}")
    if not (root / "th4-decoboon-web").is_dir():
        raise SystemExit(f"[provider-contract] Web repo not found under: {root}")
    return root


def run_self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="provider-contract-") as temp:
        base = Path(temp)
        backend = base / "backend"
        app = base / "app"
        web = base / "web"
        (backend / "src/main/java/demo").mkdir(parents=True)
        (app / "src/domains/demo").mkdir(parents=True)
        (web / "src/domains/demo").mkdir(parents=True)
        (backend / "src/main/java/demo/Demo.java").write_text(
            '@Service("demo/item/page")\nclass Demo {}\n', encoding="utf-8"
        )
        (app / "src/domains/demo/api.ts").write_text(
            "import { post } from '@/core/http'\npost<PageResult<Record<string, unknown>>>('demo/item/page', {})\n",
            encoding="utf-8",
        )
        (web / "src/domains/demo/api.ts").write_text(
            "import { post } from '@/core/http'\nexport const x = post<unknown>('demo/item/page', {})\n",
            encoding="utf-8",
        )
        inventory = build_inventory(backend, app, web)
        assert inventory["summary"]["sharedRouteCount"] == 1, inventory["summary"]
        assert inventory["summary"]["unmatchedClientRouteCount"] == 0
        assert not diff_inventory(inventory, inventory)

        (web / "src/domains/demo/api.ts").write_text(
            "import { post } from '@/core/http'\nexport const x = post<unknown>('demo/missing/page', {})\n",
            encoding="utf-8",
        )
        missing = build_inventory(backend, app, web)
        assert missing["summary"]["unmatchedClientRouteCount"] == 1
        assert diff_inventory(inventory, missing)

        (web / "src/domains/demo/api.ts").write_text(
            "import { post } from '@/core/http'\nconst uri = 'demo/item/page'\npost(uri, {})\n",
            encoding="utf-8",
        )
        dynamic = build_inventory(backend, app, web)
        assert dynamic["summary"]["dynamicCallSiteCount"] == 1
        assert diff_inventory(inventory, dynamic)

        (backend / "src/main/java/demo/Delete.java").write_text(
            '@Service("demo/item/delete")\nclass Delete {}\n', encoding="utf-8"
        )
        (web / "src/domains/demo/api.ts").write_text(
            "import { post } from '@/core/http'\npost('demo/item/delete', {})\n", encoding="utf-8"
        )
        web_only = build_inventory(backend, app, web)
        assert web_only["summary"]["webOnlyWriteRouteCount"] == 1
        review_problems, _, _ = validate_reviews(web_only, {"reviews": []})
        assert review_problems
        approved_review = {
            "reviews": [{
                "route": "demo/item/delete",
                "status": "WEB_FIRST_APPROVED",
                "rationale": "fixture",
                "evidence": ["fixture decision"],
                "nextAction": "none",
            }]
        }
        review_problems, counts, open_routes = validate_reviews(web_only, approved_review)
        assert not review_problems, review_problems
        assert counts["WEB_FIRST_APPROVED"] == 1
        assert not open_routes
    print("[provider-contract] SELF-TEST PASS")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true", help="write/update the checked-in baseline")
    parser.add_argument("--check", action="store_true", help="check current routes against the baseline")
    parser.add_argument("--self-test", action="store_true", help="run negative fixture tests")
    parser.add_argument(
        "--strict-web-only",
        action="store_true",
        help="fail when any Web-only write route still has open review status",
    )
    parser.add_argument("--frontend-root", help="directory containing th4-decoboon-app and th4-decoboon-web")
    args = parser.parse_args()

    if args.self_test:
        run_self_test()
        if not (args.write or args.check):
            return

    frontend_root = resolve_frontend_root(args.frontend_root)
    inventory = build_inventory(ROOT_DIR, frontend_root / "th4-decoboon-app", frontend_root / "th4-decoboon-web")
    print_summary(inventory)

    if args.write:
        write_baseline(inventory)
        return

    if not BASELINE.is_file():
        raise SystemExit(f"[provider-contract] FAIL: baseline missing: {BASELINE}")

    baseline = json.loads(BASELINE.read_text(encoding="utf-8"))
    problems = diff_inventory(baseline, inventory)
    if problems:
        print("[provider-contract] FAIL: contract drift detected", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        print(
            "[provider-contract] review the route/ownership change, then run with --write and commit the baseline.",
            file=sys.stderr,
        )
        raise SystemExit(1)
    print("[provider-contract] PASS: client routes match checked-in baseline")

    if not REVIEWS.is_file():
        raise SystemExit(f"[provider-contract] FAIL: review registry missing: {REVIEWS}")
    review_doc = json.loads(REVIEWS.read_text(encoding="utf-8"))
    review_problems, review_counts, open_routes = validate_reviews(inventory, review_doc)
    print_review_summary(review_counts, open_routes)
    if review_problems:
        print("[provider-contract] FAIL: Web-only review registry is invalid", file=sys.stderr)
        for problem in review_problems:
            print(f"  - {problem}", file=sys.stderr)
        raise SystemExit(1)
    if args.strict_web_only and open_routes:
        print("[provider-contract] FAIL: open Web-only review debt remains", file=sys.stderr)
        for route in open_routes:
            print(f"  - {route}", file=sys.stderr)
        raise SystemExit(1)
    if not args.strict_web_only:
        print("[provider-contract] PASS: Web-only review registry coverage is complete (open debt reported, not blocking)")


if __name__ == "__main__":
    main()
