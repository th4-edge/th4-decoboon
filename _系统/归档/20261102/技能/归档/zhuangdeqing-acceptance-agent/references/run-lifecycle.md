# 运行目录与状态

## 1. 目录

```text
docs/acceptance/runs/<RUN-ID>/
├── run.json
├── case-results/
│   └── <CASE-ID>.json
├── evidence/
│   └── <CASE-ID>/
├── defects/
│   └── <DEF-ID>.json
├── reports/
│   ├── preflight.md
│   └── final.md
└── raw/
```

## 2. 状态

Run：`PLANNED / IN_PROGRESS / BLOCKED / COMPLETED / ABORTED`

Case：`NOT_RUN / PASS / FAIL / BLOCKED`

Defect：`OPEN / FIXING / FIXED_PENDING_VERIFY / VERIFIED / REOPENED / WONTFIX`

## 3. 不可变原则

- 已执行证据不覆盖，不重命名后伪造新结果；
- 修复后创建新 run 或新增 retest 目录；
- run.json 记录代码提交、制品哈希、oracle ID、计划 ID、环境、账号和数据前缀；
- 结果变更必须保留时间、执行人、原因和关联证据。
