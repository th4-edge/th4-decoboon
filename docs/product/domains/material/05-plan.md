# 材料履约与材料业务助手 · 垂直切片计划

## 北向目标

材料计划、选材、作业单、采购、到场与结算形成可追溯闭环，并提供可解释、可校核的业务助手。

## 批次

| 批次 | 垂直切片 | 涉及角色 | 涉及对象 | 后端 | App/Web | 业务校核 | 退出条件 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| B1 | 业务卡核对、结构化蓝图、字段字典 | 产品/Boss/财务/设计/PM | MaterialPlan/Selection/Sheet/Purchase/Receipt/Settlement | 无或只读 blueprint definition | 无 | 现有 OBJ/BR/DEC/SCN 引用 | BLUEPRINT_READY | IN_PROGRESS |
| B2 | Web 蓝图中心 + 项目材料业务体检 | Boss/Store/Finance | 项目/材料/检查 | blueprint runtime/check provider | Web | 选材/下单/到场/成本完整性 | DEV_VERIFIED + UI_REVIEWED | PENDING |
| B3 | App 业务体检 + 页面解释 | Designer/PM/Boss/Finance | Sheet/Plan/Receipt | assistant context provider | App | 字段/时间/下一步 | DEV_VERIFIED + UI_REVIEWED | PENDING |
| B4 | 只读材料助手 | 全角色（按权限） | 全部材料对象 | assistant orchestrator/tools | Web/App | 50 黄金问题、8–12 校核 | 权限/反向/证据 PASS | PENDING |
| B5 | 独立验收与对账 | independent | 全部 | 无实现 | Web/App | 正向/反向/越权/幂等/边界 | ACCEPTED | PENDING |
| B6 | 体验版/发布准备 | release | App/后端/Web | 构建/发布记录 | App 体验版 | 冒烟/回滚 | RELEASED 或决策包 | PENDING |

## 依赖与顺序

- B1 先于所有实现；没有蓝图层和字段映射，不做页面。
- B2 的检查规则必须复用领域事实，不在页面计算。
- B3 的上下文助手不能直接查库。
- B4 必须依赖 B1 的蓝图、B2 的校核和 B3 的对象上下文。
- B5 必须独立于实现，不能由执行者自验。
- B6 只做体验版；正式版由用户完成。

## 回滚

- 后端/Web 使用 release 目录和 `.previous`；
- App 保留上一体验版；
- 蓝图/助手关闭开关可回退到静态说明；
- 不产生不可逆数据迁移。

## 门禁

- 本批结束停在：
- 下一批开始前必须：
