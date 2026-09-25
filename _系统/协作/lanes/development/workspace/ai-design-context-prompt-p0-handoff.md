# AI设计上下文与Prompt编排 P0.1 交接

## 输入依据

- `REQ-BIZ-20260926-003`
- `DEC-010` AI-DEC-10～12
- `SOL-004` 设计上下文与Prompt编排章节
- `PKT-20260926-AI-DESIGN-001`

## Changed Files

- `src/main/java/com/th4/edge/decoboon/design/configure/DesignGenerationMode.java`
- `src/main/java/com/th4/edge/decoboon/design/configure/DesignGenerationStatus.java`
- `src/main/java/com/th4/edge/decoboon/design/model/DesignGenerationTask.java`
- `src/main/java/com/th4/edge/decoboon/design/model/DesignContextSnapshot.java`
- `src/main/java/com/th4/edge/decoboon/design/model/DesignPromptSnapshot.java`
- `src/main/java/com/th4/edge/decoboon/design/repository/DesignGenerationTaskRepository.java`
- `src/main/java/com/th4/edge/decoboon/design/repository/DesignContextSnapshotRepository.java`
- `src/main/java/com/th4/edge/decoboon/design/repository/DesignPromptSnapshotRepository.java`
- `src/main/java/com/th4/edge/decoboon/design/context/DesignAiContextBuilder.java`
- `src/main/java/com/th4/edge/decoboon/design/context/DesignPromptRenderer.java`
- `src/main/java/com/th4/edge/decoboon/design/context/DesignGenerationContext.java`
- `src/main/java/com/th4/edge/decoboon/design/service/generation/build/Build.java`
- `src/main/java/com/th4/edge/decoboon/design/service/generation/build/support/Build.java`
- `src/main/java/com/th4/edge/decoboon/design/service/generation/info/Info.java`
- `src/main/java/com/th4/edge/decoboon/design/service/generation/info/support/Info.java`
- `src/main/java/com/th4/edge/decoboon/design/service/generation/update/Update.java`
- `src/main/java/com/th4/edge/decoboon/design/service/generation/update/support/Update.java`
- `src/main/java/com/th4/edge/decoboon/design/service/generation/page/Page.java`
- `src/main/java/com/th4/edge/decoboon/design/service/generation/page/support/Page.java`
- `src/main/java/com/th4/edge/decoboon/design/context/DesignGenerationConfirmationContext.java`
- `src/main/java/com/th4/edge/decoboon/design/context/DesignPromptPrivacyContext.java`
- `src/main/java/com/th4/edge/decoboon/design/configure/DesignRenderStatus.java`
- `src/main/java/com/th4/edge/decoboon/design/configure/DesignGenerationAsyncConfigure.java`
- `src/main/java/com/th4/edge/decoboon/design/model/DesignRenderVersion.java`
- `src/main/java/com/th4/edge/decoboon/design/repository/DesignRenderVersionRepository.java`
- `src/main/java/com/th4/edge/decoboon/design/context/DesignGenerationRunner.java`
- `src/main/java/com/th4/edge/decoboon/design/context/DesignGenerationStateContext.java`
- `src/main/java/com/th4/edge/decoboon/design/context/DesignReferenceImageContext.java`
- `src/main/java/com/th4/edge/decoboon/design/context/DesignRenderClient.java`
- `src/main/java/com/th4/edge/decoboon/design/context/SeedreamDesignRenderClient.java`
- `docs/design/migrations/63-AI设计上下文与Prompt编排.sql`

## Verification

- `mvn -q -DskipTests compile` PASS。
- `mvn -q -DskipTests package` PASS。
- 本地假模型服务 Runner 端到端自测 PASS：`PromptReady -> Generating -> DraftReady`，效果图版本保存成功。
- 独立临时自测 PASS：
  - 完整上下文置信度为 High；
  - 区域尺寸和客户诉求进入 VisualPrompt；
  - 手机号、完整地址和客户姓名未进入 Prompt；
  - 无CAD/户型图时构建阻断。
- `ProviderSecurityAcceptanceTest` 当前仅因路由总数 341→345 失败；新增 build/info/update/page Provider 均已继承 Security。
- P0.4 字段收口已完成：内部成本、用量、请求号、模型参数、原始失败信息和 sourceDigest 不对外下发；TalkBrief 仅 Designer 返回。

## Risks

- CAD/户型图存在但解析能力尚未接入，当前仅生成可审核上下文与Prompt。
- 外部模型调用、任务重试、模型凭证和效果图版本已进入P0.2；配额和成本计算仍待后续。
- 同一上下文并发提交只做应用层幂等查询，尚未增加数据库唯一约束。
- 测试与发布脚本属于 acceptance/release 范围，本分支未越界修改。

## Acceptance Needed

- `ProviderSecurityAcceptanceTest` 路由总数更新为 345，并覆盖 `design/generation/{build,info,update,page}` 未授权断言。
- 增加四角色字段最小化和 TalkBrief 不下发断言；业务员、项目经理、财务、业主直连拒绝。
- `scripts/build-release.sh` 的 `MIGRATION_END` 上调到 63。
- 迁移 63 在隔离 MySQL 演练。
- 设计上下文正反向用例、PII隔离、无CAD阻断、上下文冲突与幂等用例由 acceptance 固化到 `src/test`。

## Next Action

- P0.4：先由 product-planning 冻结 App 页面、状态和错误恢复，再实现设计师入口。
- P0.4：App设计阶段入口、上下文/Prompt预览、版本确认。
