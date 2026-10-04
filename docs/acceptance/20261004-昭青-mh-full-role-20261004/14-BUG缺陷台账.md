# BUG 缺陷台账

| ID | 严重度 | 状态 | 标题 | 复现 | 修复 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| XA-20261004-001 | P1 | FIXED_VERIFIED | PM 手工新建任务失败 | 旧“新建任务”无 `catalogId`，保存返回“工艺任务不能为空” | 新建任务统一进入工艺任务库；`task/edit` 新建模式自动重定向；兼容 `projectId + stageId` | PM 选择“拆除--保温墙铲除”后成功生成 `catalog_item_id=228668580261003264` 的任务；`verify:mp-weixin` PASS |
| XA-20261004-002 | P2 | FIXED_VERIFIED | 无权限深链打开财务页 | Marketer 直达资金台账先打开页面后由接口 403 | App `onShow` 补 `hasRouteRule + canVisit` 页面栈检查；接口拒绝保留 | Marketer 深链打开后被 `App.onShow` 返回首页；`check:navigation` PASS |
| XA-20261004-003 | P2 | FIXED_VERIFIED | TeamLeader 无团队页面显示加载失败 | 无 `team_id` 时团队页直接请求报表 | 新增 `useTeamContext`；无团队统一提示并返回首页 | 临时移除团队绑定后访问设计调度返回首页；随后恢复 `team_id`；`verify:mp-weixin` PASS |

## 保持产品裁决

- `XA-20261004-004`：主材选材“客户已确认”的身份证据仍是 `GAP`，本次未改业务规则，也没有执行员工代客户确认。
