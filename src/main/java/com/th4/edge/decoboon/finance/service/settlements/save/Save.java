package com.th4.edge.decoboon.finance.service.settlements.save;

import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 生成或重算项目结算。 */
public interface Save extends ModelService<Settlement, Settlement>, Security {
}
