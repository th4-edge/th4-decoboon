# Payroll Labor B1 · Independent Black-box Acceptance

- Run: `20261021-payroll-b1-independent`
- Date: 2026-10-21
- Domain: `payroll-labor`
- Batch: B1 财务 Web 薪酬方案维护闭环
- Verdict: **PASS**
- Environment: local backend, local Web, isolated local MySQL, WeChat DevTools local simulator
- Production writes: none
- Implementation changes: none

## Final status

| Area | Result | Evidence |
| --- | --- | --- |
| Backend targeted tests | PASS 12/0/0 | `p3-revalidation-backend-targeted.log` |
| Backend full tests (earlier independent run) | PASS 900/0/0 | `backend-full-tests.log` |
| Web type-check/build/permissions/store-scope/workbenches | PASS | `web-validation.log`, `p3-revalidation-web.log` |
| App type-check/closure contract/mp-weixin build | PASS | `acc003-app-validation.log` |
| Finance Web CRUD and role/negative checks | PASS | browser/API evidence in this directory |
| Missing `scheduledDays` P3 | RESOLVED: code 400, no persistence | `p3-revalidation-api-missing-days.json` |
| Stale validation message P3 | RESOLVED | `p3-revalidation-ui-watch.json`, P3 screenshots |
| App create precision | PASS: `5432.12 / 23 天`, shows `¥5,432.12`, unlocked | `acc003-app-precision-e2e.json`, `acc003-app-02-created-exact.png` |
| App edit precision | PASS: `5432.12 / 24 天`, shows `¥5,432.12`, unlocked | `acc003-app-precision-e2e.json`, `acc003-app-04-edited-exact.png` |
| App delete/cleanup | PASS: confirmation shown, list 6→5, API count 0 | `acc003-app-precision-e2e.json`, `acc003-app-06-deleted-list.png` |
| App runtime console | 0 errors | `acc003-app-precision-e2e.json` |

## Closed findings

- **ACC-B1-001 — RESOLVED**: missing `scheduledDays` returns Provider code `400` and does not persist.
- **ACC-B1-002 — RESOLVED**: stale client validation message clears after salary input changes.
- **ACC-B1-U01 — CLOSED**: App live create/edit/delete completed end-to-end in WeChat DevTools local simulator.
- **ACC-B1-003 — RESOLVED**: App list preserves two decimal places and matches API amount exactly.

## Cleanup

- Three temporary credential password hashes restored: 3/3 matched backup.
- 徐勇 (`emp_id=1790224826490830054`): 0 schemes.
- 吴濛濛 (`emp_id=1790221992067828768`): 1 original scheme.
- Total schemes: 5; temporary acceptance schemes: 0.
- WeChat DevTools project and temporary build closed/deleted.

## Release gate

Independent B1 acceptance has passed. The batch may enter the release gate; actual deployment/release was not performed in this run.
