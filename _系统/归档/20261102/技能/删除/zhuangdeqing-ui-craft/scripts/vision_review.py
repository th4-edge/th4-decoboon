#!/usr/bin/env python3
"""Local vision review bridge for ZHUANGDEQING UI Craft.

Uses Ollama's qwen2.5vl model to turn screenshots into structured visual feedback.
The primary Codex model may be text-only; this script is the separate visual reviewer.
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Iterable

try:
    from PIL import Image
except ImportError:
    Image = None

PRESETS = {
    "ui-review": """你是严格的商业级企业工作台 UI 评审员。请直接观察截图，用中文输出，必须具体、克制、可执行：
1. 第一印象与产品真实性；
2. 视觉层级：用户是否 3 秒内知道先看什么、先做什么；
3. 排版与节奏：字阶、行高、对齐、留白、密度、视觉重心；
4. 配色与对比：主色、语义色、色彩比例、对比度；
5. 组件与表格：卡片、表格、页签、按钮、状态标签是否一致、是否过重；
6. AI 味/模板感：逐项指出卡片墙、药丸、英文 eyebrow、装饰性 2.5D/3D、说明性文案、模板化 KPI；
7. 最需要改的 5 点，按影响排序。
禁止泛泛夸奖；禁止为了“更丰富”建议增加辅助色、装饰图形或说明性文案；优先指出可见的结构、层级、密度、对齐和视觉噪音问题。""",
    "reference-study": """你是资深 B 端视觉与交互设计研究员。请观察这幅站酷参考图，提炼可复用的设计原则，用中文输出：
1. 页面类型与业务语境；
2. 构图与信息层级；
3. 排版、字阶与留白；
4. 配色比例与质感；
5. 组件密度、表格/卡片/图表的处理；
6. 交互暗示与动效暗示；
7. 哪些做法适合装得清，哪些不适合；
8. 3–5 条可转译为设计 Token 或页面规则的具体结论。
不要复制具体作品，只提炼原则。""",
    "reference-compare": """第一张图是参考，第二张图是当前页面。请做专业视觉对比，用中文输出：
1. 两者第一印象差异；
2. 构图、层级、留白和密度差异；
3. 字阶、对齐、数字与表格处理差异；
4. 配色比例、色调与语义色差异；
5. 组件形态、边缘、阴影与材质差异；
6. 当前页面的 AI 味/模板感；
7. 当前页面最值得借鉴的 5 个具体做法；
8. 当前页面最需要修正的 5 个具体问题。
不要只说“更高级/更现代”，必须给出可观察、可执行的描述。""",
    "free": "请直接观察图片，给出专业、具体、可执行的中文视觉与交互评审。",
}


def image_bytes(path: Path, max_side: int) -> bytes:
    if not path.exists():
        raise FileNotFoundError(path)
    raw = path.read_bytes()
    if Image is None:
        return raw
    try:
        img = Image.open(io.BytesIO(raw))
        try:
            img.seek(0)
        except Exception:
            pass
        img = img.convert("RGB")
        img.thumbnail((max_side, max_side))
        buf = io.BytesIO()
        img.save(buf, format="PNG", optimize=True)
        return buf.getvalue()
    except Exception:
        return raw


def request_ollama(host: str, model: str, prompt: str, images: Iterable[Path], timeout: int, num_ctx: int, max_side: int) -> str:
    payload = {
        "model": model,
        "prompt": prompt,
        "images": [base64.b64encode(image_bytes(p, max_side)).decode("ascii") for p in images],
        "stream": False,
        "options": {"temperature": 0.2, "num_ctx": num_ctx},
    }
    req = urllib.request.Request(
        f"{host.rstrip('/')}/api/generate",
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            data = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Ollama HTTP {exc.code}: {body}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(
            f"无法连接 Ollama ({host})。请确认 `ollama serve` 正在运行。原始错误: {exc}"
        ) from exc
    return str(data.get("response", "")).strip()


def list_models(host: str, timeout: int = 10) -> list[str]:
    req = urllib.request.Request(f"{host.rstrip('/')}/api/tags")
    with urllib.request.urlopen(req, timeout=timeout) as response:
        data = json.loads(response.read().decode("utf-8"))
    return [m.get("name", "") for m in data.get("models", []) if m.get("name")]


def main() -> int:
    parser = argparse.ArgumentParser(description="Local vision review through Ollama")
    parser.add_argument("--image", action="append", required=False, help="Image path; repeat for multiple images")
    parser.add_argument("--preset", choices=sorted(PRESETS), default="ui-review")
    parser.add_argument("--prompt", default=None, help="Custom prompt append or override for --preset free")
    parser.add_argument("--model", default="qwen2.5vl:7b")
    parser.add_argument("--host", default="http://127.0.0.1:11434")
    parser.add_argument("--timeout", type=int, default=300)
    parser.add_argument("--num-ctx", type=int, default=8192, help="Ollama context size")
    parser.add_argument("--max-image-side", type=int, default=1280, help="Resize images before sending")
    parser.add_argument("--json", action="store_true", help="Output structured JSON")
    parser.add_argument("--output", help="Optional output Markdown/JSON path")
    parser.add_argument("--check", action="store_true", help="Check Ollama and model availability")
    args = parser.parse_args()

    if args.check:
        models = list_models(args.host, args.timeout)
        print(json.dumps({"host": args.host, "models": models, "ok": args.model in models}, ensure_ascii=False, indent=2))
        return 0 if args.model in models else 2

    if not args.image:
        parser.error("--image is required unless --check is used")

    images = [Path(p).expanduser() for p in args.image]
    prompt = PRESETS[args.preset]
    if args.prompt:
        prompt = args.prompt if args.preset == "free" else prompt + "\n\n补充要求：\n" + args.prompt

    started = time.time()
    try:
        response = request_ollama(args.host, args.model, prompt, images, args.timeout, args.num_ctx, args.max_image_side)
    except Exception as exc:
        print(f"vision-review failed: {exc}", file=sys.stderr)
        return 1
    elapsed = round(time.time() - started, 1)

    result = {
        "model": args.model,
        "host": args.host,
        "preset": args.preset,
        "images": [str(p) for p in images],
        "elapsed_seconds": elapsed,
        "review": response,
    }
    text = json.dumps(result, ensure_ascii=False, indent=2) if args.json else (
        f"# Vision Review\n\n- model: `{args.model}`\n- preset: `{args.preset}`\n- images: {len(images)}\n- elapsed: {elapsed}s\n\n{response}\n"
    )
    if args.output:
        out = Path(args.output).expanduser()
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(text, encoding="utf-8")
        print(f"saved: {out}")
    else:
        print(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
