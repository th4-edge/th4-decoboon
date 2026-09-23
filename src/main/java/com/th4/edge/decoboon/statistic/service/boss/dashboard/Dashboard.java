package com.th4.edge.decoboon.statistic.service.boss.dashboard;

import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BossDashboardQuery;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业负责人经营驾驶舱。 */
public interface Dashboard extends Provider<BossDashboardQuery, BossDashboard>, Security {
}
