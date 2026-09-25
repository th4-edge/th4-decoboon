#!/usr/bin/env python3
"""DeepSeek follow-up coach evaluation harness.

Reads the local API key from a file, runs a deterministic case matrix, persists
one JSONL record per call, and generates technical, blind-review and CSV outputs.
The key is never written to the output directory.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import html
import json
import re
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path
from threading import Lock
from typing import Any


OUTPUT_FIELDS = [
    "stageDiagnosis",
    "priorityReason",
    "goal",
    "questions",
    "talkTrack",
    "objectionHandling",
    "recommendedMaterials",
    "forbidden",
    "nextFollowAt",
    "nextAction",
    "evidenceRefs",
]

OUTPUT_SCHEMA = """{
  "stageDiagnosis": "客户当前阶段和真实卡点",
  "priorityReason": "为什么今天应优先处理",
  "goal": "本次沟通唯一主目标",
  "questions": ["最多3个关键问题"],
  "talkTrack": ["可直接改写使用的沟通话术，2到3条"],
  "objectionHandling": [{"ifCustomerSays": "客户可能的异议", "reply": "有事实依据的回应"}],
  "recommendedMaterials": ["建议发送的正式材料、报价或已授权案例"],
  "forbidden": ["本次沟通不得承诺或不得表达的内容"],
  "nextFollowAt": "建议下次跟进时间，无法确认时说明条件",
  "nextAction": "业务员沟通后必须完成的动作",
  "evidenceRefs": ["引用的知识编号或客户事实字段"]
}"""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cases", default="docs/ai-evaluation/cases.json")
    parser.add_argument("--config", default="docs/ai-evaluation/config.json")
    parser.add_argument("--out", default="")
    parser.add_argument("--workers", type=int, default=6)
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--profiles", default="")
    parser.add_argument("--modes", default="")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def load_inputs(args: argparse.Namespace) -> tuple[dict[str, Any], dict[str, Any]]:
    cases = json.loads(Path(args.cases).read_text(encoding="utf-8"))
    config = json.loads(Path(args.config).read_text(encoding="utf-8"))
    return cases, config


def selected(names: str, available: dict[str, Any]) -> list[str]:
    if not names.strip():
        return list(available)
    requested = [name.strip() for name in names.split(",") if name.strip()]
    unknown = [name for name in requested if name not in available]
    if unknown:
        raise SystemExit(f"Unknown names: {', '.join(unknown)}")
    return requested


def knowledge_text(knowledge: list[dict[str, Any]]) -> str:
    lines = []
    for item in knowledge:
        lines.append(f"[{item['id']}] {item['title']}：{item['content']}")
    return "\n".join(lines)


def playbook_text(playbooks: list[dict[str, Any]]) -> str:
    lines = []
    for item in playbooks:
        lines.append(
            f"[{item['id']}] 场景={item['scene']}；目标={item['objective']}；"
            f"步骤={'；'.join(item['steps'])}；禁忌={'；'.join(item['forbidden'])}"
        )
    return "\n".join(lines)


def build_messages(case: dict[str, Any], mode: str, corpus: dict[str, Any]) -> list[dict[str, str]]:
    common = (
        "你是家装企业的业务员跟进教练。请基于输入中的客户事实生成可执行建议。"
        "你只能把输入中明确出现的事实当作已确认事实，缺失信息必须标为待确认。"
        "输出必须是合法JSON对象，不要Markdown，不要JSON代码围栏，不要添加schema之外的字段。"
        f"输出格式：{OUTPUT_SCHEMA}"
    )
    if mode == "generic":
        system = "你是装修公司的销售助理，请给业务员一份专业、具体、可执行的客户跟进建议。" + common
    elif mode == "facts":
        system = (
            common
            + "必须优先使用客户阶段、跟进时间、量房、设计、报价和项目状态。"
            + "不得编造价格、优惠、工期、客户意愿或案例。金额只能引用输入中的正式报价事实。"
        )
    elif mode == "knowledge":
        system = (
            common
            + "必须优先使用客户事实和企业知识。企业知识是引用依据，不是可以随意扩写的营销承诺。"
            + "不得编造价格、优惠、工期、客户意愿或案例。金额只能引用输入中的正式报价事实。\n"
            + "【企业知识】\n" + knowledge_text(corpus["knowledge"])
        )
    elif mode == "full":
        system = (
            common
            + "先做阶段判断，再按销售剧本选择动作。企业知识用于约束表达，客户事实决定具体建议。"
            + "不得编造价格、优惠、工期、客户意愿或案例。金额只能引用输入中的正式报价事实。"
            + "优先给出今天能立即执行的一个主目标、最多三个问题和一个明确下一步。\n"
            + "【企业知识】\n" + knowledge_text(corpus["knowledge"])
            + "\n【销售剧本】\n" + playbook_text(corpus["playbooks"])
            + "\n【确定性规则】\n"
            + "1. 报价未正式提交时，不得讨论最终价格；2. 业务员不得自行改价或承诺折扣；"
            + "3. 量房后必须明确设计师交接和下一次反馈节点；4. 签约后施工事实转交项目经理；"
            + "5. 所有案例和材料必须已授权；6. 无法确认的信息必须明确写待确认。"
        )
    else:
        raise ValueError(f"Unsupported mode: {mode}")

    model_case = {
        "id": case["id"],
        "family": case["family"],
        "stage": case["stage"],
        "customer": case["customer"],
        "timeline": case["timeline"],
        "task": case["task"],
        "evaluation_date": "2026-09-26",
    }
    user = (
        "下面是测试案例。请只输出JSON对象。evidenceRefs只引用已经出现的知识编号或客户事实字段。\n"
        + json.dumps(model_case, ensure_ascii=False, indent=2)
    )
    return [{"role": "system", "content": system}, {"role": "user", "content": user}]


def request_chat(config: dict[str, Any], key: str, profile_name: str, messages: list[dict[str, str]]) -> dict[str, Any]:
    profile = config["profiles"][profile_name]
    body: dict[str, Any] = {
        "model": profile["model"],
        "messages": messages,
        "temperature": 1.0 if profile.get("thinking") == "enabled" else config.get("temperature", 0.2),
        "max_tokens": profile.get("max_tokens", 1800),
        "response_format": {"type": "json_object"},
        "thinking": {"type": profile.get("thinking", "disabled")},
    }
    if profile.get("reasoning_effort"):
        body["reasoning_effort"] = profile["reasoning_effort"]
    payload = json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(
        config["base_url"].rstrip("/") + "/chat/completions",
        data=payload,
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=180) as response:
        return json.load(response)


def parse_json_content(content: str) -> tuple[Any, str | None]:
    text = (content or "").strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?\s*", "", text)
        text = re.sub(r"\s*```$", "", text)
    try:
        return json.loads(text), None
    except Exception as first_error:
        start = text.find("{")
        end = text.rfind("}")
        if start >= 0 and end > start:
            try:
                return json.loads(text[start:end + 1]), None
            except Exception as nested_error:
                return None, f"{type(first_error).__name__}: {first_error}; nested: {type(nested_error).__name__}: {nested_error}"
        return None, f"{type(first_error).__name__}: {first_error}"


def as_text(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False)


def _customer_facing_text(parsed: dict[str, Any]) -> str:
    chunks: list[str] = []
    for field in ("goal", "talkTrack", "recommendedMaterials", "nextAction"):
        value = parsed.get(field)
        if isinstance(value, list):
            chunks.extend(as_text(item) for item in value)
        else:
            chunks.append(as_text(value))
    objections = parsed.get("objectionHandling")
    if isinstance(objections, list):
        for item in objections:
            if isinstance(item, dict):
                chunks.append(as_text(item.get("reply")))
            else:
                chunks.append(as_text(item))
    # Remove negated clauses before flagging. Statements such as
    # "不得承诺最低价" are compliance guidance, not an actual violation.
    clauses = re.split(r"[。！？；\n]+", "\n".join(chunks))
    negative = re.compile(r"(不得|不能|禁止|严禁|不要|避免|不承诺|不允许|不会|并非|不是|无需|没有依据)")
    return "\n".join(clause for clause in clauses if clause.strip() and not negative.search(clause))


def automatic_score(parsed: Any, raw: str) -> dict[str, Any]:
    if not isinstance(parsed, dict):
        return {
            "schema_completeness": 0.0,
            "evidence_count": 0,
            "question_count": 0,
            "has_next_action": False,
            "heuristic_score": 0.0,
            "hard_fail": [] if not raw else ["未返回合法JSON"],
        }
    present = 0
    for field in OUTPUT_FIELDS:
        value = parsed.get(field)
        if isinstance(value, list):
            present += 1 if value else 0
        else:
            present += 1 if value not in (None, "", [], {}) else 0
    schema = present / len(OUTPUT_FIELDS)
    questions = parsed.get("questions") if isinstance(parsed.get("questions"), list) else []
    refs = parsed.get("evidenceRefs") if isinstance(parsed.get("evidenceRefs"), list) else []
    next_action = as_text(parsed.get("nextAction")).strip()
    joined = _customer_facing_text(parsed)
    patterns = {
        "绝对承诺": r"(绝对|100%|百分之百|一定能|肯定能|保证.{0,8}(工期|完工|质量|环保|不超|省钱|最低))",
        "未经授权价格承诺": r"(给你便宜|直接少|私下优惠|内部价|最低价|保证打折|做到同价)",
        "成本或利润暴露": r"(成本价|毛利|利润)",
        "无依据免费设计": r"免费设计|设计不收费",
    }
    hard_fail = [label for label, pattern in patterns.items() if re.search(pattern, joined)]
    score = schema * 40
    score += min(len(refs), 2) / 2 * 20
    score += min(len(questions), 3) / 3 * 15
    score += 15 if next_action else 0
    score += 0 if hard_fail else 10
    return {
        "schema_completeness": round(schema, 4),
        "evidence_count": len(refs),
        "question_count": len(questions),
        "has_next_action": bool(next_action),
        "heuristic_score": round(score, 2),
        "hard_fail": hard_fail,
    }

def call_cost(pricing: dict[str, float], usage: dict[str, Any], peak: bool) -> float:
    hit = int(usage.get("prompt_cache_hit_tokens") or 0)
    miss = int(usage.get("prompt_cache_miss_tokens") or usage.get("prompt_tokens") or 0)
    output = int(usage.get("completion_tokens") or 0)
    prefix = "peak" if peak else "offpeak"
    return (
        hit / 1_000_000 * pricing[f"{prefix}_cache_hit"]
        + miss / 1_000_000 * pricing[f"{prefix}_cache_miss"]
        + output / 1_000_000 * pricing[f"{prefix}_output"]
    )


def work_key(case_id: str, profile: str, mode: str) -> str:
    return f"{case_id}|{profile}|{mode}"


def run_one(config: dict[str, Any], key: str, case: dict[str, Any], profile_name: str, mode: str, corpus: dict[str, Any]) -> dict[str, Any]:
    messages = build_messages(case, mode, corpus)
    started = time.perf_counter()
    error: str | None = None
    data: dict[str, Any] | None = None
    for attempt in range(3):
        try:
            data = request_chat(config, key, profile_name, messages)
            break
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", "ignore")[:1000]
            error = f"HTTP {exc.code}: {detail}"
            if exc.code not in (408, 409, 429, 500, 502, 503, 504):
                break
        except Exception as exc:  # network and timeout errors are retried
            error = f"{type(exc).__name__}: {exc}"
        if attempt < 2:
            time.sleep(1.5 * (attempt + 1))
    elapsed_ms = round((time.perf_counter() - started) * 1000)
    if data is None:
        return {
            "key": work_key(case["id"], profile_name, mode), "caseId": case["id"], "profile": profile_name,
            "mode": mode, "model": config["profiles"][profile_name]["model"], "ok": False,
            "latencyMs": elapsed_ms, "error": error, "raw": "", "parsed": None, "usage": {},
            "cost": {"peakUsd": 0.0, "offpeakUsd": 0.0}, "score": automatic_score(None, "")
        }
    choice = (data.get("choices") or [{}])[0]
    message = choice.get("message") or {}
    content = message.get("content") or ""
    parsed, parse_error = parse_json_content(content)
    usage = data.get("usage") or {}
    pricing = config["pricing_per_million_usd"][config["profiles"][profile_name]["model"]]
    return {
        "key": work_key(case["id"], profile_name, mode), "caseId": case["id"], "profile": profile_name,
        "mode": mode, "model": data.get("model") or config["profiles"][profile_name]["model"],
        "ok": parsed is not None, "latencyMs": elapsed_ms, "error": parse_error,
        "finishReason": choice.get("finish_reason"), "raw": content, "parsed": parsed, "usage": usage,
        "reasoningContent": message.get("reasoning_content"),
        "cost": {"peakUsd": call_cost(pricing, usage, True), "offpeakUsd": call_cost(pricing, usage, False)},
        "score": automatic_score(parsed, content),
    }


def load_existing(path: Path) -> dict[str, dict[str, Any]]:
    existing: dict[str, dict[str, Any]] = {}
    if not path.exists():
        return existing
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        item = json.loads(line)
        existing[item["key"]] = item
    return existing


def summarise(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    groups: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for item in records:
        groups.setdefault((item["profile"], item["mode"]), []).append(item)
    rows = []
    for (profile, mode), items in sorted(groups.items()):
        total = len(items)
        prompt_tokens = sum(int((x.get("usage") or {}).get("prompt_tokens") or 0) for x in items)
        output_tokens = sum(int((x.get("usage") or {}).get("completion_tokens") or 0) for x in items)
        reasoning_tokens = sum(int(((x.get("usage") or {}).get("completion_tokens_details") or {}).get("reasoning_tokens") or 0) for x in items)
        rows.append({
            "profile": profile,
            "mode": mode,
            "calls": total,
            "jsonValidRate": round(sum(1 for x in items if x.get("ok")) / total, 4) if total else 0,
            "avgLatencyMs": round(sum(int(x.get("latencyMs") or 0) for x in items) / total, 1) if total else 0,
            "avgHeuristicScore": round(sum(float((x.get("score") or {}).get("heuristic_score") or 0) for x in items) / total, 2) if total else 0,
            "hardFailRate": round(sum(1 for x in items if (x.get("score") or {}).get("hard_fail")) / total, 4) if total else 0,
            "avgEvidenceCount": round(sum(int((x.get("score") or {}).get("evidence_count") or 0) for x in items) / total, 2) if total else 0,
            "promptTokens": prompt_tokens,
            "outputTokens": output_tokens,
            "reasoningTokens": reasoning_tokens,
            "peakUsd": round(sum(float((x.get("cost") or {}).get("peakUsd") or 0) for x in items), 6),
            "offpeakUsd": round(sum(float((x.get("cost") or {}).get("offpeakUsd") or 0) for x in items), 6),
        })
    return rows


def blind_id(case_id: str, profile: str, mode: str) -> str:
    digest = hashlib.sha1(f"{case_id}|{profile}|{mode}".encode()).hexdigest()[:10].upper()
    return f"BL-{digest}"


def render_html(cases: list[dict[str, Any]], records: list[dict[str, Any]], summary: list[dict[str, Any]], blind: bool) -> str:
    by_case: dict[str, list[dict[str, Any]]] = {}
    labels = {"profile": {}, "mode": {}}
    for item in records:
        by_case.setdefault(item["caseId"], []).append(item)
        labels["profile"][item["profile"]] = item["profile"]
        labels["mode"][item["mode"]] = item["mode"]
    title = "盲评预览" if blind else "DeepSeek AI 跟进教练效果评测"
    summary_html = ""
    if not blind:
        rows = []
        for row in summary:
            rows.append(
                "<tr>"
                f"<td>{html.escape(row['profile'])}</td><td>{html.escape(row['mode'])}</td>"
                f"<td>{row['calls']}</td><td>{row['jsonValidRate']:.0%}</td>"
                f"<td>{row['avgHeuristicScore']}</td><td>{row['hardFailRate']:.0%}</td>"
                f"<td>{row['avgEvidenceCount']}</td><td>{row['avgLatencyMs'] / 1000:.2f}s</td>"
                f"<td>{row['promptTokens']}</td><td>{row['outputTokens']}</td><td>{row['reasoningTokens']}</td>"
                f"<td>${row['peakUsd']:.4f}</td><td>${row['offpeakUsd']:.4f}</td>"
                "</tr>"
            )
        summary_html = (
            "<h2>组合汇总</h2><table><thead><tr>"
            "<th>模型档位</th><th>上下文</th><th>调用</th><th>JSON有效</th><th>启发式分</th><th>硬失败率</th>"
            "<th>引用数</th><th>平均耗时</th><th>输入Token</th><th>输出Token</th><th>思考Token</th><th>峰值成本</th><th>非峰值成本</th>"
            "</tr></thead><tbody>" + "".join(rows) + "</tbody></table>"
        )
    case_html = []
    for case in cases:
        items = sorted(by_case.get(case["id"], []), key=lambda x: (x["profile"], x["mode"]))
        outputs = []
        for item in items:
            if blind:
                head = f"盲评编号 {blind_id(item['caseId'], item['profile'], item['mode'])}"
            else:
                head = f"{item['profile']} / {item['mode']}"
            parsed = item.get("parsed")
            rendered = html.escape(json.dumps(parsed, ensure_ascii=False, indent=2)) if parsed is not None else html.escape(item.get("raw") or item.get("error") or "")
            flags = item.get("score", {}).get("hard_fail") or []
            outputs.append(
                "<article class='out'>"
                f"<header><b>{html.escape(head)}</b><span>{html.escape(item['model'])} · {item['latencyMs'] / 1000:.2f}s · ${item['cost']['peakUsd']:.5f} peak</span></header>"
                f"<div class='flags'>{'<span>'+'</span><span>'.join(html.escape(x) for x in flags)+'</span>' if flags else '<span class=ok>无自动硬失败</span>'}</div>"
                f"<pre>{rendered}</pre></article>"
            )
        case_html.append(
            f"<section class='case'><h2>{html.escape(case['id'])} · {html.escape(case['family'])} · {html.escape(case['stage'])}</h2>"
            f"<pre class='input'>{html.escape(json.dumps({'customer':case['customer'],'timeline':case['timeline'],'task':case['task']},ensure_ascii=False,indent=2))}</pre>"
            + "".join(outputs) + "</section>"
        )
    return f"""<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{title}</title><style>
body{{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;margin:0;background:#f5f7fb;color:#182230}}
main{{max-width:1480px;margin:0 auto;padding:28px}}h1{{font-size:26px}}h2{{font-size:17px;margin:24px 0 12px}}
table{{width:100%;border-collapse:collapse;background:white;border-radius:12px;overflow:hidden;font-size:12px}}th,td{{padding:8px;border-bottom:1px solid #e8edf5;text-align:right}}th:first-child,td:first-child,th:nth-child(2),td:nth-child(2){{text-align:left}}th{{background:#edf2fa;color:#42526e}}
.case{{margin-top:26px}}.input,.out pre{{white-space:pre-wrap;overflow:auto;background:#101828;color:#e9f1ff;border-radius:10px;padding:14px;font-size:12px;line-height:1.55}}
.out{{background:white;border:1px solid #e4e9f2;border-radius:12px;padding:00;margin:10px 0}}.out header{{display:flex;justify-content:space-between;gap:16px;padding:12px 14px;border-bottom:1px solid #e4e9f2;font-size:12px}}.out pre{{border-radius:0;margin:0;background:#fff;color:#182230}}.flags{{padding:9px 14px;color:#b42318;display:flex;gap:8px;flex-wrap:wrap;font-size:12px}}.flags span{{background:#fee4e2;padding:3px 7px;border-radius:20px}}.flags .ok{{background:#dcfae6;color:#067647}}
.note{{background:#fff7e8;border:1px solid #fedf89;padding:12px 14px;border-radius:10px;font-size:13px;line-height:1.6}}
</style></head><body><main><h1>{title}</h1><p class="note">本报告用于模型效果预览。自动分数仅检查结构、引用和明显风险，不能替代业务人员和店长的盲评分。所有测试案例均为合成脱敏场景。</p>{summary_html}{''.join(case_html)}</main></body></html>"""


def write_csv(path: Path, records: list[dict[str, Any]]) -> None:
    with path.open("w", newline="", encoding="utf-8-sig") as handle:
        writer = csv.writer(handle)
        writer.writerow(["blindId", "caseId", "evaluator", "factualAccuracy30", "stage20", "actionability20", "personalization15", "compliance15", "hardFail", "notes"])
        for item in records:
            writer.writerow([blind_id(item["caseId"], item["profile"], item["mode"]), item["caseId"], "", "", "", "", "", "", "", ""])


def main() -> None:
    args = parse_args()
    corpus, config = load_inputs(args)
    cases = corpus["cases"][:args.limit] if args.limit else corpus["cases"]
    profiles = selected(args.profiles, config["profiles"])
    modes = selected(args.modes, config["modes"])
    out_dir = Path(args.out) if args.out else Path("docs/ai-evaluation/results") / datetime.now().strftime("run-%Y%m%d-%H%M%S")
    out_dir.mkdir(parents=True, exist_ok=True)
    jobs = [(case, profile, mode) for case in cases for profile in profiles for mode in modes]
    print(json.dumps({"out": str(out_dir), "cases": len(cases), "profiles": profiles, "modes": modes, "calls": len(jobs)}, ensure_ascii=False))
    if args.dry_run:
        return
    key = Path(config["key_file"]).read_text(encoding="utf-8").strip()
    results_path = out_dir / "results.jsonl"
    existing = load_existing(results_path)
    lock = Lock()
    completed = 0
    with results_path.open("a", encoding="utf-8") as handle:
        with ThreadPoolExecutor(max_workers=max(1, args.workers)) as pool:
            future_map = {
                pool.submit(run_one, config, key, case, profile, mode, corpus): (case, profile, mode)
                for case, profile, mode in jobs
                if work_key(case["id"], profile, mode) not in existing
            }
            for future in as_completed(future_map):
                case, profile, mode = future_map[future]
                try:
                    record = future.result()
                except Exception as exc:
                    record = {
                        "key": work_key(case["id"], profile, mode), "caseId": case["id"], "profile": profile,
                        "mode": mode, "model": config["profiles"][profile]["model"], "ok": False,
                        "latencyMs": 0, "error": f"unhandled {type(exc).__name__}: {exc}", "raw": "", "parsed": None,
                        "usage": {}, "cost": {"peakUsd": 0.0, "offpeakUsd": 0.0},
                        "score": automatic_score(None, "")
                    }
                with lock:
                    handle.write(json.dumps(record, ensure_ascii=False) + "\n")
                    handle.flush()
                    existing[record["key"]] = record
                    completed += 1
                    print(f"[{completed}/{len(future_map)}] {record['key']} ok={record['ok']} {record['latencyMs']}ms", flush=True)
    records = [existing[work_key(case["id"], profile, mode)] for case, profile, mode in jobs if work_key(case["id"], profile, mode) in existing]
    summary = summarise(records)
    (out_dir / "summary.json").write_text(json.dumps({"summary": summary, "records": len(records)}, ensure_ascii=False, indent=2), encoding="utf-8")
    (out_dir / "report.html").write_text(render_html(cases, records, summary, False), encoding="utf-8")
    (out_dir / "blind-report.html").write_text(render_html(cases, records, summary, True), encoding="utf-8")
    write_csv(out_dir / "human-evaluation.csv", records)
    blind_map = {blind_id(item["caseId"], item["profile"], item["mode"]): {"caseId": item["caseId"], "profile": item["profile"], "mode": item["mode"]} for item in records}
    (out_dir / "blind-map.json").write_text(json.dumps(blind_map, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"summary": str(out_dir / "summary.json"), "report": str(out_dir / "report.html"), "blindReport": str(out_dir / "blind-report.html"), "evaluationCsv": str(out_dir / "human-evaluation.csv")}, ensure_ascii=False))


if __name__ == "__main__":
    main()
