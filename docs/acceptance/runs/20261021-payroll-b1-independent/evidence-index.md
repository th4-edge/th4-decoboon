# Evidence Index

## Automated validation

- `backend-targeted-tests.log` — payroll targeted tests, 11/0/0
- `backend-full-tests.log` — full backend tests, 900/0/0
- `web-validation.log` — Web type-check, permissions, store-scope, workbenches, build
- `app-validation.log` — App type-check, payroll closure contract, mp-weixin build
- `app-local-build-runtime.log` — development build configured to local API
- `app-runtime-config.txt` — local runtime safety configuration

## API / role negatives

- `api-negative-and-role-results.json` — StoreManager write, storeId tampering, fact lock, validation, BOSS write
- `api-cross-tenant-employee-check.json` — cross-tenant employee rejection
- `api-filter-checks.json` — status/month/store filter results
- `audit-log-lines.txt` — CREATE/UPDATE/DELETE audit snapshots

## Browser UI

- `01-finance-list-1440x900.png`
- `02-finance-filter-wang-1440x900.png`
- `03-finance-empty-1440x900.png`
- `04-finance-error-offline-1440x900.png`
- `05-finance-list-1280x800.png`
- `06-finance-list-1024x768.png`
- `07-finance-create-drawer-1024x768.png`
- `08-finance-create-validation-1024x768.png`
- `09-finance-create-filled-1024x768.png`
- `10-finance-created-1024x768.png`
- `11-finance-edit-drawer-1024x768.png`
- `12-finance-edited-1024x768.png`
- `13-finance-terminate-dialog-1024x768.png`
- `14-finance-terminated-1024x768.png`
- `15-finance-delete-dialog-1024x768.png`
- `16-finance-deleted-1024x768.png`
- `17-finance-successor-1024x768.png`
- `18-storemanager-readonly-1440x900.png`
- `19-boss-writable-1280x800.png`
- UI snapshot text files `ui-*.txt`
- Viewport metrics `ui-1440-metrics.txt`, `ui-1280-metrics.txt`, `ui-1024-metrics.txt`

## App DevTools runtime

- `20-app-compensation-list.png`
- `21-app-successor-edit.png`
- `app-runtime-locked-flow.json`
- `app-runtime-locked-flow.mjs`
- `devtools-open.log`, `devtools-auto.log`, `devtools-close.log`

## Cleanup

- `credential-restore.txt`
- `db-final-state.txt`
- `00-git-status-before.txt`, `00-git-status-after.txt`

## P3 revalidation (2026-10-21)

- `p3-revalidation-backend-targeted.log` — targeted tests 12/0/0
- `p3-revalidation-web.log` — Web type-check and build
- `p3-revalidation-api-missing-days.json` — provider code 400, required message, no persistence
- `p3-revalidation-drawer-initial-snapshot.txt` — create drawer initial state
- `p3-revalidation-error-visible-snapshot.txt` — old client error visible after invalid save
- `p3-revalidation-error-cleared-snapshot.txt` — stale error absent after salary edit
- `p3-revalidation-ui-watch.json` — machine-readable UI watcher result
- `p3-revalidation-console.txt` — normal page: 0 console errors, 1 known route warning
- `p3-01-error-visible-1024x768.png`
- `p3-02-error-cleared-1024x768.png`
- `p3-revalidation-cleanup.txt` — credential restore and scheme-count checks

## App P2 runtime CRUD closure (2026-10-21)

- `p2-app-crud-build.log` — development build with local API
- `p2-app-devtools-open.log`, `p2-app-devtools-auto.log`, `p2-app-devtools-close.log`
- `p2-app-crud-e2e.mjs` — end-to-end automator script
- `p2-app-crud-e2e.json` — create/edit/delete results, API回读, console, and amount display finding
- `p2-app-01-create-filled.png` — real employee selection and create inputs
- `p2-app-02-created-list.png` — created list row, unlocked
- `p2-app-03-edit-filled.png` — real edit inputs
- `p2-app-04-edited-list.png` — edited list row; API 5432.12 displayed as ¥5,432
- `p2-app-05-delete-confirm.png` — real delete confirmation
- `p2-app-06-deleted-list.png` — list restored to 5 rows
- `p2-app-crud-cleanup.txt` — credential restore and residual checks

## ACC-B1-003 precision revalidation (2026-10-21)

- `acc003-app-validation.log` — App type-check, payroll closure contract, local mp-weixin build
- `acc003-devtools-open.log`, `acc003-devtools-auto.log`, `acc003-devtools-close.log`
- `acc003-app-precision-e2e.mjs` — runtime create/edit/delete precision script
- `acc003-app-precision-e2e.json` — exact `¥5,432.12` assertions, API回读, cleanup and console result
- `acc003-app-01-create-filled.png`
- `acc003-app-02-created-exact.png`
- `acc003-app-03-edit-filled.png`
- `acc003-app-04-edited-exact.png`
- `acc003-app-05-delete-confirm.png`
- `acc003-app-06-deleted-list.png`
- `acc003-cleanup.txt` — credential restore and scheme-count checks
