# 企业管理 · EMG-V2 UI 复核

## 1. 真实运行态

- App 使用微信开发者工具自动化，店长账号进入：
  - `subpackages/company/pages/members`
  - `subpackages/company/pages/invite`
  - `subpackages/company/pages/member-detail`
- Web 使用真实 Vite 页面，BOSS 与店长账号分别进入“成员与组织”。

## 2. 已确认页面事实

- App 店长列表对自己和 BOSS 无操作；对普通业务员显示交接、停用、离职。
- App 邀请页店长无店长、无 TeamLeader 选项。
- App 员工详情无门店选择器、店长岗位选项。
- Web BOSS 可打开调岗/调店弹窗并完成调整。
- Web 店长邀请页无店长/TeamLeader；目标门店固定本店。
- Web 店长不能操作 BOSS、其他店长或自己。
- 停用/恢复/离职动作在 Web 有明确确认与错误提示。

## 3. 证据

- `screenshots/app-storemanager-members.png`
- `screenshots/app-invite-storemanager-roles.png`
- `screenshots/app-member-detail-no-store-manager-option.png`
- `screenshots/web-boss-members-lifecycle.png`
- `screenshots/web-storemanager-invite-roles.png`
- `app-lifecycle-acceptance.json` / `app-lifecycle-acceptance.mjs`

## 4. 透明说明

App 自动化脚本在登录后会清空首页其他域已存在的已知 403 console 错误，只统计本切片生命周期页面的 console error；因此报告中的 `consoleErrors: []` 仅代表本切片页面，不宣称全应用零错误。

## 5. UI 结论

本切片页面结构、角色可见性和关键动作入口未发现阻断性缺陷。最终视觉与交互验收仍需独立验收责任方结合真机/真实浏览器确认。
