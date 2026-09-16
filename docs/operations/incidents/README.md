# 生产事故记录

本目录由 `scripts/prod-monitor.sh` 自动生成，不要手工覆盖已归档的原始快照。

目录结构：

```text
docs/operations/incidents/
├── registry.json
├── YYYY-MM-DD/
│   └── YYYYmmdd-HHMMSS/
│       ├── raw.txt
│       ├── analysis.json
│       └── summary.md
```

状态约定：

- `observed`：已记录，但属于预期业务噪音。
- `open`：需要处理或正在处理。
- `fixed`：已完成代码修复、测试和发版验证。
- `muted`：经确认短期无需处理，需填写原因和复核日期。
