package com.th4.edge.decoboon.statistic.service.business.report;

import com.th4.edge.decoboon.statistic.model.BusinessAnalysisQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAnalysisReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业务分析报表（渠道 / 区域 / 客单 / 转化）。 */
public interface Report extends Provider<BusinessAnalysisQuery, BusinessAnalysisReport>, Security {
}
