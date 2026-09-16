package com.th4.edge.decoboon.finance.service.settlements.page;

import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 项目结算分页。 */
public interface Page extends ModelService<Settlement, org.springframework.data.domain.Page<Settlement>>, Security {
}
