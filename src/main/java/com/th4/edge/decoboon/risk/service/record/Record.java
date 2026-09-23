package com.th4.edge.decoboon.risk.service.record;

import com.th4.edge.decoboon.risk.model.RiskDispositionRequest;
import com.th4.edge.decoboon.risk.model.RiskItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 写处置记录 / 催办。 */
public interface Record extends Provider<RiskDispositionRequest, RiskItem>, Security {
}
