# Artifact Contract

## 00-charter.md

Must contain the north star, target users, in-scope, non-goals, invariants, success metrics, business questions, and links to decisions.

## 01-scenarios.md

At least two real scenarios. Each scenario must include user, trigger, current action, system next action, exception, objects, and state changes.

## 02-business-sync.md

T05/T06/T12 references, authoritative rules, open GAPs, decision status, affected objects, permissions, amount/cost semantics, and evidence.

## 03-blueprint.json

Machine-readable structure:

```json
{
  "id": "domain.end-to-end",
  "version": "0.1.0",
  "status": "draft",
  "sourceRefs": [],
  "audience": [],
  "layers": ["standard", "tenant", "project"],
  "objects": [],
  "stages": [],
  "fields": [],
  "rules": [],
  "checks": [],
  "exceptions": [],
  "uiBindings": [],
  "agentTools": []
}
```

## 04-requirements.md

Requirement ID, user story, preconditions, permissions, data, state changes, UI, API, acceptance, and source refs.

## 05-plan.md

Vertical-slice batches, dependencies, exit criteria, rollback, owner, and gate.

## 06-implementation.md

Changed files, API/entity/migration notes, tests, build, deploy/restart evidence, risks, and unresolved items.

## 07-ui-review.md

Real render evidence, device/viewport, screenshots, interaction states, accessibility, defects, and re-review result.

## 08-acceptance.md

Independent positive/negative/permission/idempotency/boundary/cross-surface evidence. Do not use developer self-test as independent acceptance.

## 09-release.md

Environment, version, deploy path, smoke, rollback, experience/formal boundary, and user-confirmed release actions.

## 10-reconciliation.md

One row per requirement:

```text
REQ | scenario | business rule | blueprint node | field/API | page | test | acceptance | status
```

Status values: `PASS`, `PENDING`, `UNMAPPED`, `STALE`, `GAP`, `BLOCKED`.
