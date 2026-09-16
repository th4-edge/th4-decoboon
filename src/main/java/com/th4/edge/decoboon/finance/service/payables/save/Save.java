package com.th4.edge.decoboon.finance.service.payables.save;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 新建应付单。 */
public interface Save extends ModelService<Payable, Payable>, Security {
}
