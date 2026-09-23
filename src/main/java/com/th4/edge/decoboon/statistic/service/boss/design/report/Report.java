package com.th4.edge.decoboon.statistic.service.boss.design.report;

import com.th4.edge.decoboon.statistic.model.BossDesignQuery;
import com.th4.edge.decoboon.statistic.model.BossDesignReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业负责人设计经营报告入口。 */
public interface Report extends Provider<BossDesignQuery, BossDesignReport>, Security {}
