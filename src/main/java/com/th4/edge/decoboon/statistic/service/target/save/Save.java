package com.th4.edge.decoboon.statistic.service.target.save;

import com.th4.edge.decoboon.statistic.model.BusinessTarget;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 保存企业月度经营目标。 */
public interface Save extends Provider<BusinessTarget, BusinessTarget>, Security {
}
