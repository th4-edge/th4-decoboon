package com.th4.edge.decoboon.statistic.service.boss.workbench;

import com.th4.edge.decoboon.statistic.model.BossWorkbench;
import com.th4.edge.decoboon.statistic.model.BossWorkbenchQuery;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业负责人首页聚合服务。 */
public interface Workbench extends Provider<BossWorkbenchQuery, BossWorkbench>, Security {
}
