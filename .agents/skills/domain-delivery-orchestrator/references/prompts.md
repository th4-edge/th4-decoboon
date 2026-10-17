# Prompt Templates

## A. Start a domain

```text
你是装得清业务域交付总控。你的任务不是一次性完成整个业务域，而是建立一个可恢复的交付状态。

业务域：{domain}
业务目标：{northStar}
目标角色：{roles}
本期范围：{scope}
本期不做：{nonGoals}
最高优先级不变量：{invariants}

执行：
1. 读取 AGENTS.md、业务智能体规范、相关代码/页面/迁移；
2. 创建或更新 docs/product/domains/{domain}/00-charter.md、state.json、10-reconciliation.md；
3. 写至少 2 个真实场景（使用人、触发、当前动作、系统下一步、异常）；
4. 列出必须由业务智能体裁决的问题；
5. 给出垂直切片批次计划和第一道门禁；
6. 停在 BIZ_ALIGNED 之前，不进入编码。

禁止：未经业务裁决直接实现规则；一次性铺满全域；用代码存在代替可用；自己宣布验收。
```

## B. Continue one batch

```text
继续 {domain}。先读取 state.json、00-charter.md、10-reconciliation.md 和 open decisions，不要复述历史。

当前批次：{batchId}
本批只做：{verticalSlice}
本批不做：{nonScope}

执行：
1. 核对本批业务规则与未决项；
2. 更新 blueprint/需求/页面绑定；
3. 实现最小后端/App/Web 切片；
4. 跑定向测试、类型检查、构建；
5. 做真实页面/真机走查并留存证据；
6. 更新对账矩阵；
7. 发现问题先修复，不能修复则 BLOCKED；
8. 更新 state.json 与 history；
9. 停在下一道门禁，输出下一批建议。
```

## C. Recover context

```text
从 {domain} 的 state.json 恢复，不要重新讲历史。

先报告：当前阶段、当前批次、北向目标、未闭环项、阻塞项、已完成证据、下一动作。
除非我明确说继续，否则只报告不执行。
```

## D. Business-agent sync

```text
请以 zhuangdeqing-business 的规范为唯一事实源，针对 {domain} / {object or flow} 做业务裁决。

场景：{scenario}
角色：{roles}
当前对象与状态：{objects}
已知规则：{knownRefs}
候选方案：{options}
需要回答：
1. 正确对象和状态是什么；
2. 权限与数据范围；
3. 金额/成本/指标口径；
4. 前置条件、异常和反向用例；
5. 哪些是标准、哪些是企业配置；
6. 受影响页面/API/迁移；
7. 需要哪些证据和验收。

输出 T06/T12，不要直接改代码。
```

## E. UI review

```text
对 {domain} / {page} 做真实渲染后的 UI 与交互审查。

要求：
1. 用真实浏览器/微信开发者工具打开当前实现；
2. 至少覆盖 1440/1024/390 或 App 小屏；
3. 检查加载、空态、失败、无权限、长内容、极端数值；
4. 对照业务蓝图和字段字典，检查信息层级、文案、下一步和错误恢复；
5. 输出缺陷清单、截图证据和优先级；
6. 不改业务规则，不宣布业务验收通过。
```

## F. Gate review

```text
对 {domain} 的 {phase} 门禁做审查。

读取 state.json、charter、对账矩阵和本批证据。
逐项给出 PASS / FAIL / BLOCKED：
- 业务裁决是否闭合；
- 产物是否存在；
- 测试/构建/渲染证据是否充分；
- 是否有 UNMAPPED/STALE；
- 是否越权或扩大范围；
- 下一道门禁是什么。

只有全部通过才能推进 phase；否则保持当前 phase 并写清阻塞原因。
```
