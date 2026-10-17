package com.th4.edge.decoboon.blueprint.service.runtime.info;

import com.th4.edge.decoboon.blueprint.model.BlueprintRuntime;
import com.th4.edge.decoboon.blueprint.model.BlueprintRuntimeQuery;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业务蓝图运行态。 */
public interface Info extends Provider<BlueprintRuntimeQuery, BlueprintRuntime>, Security {
}
