---
name: domain-delivery-orchestrator
description: Orchestrate a complex ZHUANGDEQING business domain across business adjudication, blueprint, planning, development, UI review, reconciliation, and acceptance. Use when starting, continuing, recovering, reviewing, or reconciling a multi-batch domain such as materials, contracts, finance, projects, or acceptance; do not use for small single-file fixes or ordinary bug fixes.
metadata:
  short-description: Recoverable multi-batch business-domain delivery protocol
---

# Domain Delivery Orchestrator

## Core rule

One batch = one vertical slice. External state is the memory; chat history is not. Never skip a gate, never expand the batch silently, and never let implementation invent business rules.

The goal is not to finish a whole domain in one turn. The goal is to keep the domain recoverable across turns:

```text
run envelope + charter + state ledger + reconciliation matrix + gate evidence
```

## Before work

Read, in this order:

1. `AGENTS.md`
2. `docs/operations/试运营单人轻量协作与发版规范.md`
3. `.agents/skills/domain-delivery-orchestrator/references/protocol.md`
4. `docs/product/domains/<domain>/state.json`
5. `docs/product/domains/<domain>/00-charter.md`
6. `docs/product/domains/<domain>/10-reconciliation.md`
7. Only the task-specific business cards, code, pages, and acceptance records.

If the domain state does not exist, initialize it with:

```bash
python3 .agents/skills/domain-delivery-orchestrator/scripts/domain_state.py init \
  --domain <domain> \
  --title "<业务域名称>" \
  --north-star "<一句话目标>" \
  --owner codex
```

Then stop after creating the charter/scenarios and reporting the first gate. Do not jump directly into implementation.

## Modes

- `start`: initialize the domain packet, write charter and at least two scenarios, identify business questions, stop at the first gate.
- `continue`: read `state.json`, execute only the named batch, update artifacts and reconciliation, then stop at the next gate.
- `recover`: report current phase, batch, open decisions, blockers, completed artifacts, and next action; do not continue unless asked.
- `gate-review`: verify the gate evidence and either advance the phase or report `BLOCKED`.
- `reconcile`: compare design ↔ blueprint ↔ requirements ↔ code ↔ page ↔ test ↔ acceptance; mark `UNMAPPED`, `STALE`, or `GAP`.
- `ui-review`: render the real App/Web UI, capture evidence, and review interaction and visual quality; do not decide business facts.
- `supervise`: read `run-envelope.md`, `state.json`, and `10-reconciliation.md`; determine whether the current batch is complete; generate the next batch prompt; never let the executor wait for the user between routine batches.
- `acceptance`: independent black-box verification only; do not edit implementation.

## Phase and gate discipline

| Phase | Required outcome | Gate |
| --- | --- | --- |
| `INTAKE` | charter, scope, non-goals, roles | goal and scope are explicit |
| `SCENARIOS_READY` | at least 2 real scenarios | each has user, trigger, action, system next, exception |
| `BIZ_ALIGNED` | T06/T12 or equivalent business decision; open P0 gaps resolved | no coding before this gate |
| `BLUEPRINT_READY` | blueprint JSON, object/state/field references | every node has source refs or is marked gap |
| `PLAN_READY` | one-batch vertical slices, dependencies, rollback, exit criteria | no batch is broader than one user journey |
| `DEV_IN_PROGRESS` | bounded implementation | no unrelated expansion |
| `DEV_VERIFIED` | targeted tests + compile/build + deploy evidence | failures are fixed or marked blocked |
| `UI_REVIEWED` | real render, viewport screenshots, interaction review | no render = no UI approval |
| `RECONCILED` | no `UNMAPPED`/`STALE` rows for this batch | design and implementation agree |
| `ACCEPTED` | independent evidence for the batch | developer cannot self-approve |
| `RELEASED` | environment, version, rollback, experience/formal boundary | mini-program formal release remains with the user |
| `LEARNED` | reusable lessons, anti-patterns, shortest paths | process facts do not overwrite business facts |

## Roles and hats

Use these as explicit responsibility hats, not as a reason to create unnecessary agent sessions:

- Orchestrator: owns state, batches, gates, and reconciliation.
- Business fact agent: owns T05/T06/T12, rules, evidence, GAP/DEC.
- Product design agent: owns scenarios, roles, flows, conditions, acceptance criteria.
- Blueprint agent: owns schema, nodes, fields, UI bindings; it cannot invent rules.
- Development agent: owns code, tests, build, deploy evidence; it cannot declare acceptance.
- UI review agent: owns real rendered evidence and interaction critique; it cannot change business rules.
- Independent acceptance agent: owns negative, cross-tenant, edge, and cross-surface verification; it cannot edit implementation.
- Process steward: owns reusable lessons and anti-patterns; it cannot produce business facts.

In single-person trial mode, switch hats explicitly in the output and use an independent agent only for high-risk acceptance or UI review where the user requests it.

## Required artifacts

Use `references/protocol.md` for the full protocol and `references/prompts.md` for the start, continue, recover, business-sync, UI-review, and gate-review prompts.

The domain packet lives under `docs/product/domains/<domain>/` and contains:

```text
00-charter.md
run-envelope.md
01-scenarios.md
02-business-sync.md
03-blueprint.json
04-requirements.md
05-plan.md
06-implementation.md
07-ui-review.md
08-acceptance.md
09-release.md
10-reconciliation.md
state.json
```

Use `scripts/domain_state.py` for initialization, status, validation, history, phase updates, and next-prompt generation. The supervisor must run `next-prompt` before dispatching a batch.

## Output contract

Every batch must report:

```text
Domain | phase | batch | north star
Current fact | what changed | what did not change
Business decisions / gaps
Artifacts updated
Tests / build / render evidence
Open risks / blockers
Reconciliation status
Next action and gate
```

Do not claim “完成” if the reconciliation matrix has `UNMAPPED` or `STALE` rows, if business decisions are open, or if the batch has not passed its gate.

## Hard boundaries

- Do not modify business facts without `zhuangdeqing-business` adjudication.
- Do not use a product document, chat memory, or code behavior as a substitute for business evidence.
- Do not let the runtime AI agent approve, pay, delete, or change permissions automatically.
- Do not let an implementation agent self-approve acceptance.
- Do not expand the batch because “while we are here” would be convenient.
- Do not publish the formal mini-program version; only build, upload the development/experience version, and verify it.
