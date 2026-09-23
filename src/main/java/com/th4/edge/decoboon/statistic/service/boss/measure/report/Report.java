package com.th4.edge.decoboon.statistic.service.boss.measure.report;

import com.th4.edge.decoboon.statistic.model.BossMeasureQuery;
import com.th4.edge.decoboon.statistic.model.BossMeasureReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业负责人量房经营报告入口。 */
public interface Report extends Provider<BossMeasureQuery, BossMeasureReport>, Security {}
