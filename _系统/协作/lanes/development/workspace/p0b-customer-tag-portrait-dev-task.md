# 研发任务单：客户标签 / 画像 / 分级 P0-b

- task_id: REQ-20260918-CUSTOMER-TAG-PORTRAIT-001 / P0-b
- 指派：backend / development lane
- 契约：`docs/plan/2026-09-18-客户标签画像与分级-P0b后端契约.md`（B1–B4 已冻结）
- 规格：`docs/plan/2026-09-18-客户标签画像与分级实施规格.md`（D1–D5、Q1–Q5 已冻结）

## 批次

| 批次 | 内容 | 产出 | 状态 |
|---|---|---|---|
| P0-b1 | 枚举 + 实体 + Repository + 迁移脚本 | `configure/`、`model/`、`repository/`、`docs/design/migrations/31-*.sql` | **已完成**（编译 + 测试通过） |
| P0-b2 | 标签字典 Provider（page/list/save/update/delete） | `service/tag/**` | **已完成**（编译通过） |
| P0-b3 | 客户标签关联 Provider（save/delete）+ 系统标签装配 | `service/tagging/**`、`CustomerContext` | 关联 Provider **已完成**；系统标签装配待 b6 |
| P0-b4 | 画像 Provider（save）+ info 装配 | `service/portrait/**` | Provider **已完成**；info 装配待 b6 |
| P0-b5 | 分级计算服务（含 breakdown 可解释输出） | `context/CustomerGradeContext` | **已完成**（编译通过） |
| P0-b6 | `customer/customer/page|info` 批量装配 tags/portrait/grade + @Properties 白名单 | 现有 Provider 改造 | **已完成**（编译通过） |
| P0-b7 | 权限与越权回归测试 | `src/test/**` | 待办 |

## 完成定义（DoD）

1. `mvn -o -q compile` 通过。
2. 契约第八节 8 条验收标准全部可验证。
3. 字典写接口对业务员/设计师返回 403。
4. 系统标签不可被任何角色改删。
5. 打标签幂等，重复提交不产生重复关联。
6. 分级返回 `breakdown`（weight/score/reason），`lost` 恒为 C。
7. 跨企业/门店/团队/非指派员工不可读写。
8. `page` 批量装配无 N+1。

## 约束（不可违反）

- 遵循根目录 `AGENTS.md` 与 `rules.md`：五条铁律、Provider 命名、权限先行、事务边界、序列化控制。
- 所有实体含 `Company company`，主键 `@SnowflakeId`，禁止裸外键。
- 生产 `spring.jpa.open-in-view` 保持 `true`，但代码不得依赖 OSIV。
- 不改动客户—房屋—报价—项目主链、状态机、金额/成本/利润口径。
- 不新增线索（Lead）对象（D1）。
