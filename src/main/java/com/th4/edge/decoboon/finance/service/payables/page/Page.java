package com.th4.edge.decoboon.finance.service.payables.page;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 应付分页。 */
public interface Page extends ModelService<Payable, org.springframework.data.domain.Page<Payable>>, Security {
}
