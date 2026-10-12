package com.th4.edge.tenant.user.service.user.store.update;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;

/**
 * 门店更新（`P0D-04`）——按 `AGENTS.md` 固定能力名 `update` 提供的别名路由，
 * 与规格建议的独立动作 `user/store/status` 共用同一实现（启用/停用是唯一可更新项）。
 */
public interface Update extends Provider<Store, Store>, Security {
}
