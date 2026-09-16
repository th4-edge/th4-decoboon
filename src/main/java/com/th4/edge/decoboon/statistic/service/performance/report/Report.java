package com.th4.edge.decoboon.statistic.service.performance.report;

import com.th4.edge.decoboon.statistic.model.PerformanceQuery;
import com.th4.edge.decoboon.statistic.model.PerformanceReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 管理层人员绩效自动考核报表。 */
public interface Report extends Provider<PerformanceQuery, PerformanceReport>, Security {
}
