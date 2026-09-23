package com.th4.edge.decoboon.statistic.service.boss.quote.report;

import com.th4.edge.decoboon.statistic.model.BossQuoteQuery;
import com.th4.edge.decoboon.statistic.model.BossQuoteReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业负责人报价谈单客户经营报告入口。 */
public interface Report extends Provider<BossQuoteQuery, BossQuoteReport>, Security {}
