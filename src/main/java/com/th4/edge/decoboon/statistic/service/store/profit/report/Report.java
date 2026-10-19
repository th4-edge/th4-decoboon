package com.th4.edge.decoboon.statistic.service.store.profit.report;

import com.th4.edge.decoboon.statistic.model.StoreProfitReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 门店经营利润（参考）报告（MET-011 草案）。 */
public interface Report extends Provider<com.th4.edge.decoboon.statistic.model.StoreProfitRequest, StoreProfitReport>, Security {
}
