#!/usr/bin/env python3
"""Blind rubric screening for AI follow-up evaluation outputs."""
from __future__ import annotations

import argparse
import json
import re
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from pathlib import Path
from threading import Lock
from typing import Any


RUBRIC = """你是家装企业销售管理评审员。请只根据案例事实、期望动作和候选建议评分，不要猜测候选建议来自哪个模型。
评分必须严格，不要因为文字长就给高分。所有未在输入中出现、又被候选建议当成事实使用的价格、优惠、工期、案例、客户意愿，都要扣分。
总分100：事实准确性30；阶段与卡点判断20；动作可执行性20；客户个性化15；合规边界15。
hardFail只记录候选建议自身明确提出的违规行为，不把“不得/不能/避免”之类的正确约束算作违规。
只输出JSON对象：
{"factualAccuracy":0,"stageUnderstanding":0,"actionability":0,"personalization":0,"compliance":0,"total":0,"hardFail":[],"bestAction":"一句最值得保留的动作","mainProblem":"最大问题","rationale":"简短理由"}"""


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--results", required=True)
    p.add_argument("--cases", default="docs/ai-evaluation/cases.json")
    p.add_argument("--config", default="docs/ai-evaluation/config.json")
    p.add_argument("--profiles", default="flash-fast,pro-fast")
    p.add_argument("--out", default="")
    p.add_argument("--workers", type=int, default=6)
    return p.parse_args()


def parse_json(text: str) -> tuple[dict[str, Any] | None, str | None]:
    raw = (text or "").strip()
    raw = re.sub(r"^```(?:json)?\s*", "", raw)
    raw = re.sub(r"\s*```$", "", raw)
    try:
        value = json.loads(raw)
        return (value, None) if isinstance(value, dict) else (None, "judge response is not an object")
    except Exception as exc:
        start, end = raw.find("{"), raw.rfind("}")
        if start >= 0 and end > start:
            try:
                value = json.loads(raw[start:end + 1])
                return (value, None) if isinstance(value, dict) else (None, "judge response is not an object")
            except Exception as nested:
                return None, f"{type(exc).__name__}: {exc}; nested: {type(nested).__name__}: {nested}"
        return None, f"{type(exc).__name__}: {exc}"


def request_judge(config: dict[str, Any], key: str, case: dict[str, Any], candidate: dict[str, Any]) -> tuple[dict[str, Any] | None, dict[str, Any], str | None]:
    body = {
        "model": "deepseek-v4-pro",
        "messages": [
            {"role": "system", "content": RUBRIC},
            {"role": "user", "content": json.dumps({
                "case": {"customer": case["customer"], "timeline": case["timeline"], "task": case["task"], "expectedActions": case["expected"]},
                "candidate": candidate.get("parsed") or candidate.get("raw")
            }, ensure_ascii=False, indent=2)},
        ],
        "temperature": 0,
        "max_tokens": 1200,
        "thinking": {"type": "disabled"},
        "response_format": {"type": "json_object"},
    }
    req = urllib.request.Request(
        config["base_url"].rstrip("/") + "/chat/completions",
        data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=180) as response:
        data = json.load(response)
    choice = (data.get("choices") or [{}])[0]
    parsed, error = parse_json((choice.get("message") or {}).get("content") or "")
    return parsed, data.get("usage") or {}, error


def judge_one(config: dict[str, Any], key: str, case: dict[str, Any], candidate: dict[str, Any]) -> dict[str, Any]:
    started = time.perf_counter()
    parsed = usage = None
    error = None
    for attempt in range(3):
        try:
            parsed, usage, error = request_judge(config, key, case, candidate)
            if parsed is not None:
                break
        except urllib.error.HTTPError as exc:
            error = f"HTTP {exc.code}: {exc.read().decode('utf-8', 'ignore')[:600]}"
            if exc.code not in (408, 409, 429, 500, 502, 503, 504):
                break
        except Exception as exc:
            error = f"{type(exc).__name__}: {exc}"
        if attempt < 2:
            time.sleep(1.5 * (attempt + 1))
    return {
        "key": candidate["key"],
        "caseId": candidate["caseId"],
        "profile": candidate["profile"],
        "mode": candidate["mode"],
        "judgeModel": "deepseek-v4-pro",
        "latencyMs": round((time.perf_counter() - started) * 1000),
        "judge": parsed,
        "usage": usage or {},
        "error": error,
    }


def load_jsonl(path: Path) -> list[dict[str, Any]]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def summary(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    groups: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for row in rows:
        groups.setdefault((row["profile"], row["mode"]), []).append(row)
    result = []
    for (profile, mode), items in sorted(groups.items()):
        valid = [x["judge"] for x in items if isinstance(x.get("judge"), dict)]
        def avg(field: str) -> float:
            return round(sum(float(x.get(field) or 0) for x in valid) / len(valid), 2) if valid else 0
        result.append({
            "profile": profile, "mode": mode, "calls": len(items), "judgeValidRate": round(len(valid) / len(items), 4) if items else 0,
            "avgTotal": avg("total"), "avgFactual": avg("factualAccuracy"), "avgStage": avg("stageUnderstanding"),
            "avgActionability": avg("actionability"), "avgPersonalization": avg("personalization"), "avgCompliance": avg("compliance"),
            "hardFailRate": round(sum(1 for x in valid if x.get("hardFail")) / len(valid), 4) if valid else 0,
            "avgLatencyMs": round(sum(x["latencyMs"] for x in items) / len(items), 1) if items else 0,
        })
    return result


def main() -> None:
    args = parse_args()
    config = json.loads(Path(args.config).read_text(encoding="utf-8"))
    cases = {x["id"]: x for x in json.loads(Path(args.cases).read_text(encoding="utf-8"))["cases"]}
    candidates = [x for x in load_jsonl(Path(args.results)) if x["profile"] in set(args.profiles.split(",")) and x.get("parsed")]
    out_dir = Path(args.out) if args.out else Path(args.results).parent / "judge"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "judge-results.jsonl"
    existing: dict[str, dict[str, Any]] = {}
    if out_path.exists():
        existing = {x["key"]: x for x in load_jsonl(out_path)}
    key = Path(config["key_file"]).read_text(encoding="utf-8").strip()
    jobs = [x for x in candidates if x["key"] not in existing]
    print(json.dumps({"calls": len(jobs), "out": str(out_path)}, ensure_ascii=False))
    lock = Lock()
    with out_path.open("a", encoding="utf-8") as handle:
        with ThreadPoolExecutor(max_workers=max(1, args.workers)) as pool:
            futures = {pool.submit(judge_one, config, key, cases[x["caseId"]], x): x for x in jobs}
            done = 0
            for future in as_completed(futures):
                row = future.result()
                with lock:
                    handle.write(json.dumps(row, ensure_ascii=False) + "\n")
                    handle.flush()
                    existing[row["key"]] = row
                    done += 1
                    print(f"[{done}/{len(jobs)}] {row['key']} valid={bool(row.get('judge'))}", flush=True)
    rows = list(existing.values())
    result = summary(rows)
    (out_dir / "judge-summary.json").write_text(json.dumps({"summary": result, "records": len(rows)}, ensure_ascii=False, indent=2), encoding="utf-8")
    for row in result:
        print(f"{row['profile']:12} {row['mode']:9} n={row['calls']:2} valid={row['judgeValidRate']:.0%} total={row['avgTotal']:5.1f} fact={row['avgFactual']:5.1f} stage={row['avgStage']:5.1f} action={row['avgActionability']:5.1f} personal={row['avgPersonalization']:5.1f} compliance={row['avgCompliance']:5.1f} hard={row['hardFailRate']:.0%}")


if __name__ == "__main__":
    main()
