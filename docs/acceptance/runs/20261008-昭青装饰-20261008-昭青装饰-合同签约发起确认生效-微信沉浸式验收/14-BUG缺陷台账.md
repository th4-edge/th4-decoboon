# BUG 缺陷台账

| ID | 类别 | 严重度 | 现象 | 根因 | 修复 | 状态 | 证据 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| XA-20261008-001 | BUG | P2 | 重复 Effective 误报不能更换计划 | `ensure` 重复调用 `select` | 双快照存在时幂等返回 | 已修复待独立复验 | `S03-duplicate-effective-response.json` |
