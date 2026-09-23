package com.th4.edge.decoboon.statistic.service.risk.report;

import com.th4.edge.decoboon.statistic.model.RiskCenterQuery;
import com.th4.edge.decoboon.statistic.model.RiskCenterReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 风险中心读侧报表。 */
public interface Report extends Provider<RiskCenterQuery, RiskCenterReport>, Security {
}
