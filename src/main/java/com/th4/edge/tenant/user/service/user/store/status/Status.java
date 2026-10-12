package com.th4.edge.tenant.user.service.user.store.status;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;

/** 门店启用/停用（`P0D-04` / `DESIGN-BIZ-20261013-002` §3：独立动作 + 仅 BOSS + 审计留痕）。 */
public interface Status extends Provider<Store, Store>, Security {
}
