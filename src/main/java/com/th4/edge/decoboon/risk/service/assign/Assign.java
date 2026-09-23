package com.th4.edge.decoboon.risk.service.assign;

import com.th4.edge.decoboon.risk.model.RiskAssignRequest;
import com.th4.edge.decoboon.risk.model.RiskItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 风险指派。 */
public interface Assign extends Provider<RiskAssignRequest, RiskItem>, Security {
}
