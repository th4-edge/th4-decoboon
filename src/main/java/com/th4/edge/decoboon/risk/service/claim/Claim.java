package com.th4.edge.decoboon.risk.service.claim;

import com.th4.edge.decoboon.risk.model.RiskAssignRequest;
import com.th4.edge.decoboon.risk.model.RiskItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 风险认领。 */
public interface Claim extends Provider<RiskAssignRequest, RiskItem>, Security {
}
