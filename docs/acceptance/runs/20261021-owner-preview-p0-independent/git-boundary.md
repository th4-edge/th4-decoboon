# Git 与改动边界

## 开始时

- 后端：`dev`，HEAD `dbde1e3`，工作树已有本功能未提交改动及其它文档改动
- App：`dev`，HEAD `ad3fa58`，工作树已有本功能、业主首页 v2 和本地配置未提交改动
- 验收未执行 reset、checkout、stash、clean、commit 或 push

## 验收产生的文件

仅新增：

- `docs/acceptance/runs/20261021-owner-preview-p0-independent/**`
- 外部备份目录 `/Users/zhangwei/.codex/evidence-backups/20261021-owner-preview-p0/`（已移出仓库）

## 初轮验收边界

- 初轮验收未修改 `src/main/**`、`src/test/**`、App `src/**`、`scripts/**`
- 未修改研发的 `.env`；App 走查通过 shell 环境变量注入 `VITE_API_BASE=http://192.168.31.249:18090/api/`
- 未触碰生产库、生产凭证或业主 token
- 验收结束后两个仓库原有未提交改动保持存在

## 功能相关未提交文件（交回研发）

- 后端：`CustomerJourneyContext`、`CustomerJourney`、`JourneyViewMode`、owner-preview Provider/审计模型与测试、迁移 123
- App：`owner-preview.vue`、三个预览组件、`useOwnerPreviewJourney`、journey API/model、路由/权限/入口、owner-preview 契约脚本

## 后续修复产生的产品代码改动

- `docs/design/migrations/run-migrations.sh.template`
- `src/main/java/com/th4/edge/decoboon/project/repository/ProjectRepository.java`
- `src/main/java/com/th4/edge/decoboon/project/repository/ProjectStageRepository.java`
- `src/main/java/com/th4/edge/decoboon/project/repository/ProjectTaskRepository.java`
- `src/main/java/com/th4/edge/decoboon/project/repository/ProjectContractRepository.java`
- `src/main/java/com/th4/edge/decoboon/project/service/projects/info/support/Info.java`
- `src/main/java/com/th4/edge/decoboon/project/service/stages/page/support/Page.java`
- `src/main/java/com/th4/edge/decoboon/project/service/members/list/support/List.java`
- `src/main/java/com/th4/edge/decoboon/project/service/tasks/page/support/Page.java`
- `src/main/java/com/th4/edge/decoboon/project/service/contract/info/support/Info.java`
- `src/main/java/com/th4/edge/decoboon/project/service/ownerpreview/info/support/Info.java`
- `src/test/java/com/th4/edge/decoboon/project/OwnerPreviewInfoAcceptanceTest.java`

App 源码未修改；仅用环境变量切换测试 API 并构建。

## 非本功能但同样未提交

- App：业主首页 v2 相关 `pages/index/index.vue`、`styles/home.scss`、`OwnerPhotoGrid.vue`、`.env`、`package.json` 等
- 后端：项目交付域文档、工作记录、收款冲正前置备份等

发布前必须由研发确认：本功能提交不夹带业主首页 v2 或其它未完成改动。
