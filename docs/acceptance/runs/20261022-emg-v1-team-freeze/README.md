# EMG-V1 Team 冻结收口 · 独立验收证据包

- 问题：`PI-20261022-005`
- 验收批次：`ACC-PI-20261022-005-EMG-V1`
- 业务裁决：`DEC-085` / `EV-353`
- 日期：2026-10-22
- 环境：本地隔离 MySQL 3307、后端 8090、Web 8080、微信开发者工具自动化 9420
- 结论：**PASS**（仅 `EMG-V1` 切片，不代表问题 CLOSED）

## 执行摘要

1. Web BOSS 真实页面无 Team 管理/经营入口；邀请岗位无 TeamLeader。
2. App StoreManager 真实运行态工作台无 Team 入口；设计调度只显示“成员管理”；邀请页不出现 Team。
3. 后端拒绝邀请/成员更新中的 Team 写入；无 Team 写 Provider。
4. 历史 Team 可只读查询，不赋予权限。
5. App/Web 旧 Team 深链接均被拦截。
6. 跨企业员工返回 404；TeamLeader 登录返回 403。
7. 验收中发现并修复 Web 邀请按钮插槽错误，修复后重测通过。

## 文件

- `ACCEPT-EMG-V1-Team冻结收口.md`：业务验收记录
- `app-team-freeze-acceptance.json`：App 运行态断言与路径
- `app-team-freeze-acceptance.mjs`：App 自动化脚本
- `web-boss-*.yml/png`：Web 快照与截图
- `api-*.json`：真实 HTTP 响应
- `db-after-api.jsonl`：落库前后事实
- `backend-targeted-tests.log`：后端定向测试 32/0/0
- `backend-isolated-full.log`：隔离全量测试阻塞证据（非本切片编译错误）
- `web-build.log`：Web 最终构建日志
