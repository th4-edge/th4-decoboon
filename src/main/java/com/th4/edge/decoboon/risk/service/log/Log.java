package com.th4.edge.decoboon.risk.service.log;

import com.th4.edge.decoboon.risk.model.RiskDispositionRequest;
import com.th4.edge.decoboon.risk.model.RiskLogView;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

/** 风险处置时间线。 */
public interface Log extends Provider<RiskDispositionRequest, List<RiskLogView>>, Security {
}
