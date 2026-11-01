# App负责人弹层自动化限制

- 当前小程序页面已由财务账号打开：客户负责人控件为 `cd__role--clickable`。
- miniprogram-automator 对 `.cd__role` / `.cd__role-label` 执行 `tap()` 后，未触发页面 `@tap`，弹层元素未进入页面树。
- 因此本次 App 页面点击只保留为自动化环境限制，不判产品失败。
- App 与 Web 共用 `user/employee/page`；该 Provider 修复后已用财务 token 实测返回9名候选，后续以手工/真机点击补齐 App UI 证据。
