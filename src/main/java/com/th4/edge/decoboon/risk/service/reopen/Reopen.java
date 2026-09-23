package com.th4.edge.decoboon.risk.service.reopen;

import com.th4.edge.decoboon.risk.model.RiskDispositionRequest;
import com.th4.edge.decoboon.risk.model.RiskItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 重开风险（必填原因，轮次递增）。 */
public interface Reopen extends Provider<RiskDispositionRequest, RiskItem>, Security {
}
