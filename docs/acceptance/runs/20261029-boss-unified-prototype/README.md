# 企业负责人统一经营台 · 高保真 HTML 预览证据

> 状态：`HIGH_FIDELITY_PROTOTYPE / REVIEW_READY`  
> 性质：真实浏览器渲染与交互预览，不是生产页面、不是 UI 独立验收、不是业务验收、不是发布候选。  
> 原型文件：`docs/product/prototypes/owner-operating-system/unified-v1.html`

## 1. 体验入口

直接打开：

```text
docs/product/prototypes/owner-operating-system/unified-v1.html
```

本地预览：

```bash
cd docs/product/prototypes/owner-operating-system
python3 -m http.server 4173
```

打开 `http://127.0.0.1:4173/unified-v1.html`。

## 2. 可体验内容

1. **体验预览 / 研发安排**：顶部切换负责人经营台与下一步研发路线。
2. **Web / App**：同一份页面在桌面与移动职责下的布局差异。
3. **风险对象抽屉**：点击“处理最高风险”或风险表行，查看对象事实、原因、最近时间线和完成后的回读项。
4. **亲自处理**：在对象抽屉里切换业务员、设计师、项目经理、财务方式；身份和数据范围不变。
5. **七态演练**：右上角“状态演练”或键盘 `1`～`7`，体验正常、加载、空、失败、无权限、长内容、极端值。
6. **业务推进**：点击任一阶段，查看该阶段的负责人观测提示。
7. **研发安排**：查看 `BOS-D01～D04`、Provider 字段对账、`BOS-R1` 和后续门禁。

## 3. 截图

| 端 / 内容 | 尺寸 | 截图 |
| --- | --- | --- |
| Web 经营总览 | 1440×1000 | `screenshots/web-overview-1440.png` |
| Web 经营总览 | 1280×800 | `screenshots/web-overview-1280.png` |
| Web 经营总览 | 1024×768 | `screenshots/web-overview-1024.png` |
| 移动端经营总览 | 390×844 | `screenshots/web-overview-390.png` |
| App 预览 | 390×844 | `screenshots/app-overview-390.png` |
| App 模式桌面展示 | 1440×1000 | `screenshots/app-overview-1440.png` |
| 风险对象抽屉 | 1440×1000 | `screenshots/object-drawer-1440.png` |
| 研发安排 | 1440×1000 | `screenshots/plan-1440.png` |
| 研发安排 | 1024×768 | `screenshots/plan-1024.png` |
| 研发安排移动端 | 390×844 | `screenshots/plan-390.png` |

## 4. 验证结果

| 检查 | 结果 | 说明 |
| --- | --- | --- |
| Console | PASS | 0 error / 0 warning |
| 页面级横向溢出 | PASS | 1440 / 1280 / 1024 / 390 均为 0 |
| 内容区横向溢出 | PASS | 1440 / 1280 / 1024 / 390 均为 0 |
| 可见文字最小字号 | PASS | 正文与辅助信息 ≥ 12px；头像字母除外 |
| 交互热区 | PASS | 实测无小于 24×24px 的可交互目标 |
| 单一主行动 | PASS | 经营总览同视口仅 1 个主按钮 |
| 风险抽屉 | PASS | 打开后焦点进入关闭按钮；`Escape` 关闭并归还焦点 |
| 七态演练 | PASS | 正常、加载、空、失败、无权限、长内容、极端值均可切换并恢复 |
| 研发安排移动端 | PASS | 390px 下决策表与动作表改为纵向记录，无横向找列 |
| 视觉效果预检 | `PARTIAL_VISUAL_EVIDENCE` | `qwen2.5vl:7b` 已看图预检；仍需用户或更强视觉评审最终判断 |

视觉预检文件：

- `web-overview-1440-vision-review-final.md`
- `app-overview-390-vision-review.md`
- `plan-1440-vision-review.md`

## 5. 业务边界

- 页面使用演示数据，不连接真实 Provider。
- 项目毛利在成本不完整时显示“成本未完整”，不展示伪利润。
- 公司/门店经营利润没有作为确定金额展示。
- 风险阈值与经营利润口径仍受 `GAP-BOS-002`、`GAP-070` 约束。
- 原型没有执行审批、付款、删除、权限变更或真实业务写入。
- 微信小程序独立壳层仍受 `GAP-BOS-001` 约束。

## 6. 下一步

1. 用户确认 `BOS-D01～D04`。
2. 形成 BOSS Provider 字段对账矩阵。
3. 输出 `BOS-R1` 生产实施计划和组件映射。
4. 进入真实 Provider 开发与跨端验收。

## 7. 2026-10-29 用户反馈后的定位调整

- 本原型与当前上线产品在导航结构和首屏组织上差异较大，**不得作为默认生产改造方案**。
- 原型保留为方向探索与工作面样板，用于讨论“经营判断、风险对象、亲自处理”的表达方式。
- 默认研发路线改为 `BOS-C1 兼容式经营总览`：保留现有 Web 菜单、页面职责、深链和 App 底部 Tab，只修正角色错位并增量加入经营判断与风险入口。
- 在真实客户走查确认前，不进入五域信息架构的大范围改造。
