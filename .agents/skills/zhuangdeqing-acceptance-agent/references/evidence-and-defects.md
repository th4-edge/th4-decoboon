# 证据与缺陷规则

## 1. 证据等级

| 等级 | 示例 | 能证明 |
| --- | --- | --- |
| `E1` | 需求文档、代码阅读、口头说明 | 设计与实现意图 |
| `E2` | API 响应、数据库查询、日志 | 服务端事实 |
| `E3` | 真实页面截图/视频 + 网络摘要 + 状态回读 | 角色页面行为 |
| `E4` | 匹配业主身份、真人见证、真机/弱网 | 主体真实性和现场体验 |

页面可用结论至少 `E3`；跨租户、金额、成本、业主确认等高风险结论需要 `E3 + E2`，涉及真人承诺时再要求 `E4`。

## 2. 文件命名

```text
evidence/<CASE-ID>/<step>-before.png
evidence/<CASE-ID>/<step>-after.png
evidence/<CASE-ID>/<step>-network.json
evidence/<CASE-ID>/<step>-api.json
evidence/<CASE-ID>/<step>-db.json
evidence/<CASE-ID>/<step>-video.mp4
```

## 3. 必须证明与不能证明

每个证据写明：

```text
能证明什么
不能证明什么
采集时间
角色/租户/端面
代码提交和制品版本
```

## 4. 缺陷等级

- `P0`：跨租户/越权、资金/成本错误、核心主链断裂、数据不可恢复；
- `P1`：高频角色无法完成、关键交接断裂、重复记账、跨端口径错误；
- `P2`：恢复、空态、性能、可访问性、显著交互问题；
- `P3`：低风险文案和视觉一致性问题。

## 5. 缺陷生命周期

```text
OPEN -> FIXING -> FIXED_PENDING_VERIFY -> VERIFIED
                         \-> REOPENED
OPEN/BLOCKED -> WONTFIX（仅用户或授权主体可接受）
```

开发完成不等于关闭；页面证据复验通过才可 `VERIFIED`。
