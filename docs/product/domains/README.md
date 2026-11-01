# 业务域领域包（Domain Packet）

每个复杂业务域一个目录，目录名使用小写短横线（如 `material`、`contract-change`、`finance`）。

领域包不是文档集合，而是跨会话恢复交付的持久化状态。任何继续开发、恢复上下文、门禁审查或对账，都必须先读：

1. `state.json`
2. `run-envelope.md`
3. `00-charter.md`
4. `10-reconciliation.md`

## 初始化

```bash
python3 scripts/domain_state.py init \
  --domain material \
  --title "材料履约" \
  --north-star "材料计划、选材、作业单、采购、到场与结算形成可追溯闭环" \
  --owner codex
```

## 查看与校验

```bash
python3 scripts/domain_state.py show --domain material
python3 scripts/domain_state.py validate --domain material
python3 scripts/domain_state.py next-prompt --domain material
```

## 规则

- 离开前先确认 `run-envelope.md`。
- 监督任务可在授权范围内连续推进，但不得跳过人工门禁。
- 一次只推进一个垂直切片批次。
- 业务事实未经 `zhuangdeqing-business` 裁决，不进入编码。
- 没有真实页面/真机证据，不算 UI 通过。
- 开发者自测不等于独立验收。
- 小程序正式版提审和发布由用户本人完成。

## 全域交付总控

多域并行或连续推进时，先读：

- `DOMAIN-REGISTRY.md`
- `PROGRAM-BOARD.json`
- `PROGRAM-PROTOCOL.md`

同一时间只允许一个域持有代码写租约。全域总控只编排，不直接修改业务代码。
