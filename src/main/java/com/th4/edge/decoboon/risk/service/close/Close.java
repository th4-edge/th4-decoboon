package com.th4.edge.decoboon.risk.service.close;

import com.th4.edge.decoboon.risk.model.RiskDispositionRequest;
import com.th4.edge.decoboon.risk.model.RiskItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 关闭风险（必填处置结果）。 */
public interface Close extends Provider<RiskDispositionRequest, RiskItem>, Security {
}
