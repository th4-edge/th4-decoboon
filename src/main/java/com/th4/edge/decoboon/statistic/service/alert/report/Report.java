package com.th4.edge.decoboon.statistic.service.alert.report;

import com.th4.edge.decoboon.statistic.model.BusinessAlertQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业经营预警报表。 */
public interface Report extends Provider<BusinessAlertQuery, BusinessAlertReport>, Security {
}
