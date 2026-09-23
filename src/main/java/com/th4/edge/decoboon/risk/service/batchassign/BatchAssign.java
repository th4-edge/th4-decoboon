package com.th4.edge.decoboon.risk.service.batchassign;

import com.th4.edge.decoboon.risk.model.RiskBatchAssignRequest;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 风险批量指派（单次上限 50 条）。 */
public interface BatchAssign extends Provider<RiskBatchAssignRequest, Integer>, Security {
}
