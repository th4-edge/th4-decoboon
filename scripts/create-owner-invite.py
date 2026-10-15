#!/usr/bin/env python3
"""为指定项目生成业主邀请（线上业主端验证用）。

用法：
  python3 scripts/create-owner-invite.py --project-no P1792019925776 --phone 13912345678 --name 李思远
  python3 scripts/create-owner-invite.py --list --project-no P1792019925776

说明：
- `--phone` 必须是**业主测试微信绑定的手机号**（后端会校验微信授权手机号与邀请手机号一致）。
- 返回 inviteId / token / 小程序路径 / 开发者工具启动参数。
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

TYPE_LABEL = {"Pending": "待接受", "Accepted": "已接受", "Cancelled": "已撤销", "Expired": "已过期"}


class Api:
    def __init__(self, base: str, token: str):
        self.base, self.token = base.rstrip("/"), token

    def __call__(self, route: str, payload: dict):
        out = subprocess.run(["curl", "-s", "-X", "POST", f"{self.base}/{route}",
                              "-H", "Content-Type: application/json", "-H", "token: " + self.token,
                              "--data-binary", "@-"],
                             input=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
                             capture_output=True)
        raw = out.stdout.decode("utf-8", "ignore")
        try:
            res = json.loads(raw or "{}")
        except json.JSONDecodeError:
            raise RuntimeError(f"{route}: 非 JSON 响应 {raw[:200]}")
        if not res.get("success"):
            raise RuntimeError(f"{route} 失败: {json.dumps(res.get('errorCode') or res, ensure_ascii=False)[:300]}")
        return res.get("data") or {}


def rows(data) -> list[dict]:
    if isinstance(data, list):
        return data
    for key in ("list", "content", "records", "items"):
        if isinstance(data, dict) and isinstance(data.get(key), list):
            return data[key]
    return []


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api", default="https://decoboon.joytsing.com/api")
    parser.add_argument("--token-file", default="/tmp/.decoboon-prod-token")
    parser.add_argument("--project-no")
    parser.add_argument("--name", default="业主")
    parser.add_argument("--phone")
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--cancel")
    args = parser.parse_args()

    token = Path(args.token_file).read_text(encoding="utf-8").strip()
    api = Api(args.api, token)

    if args.cancel:
        api("identity/customer-invite/cancel", {"inviteId": int(args.cancel)})
        print(f"已撤销邀请 {args.cancel}")
        return 0

    projects = rows(api("project/project/page", {"current": 1, "size": 50}))
    if args.project_no:
        projects = [p for p in projects if str(p.get("no")) == args.project_no]
    if not projects:
        print("未找到项目；用 --project-no 指定", file=sys.stderr)
        return 2
    for p in projects:
        pid = p.get("projectId")
        print(f"项目 {p.get('no')} {p.get('name')} projectId={pid} 状态={p.get('status')}")
    project = projects[0]
    project_id = int(project.get("projectId"))

    if args.list:
        for inv in rows(api("identity/customer-invite/page", {"projectId": project_id, "current": 1, "size": 50})):
            status = inv.get("status")
            label = TYPE_LABEL.get(str(status), str(status))
            print(f"  inviteId={inv.get('inviteId')} 业主={inv.get('customerName')} 手机={inv.get('maskedTelNo') or inv.get('telNo')}"
                  f" {label} 创建={inv.get('createTime')}")
        return 0

    if not args.phone:
        print("生成邀请需要 --phone（业主测试微信绑定的手机号）", file=sys.stderr)
        return 2

    invite = api("identity/customer-invite/save", {"projectId": project_id,
                                                  "customerName": args.name,
                                                  "telNo": args.phone})
    path = (f"pages/owner/enter?type=customer&inviteId={invite.get('inviteId')}"
            f"&token={invite.get('token')}")
    print(json.dumps({"inviteId": invite.get("inviteId"), "token": invite.get("token"),
                      "projectName": project.get("name"), "status": invite.get("status"),
                      "expireTime": invite.get("expireTime")}, ensure_ascii=False, indent=2))
    print("\n小程序路径（开发者工具自定义编译「启动页面+启动参数」）：")
    print(f"  启动页面: pages/owner/enter")
    print(f"  启动参数: type=customer&inviteId={invite.get('inviteId')}&token={invite.get('token')}")
    print(f"  完整路径: {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
