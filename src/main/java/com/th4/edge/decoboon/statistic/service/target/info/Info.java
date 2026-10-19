package com.th4.edge.decoboon.statistic.service.target.info;

import com.th4.edge.decoboon.statistic.model.BusinessTarget;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 月度经营目标详情（公司目标 / 本店目标，DEC-076）。 */
public interface Info extends Provider<BusinessTarget, BusinessTarget>, Security {
}
