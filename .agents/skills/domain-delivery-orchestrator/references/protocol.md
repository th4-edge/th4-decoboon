# Recoverable Business-Domain Delivery Protocol

## 1. Why this exists

A complex domain cannot be delivered reliably in one model turn. The protocol externalizes memory so that any later turn can recover the goal, decisions, scope, evidence, and next action without relying on chat context.

The protocol has four invariants:

1. Business facts come from the business agent, never from code or conversation memory.
2. One batch is one vertical slice.
3. A phase cannot advance without its gate evidence.
4. Design, blueprint, code, UI, tests, and acceptance must be reconciled.

## 2. Domain packet

```text
docs/product/domains/<domain>/
├── run-envelope.md          # bounded autonomy authorization
├── 00-charter.md            # north star, scope, non-goals, invariants, roles
├── 01-scenarios.md          # at least 2 real business scenarios
├── 02-business-sync.md      # T05/T06/T12 references, decisions, GAPs
├── 03-blueprint.json        # machine-readable business blueprint
├── 04-requirements.md       # requirements, conditions, permissions, data
├── 05-plan.md               # vertical-slice batches and gates
├── 06-implementation.md     # changed files, contracts, tests, deploy evidence
├── 07-ui-review.md          # real render evidence and defects
├── 08-acceptance.md         # independent acceptance evidence
├── 09-release.md            # environments, version, rollback, release boundary
├── 10-reconciliation.md     # design ↔ blueprint ↔ code ↔ UI ↔ test matrix
└── state.json               # current phase, batch, open decisions, next action
```

Templates are in `docs/product/domains/_template/`.

## 3. State model

`state.json` is the recovery source. Required fields:

```json
{
  "schemaVersion": 1,
  "domain": "material",
  "title": "材料履约",
  "northStar": "材料计划、选材、作业单、采购、到场与结算形成可追溯闭环",
  "owner": "codex",
  "phase": "INTAKE",
  "batch": "B0",
  "nextAction": "完成 charter 与两个真实场景，提交业务咨询",
  "openDecisions": [],
  "blockers": [],
  "artifacts": {},
  "history": []
}
```

Use `scripts/domain_state.py show` to recover and `validate` before a gate.

## 4. Phase gates

### INTAKE

Required: charter with goal, users, scope, non-goals, invariants, success metrics.

Block if the goal is a feature list rather than a business outcome.

### SCENARIOS_READY

Required: at least two scenarios, each with:

- user/role;
- trigger event;
- current action;
- system next action;
- exception/recovery path;
- relevant objects and state changes.

### BIZ_ALIGNED

Required: business agent response or T12 with authoritative rules, conditions, roles, permissions, amount/cost semantics, exceptions, and evidence. P0 gaps must be closed or explicitly blocking.

No implementation before this gate.

### BLUEPRINT_READY

Required: blueprint JSON with object graph, stages, roles, fields, rules, checks, exceptions, UI bindings, and source refs. Every node either references an approved business card or is marked `gap`.

### PLAN_READY

Required: batches that are vertical slices, dependencies, exit criteria, rollback, and the next gate. A batch should not span unrelated domains.

### DEV_IN_PROGRESS / DEV_VERIFIED

Required: changed files, API/entity/migration notes, targeted tests, compile/build, and deploy evidence. The implementation agent cannot claim independent acceptance.

### UI_REVIEWED

Required: real App/Web render at agreed resolutions/devices, screenshots, interaction states, accessibility review, and defect ledger. No render means no UI approval.

### RECONCILED

Required: every batch requirement has a row in `10-reconciliation.md` and a corresponding blueprint node, code/API, page, test, and acceptance evidence. No `UNMAPPED` or `STALE` rows for the batch.

### ACCEPTED

Required: independent positive, negative, cross-tenant/permission, duplicate/idempotency, boundary, and cross-surface evidence. High-risk changes require an independent reviewer.

### RELEASED

Required: environment, version, rollback, smoke evidence, and the correct release boundary. The formal mini-program release remains with the user.

### LEARNED

Required: reusable lesson or anti-pattern; no process lesson may be promoted to a business fact without business-agent review.

## 5. Business-agent synchronization

Use the existing business-agent protocol:

```text
T05 request
→ T06 answer
→ T12 handoff when implementation is involved
→ implementation and verification
→ T13 receipt/learning/unlock
```

Rules:

- Do not copy business rules into the product repository as a second source of truth.
- Blueprint nodes cite `BR/DEC/SCN/OBJ/MET/EV` identifiers.
- A missing source is `gap`, not an invitation to infer.
- When a business card changes, mark dependent blueprint nodes and reconciliation rows `stale`.
- Runtime help must not present `gap`, `stale`, or `draft` content as normative.

## 6. Batch design

Each batch should be a single vertical slice:

- one user scenario or one tightly coupled object lifecycle;
- one backend contract slice;
- one App surface or one Web surface, not both unless required;
- one data check or one failure path;
- one test/render/acceptance evidence set.

At the end of a batch, stop and report the next gate. Do not use “顺带” work to expand scope.

## 7. Recovery ritual

At the start of every continuing session:

1. Read `state.json`.
2. Read `00-charter.md`.
3. Read `10-reconciliation.md`.
4. Read open decisions and blockers.
5. Report the current phase and next action.
6. Execute only the requested batch.
7. Update state, history, and reconciliation before stopping.

## 8. Long-running supervision

A long-running supervisor is allowed only when `run-envelope.md` defines:

- batch queue;
- allowed autonomous decisions;
- human-only gates;
- quality gates;
- retry and stop budget;
- return packet.

The supervisor reads state, generates the next prompt, and continues routine batches. It must not wait for the user between every batch. If a hard gate is hit, it must stop that branch, record a decision request, and continue independent work where possible.

## 9. Done definition

A batch is done only when:

- the batch scope is implemented;
- business decisions are referenced and not contradicted;
- tests/build/render evidence exists;
- reconciliation has no `UNMAPPED`/`STALE` rows for the batch;
- the next gate and next action are explicit;
- state and history are updated.
