package com.th4.edge.decoboon.statistic.service.boss.contractchange.report;

import com.th4.edge.decoboon.statistic.model.BossContractChangeQuery;
import com.th4.edge.decoboon.statistic.model.BossContractChangeReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业负责人增减项经营工作台入口。 */
public interface Report extends Provider<BossContractChangeQuery, BossContractChangeReport>, Security {
}
