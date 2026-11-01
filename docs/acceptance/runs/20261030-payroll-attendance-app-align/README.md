# 2026-10-30 App 月度考勤应出勤调整对齐

## 范围

- 业务裁决：`USER-DEC-20261029-006`
- 变更记录：`CHG-20261030-002`
- App 页面：`src/subpackages/payroll/pages/attendance-edit.vue`
- 非范围：工资公式、权限矩阵、状态机、员工发放、DDL/迁移

## 结论

状态：`DEV_VERIFIED / PENDING_REAL_UI_ACCEPTANCE`

App 与 Web 均采用：

```text
薪酬方案派生默认应出勤 -> 允许月度 1～31 天调整
-> 保存为考勤快照 -> 工资计算读取快照
```

无生效薪酬方案时阻断保存，不使用 22 或其他本地默认值兜底。

## 验证

- App `type-check`：PASS
- App 薪酬闭环契约：PASS
- App `check:client-copy`：PASS
- App `check:ui`：PASS
- App `build:mp-weixin`：PASS；构建产物 `dist/build/mp-weixin` 已包含可编辑 `scheduledDays`
- 后端定向薪酬测试：17/17 PASS
- Java 22 后端全量：1129 tests / 0 failures / 0 errors / 0 skipped
- Web type-check：PASS

原始 App 精简验证输出：`app-verification.log`  
后端 JDK22 定向与全量结果摘要：`backend-verification.txt`

## 未覆盖

- 真实财务账号在 App/微信开发者工具中修改应出勤并回读
- 工资单生成后验证同一应出勤快照进入工资项
- 独立验收与发布

## 下一步

使用受控测试企业完成真实 App 页面提交、回读和工资项核对，再决定发布。
