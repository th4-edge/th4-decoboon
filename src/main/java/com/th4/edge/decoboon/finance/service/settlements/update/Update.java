package com.th4.edge.decoboon.finance.service.settlements.update;

import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 确认或关闭项目结算。 */
public interface Update extends ModelService<Settlement, Settlement>, Security {
}
