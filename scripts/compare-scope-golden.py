#!/usr/bin/env python3
"""统计域门店维度「口径金标」规范化比对（P0B-02 方法，本批复用）。

用法: python3 scripts/compare-scope-golden.py <before 目录> <after 目录> [--dump 目录]

规范化规则（两侧一致施加，保证可比）：
  1. 递归删除易变时间字段：asOf / computedAt / generatedAt / calculatedAt / now / updatedAt；
  2. 数字 → "NUM:<去除末尾零的十进制>",保证 "75.0" 与 "75"、1e2 与 100 等价；
  3. 15 位以上纯数字字符串（雪花 ID）→ "<snowflake>"（夹具每次运行生成的 ID 必然不同）；
  4. 键排序后逐项比较；任一处不同即打印 JSON 指针路径。
退出码 0 = 全等；1 = 存在差异。
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

VOLATILE = {"asOf", "computedAt", "generatedAt", "calculatedAt", "now", "updatedAt"}


def normalize(node):
    if isinstance(node, dict):
        return {k: normalize(v) for k, v in sorted(node.items()) if k not in VOLATILE}
    if isinstance(node, list):
        return [normalize(v) for v in node]
    if isinstance(node, bool):
        return node
    if isinstance(node, (int, float)):
        return "NUM:" + num(node)
    if isinstance(node, str) and node.isdigit() and len(node) >= 15:
        return "<snowflake>"
    return node


def num(value) -> str:
    text = ("%f" % float(value)).rstrip("0").rstrip(".")
    return text if text not in ("", "-0") else "0"


def walk_diff(a, b, path="", out=None):
    out = out if out is not None else []
    if type(a) is not type(b):
        out.append("%s: 类型 %s != %s" % (path, type(a).__name__, type(b).__name__))
        return out
    if isinstance(a, dict):
        for key in sorted(set(a) | set(b)):
            if key not in a:
                out.append("%s.%s: 仅 after 有 = %r" % (path, key, b[key]))
            elif key not in b:
                out.append("%s.%s: 仅 before 有 = %r" % (path, key, a[key]))
            else:
                walk_diff(a[key], b[key], "%s.%s" % (path, key), out)
    elif isinstance(a, list):
        if len(a) != len(b):
            out.append("%s: 长度 %d != %d" % (path, len(a), len(b)))
        for i, (x, y) in enumerate(zip(a, b)):
            walk_diff(x, y, "%s[%d]" % (path, i), out)
    elif a != b:
        out.append("%s: %r != %r" % (path, a, b))
    return out


def main() -> int:
    before, after = Path(sys.argv[1]), Path(sys.argv[2])
    dump = None
    if "--dump" in sys.argv:
        dump = Path(sys.argv[sys.argv.index("--dump") + 1])
        dump.mkdir(parents=True, exist_ok=True)
    files = sorted(p.name for p in before.glob("*.json") if not p.name.endswith(".norm.json"))
    only_before = sorted(set(files) - {p.name for p in after.glob("*.json")})
    diffs, total = [], 0
    for name in files:
        target = after / name
        if not target.exists():
            diffs.append("%s: after 缺少该文件" % name)
            continue
        total += 1
        left = normalize(json.loads((before / name).read_text(encoding="utf-8")))
        right = normalize(json.loads(target.read_text(encoding="utf-8")))
        if dump:
            (dump / (name + ".norm.json")).write_text(
                json.dumps(left, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
            (dump / (name + ".after.norm.json")).write_text(
                json.dumps(right, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        file_diffs = walk_diff(left, right, "")
        if file_diffs:
            diffs.extend(["%s %s" % (name, d) for d in file_diffs])
    for name in only_before:
        diffs.append("%s: 仅 before 有" % name)
    print("比对文件数 %d；不一致 %d" % (total, len(diffs)))
    for line in diffs[:80]:
        print("  DIFF", line)
    print("口径金标 diff:", "全等 ✅" if not diffs else "存在差异 ❌")
    return 1 if diffs else 0


if __name__ == "__main__":
    sys.exit(main())
