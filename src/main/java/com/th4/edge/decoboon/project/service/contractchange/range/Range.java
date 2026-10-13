package com.th4.edge.decoboon.project.service.contractchange.range;

import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeRange;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 读取增减项可变更来源与当前有效剩余投影，不落库。 */
public interface Range extends Provider<ContractChange, ContractChangeRange>, Security {
}
