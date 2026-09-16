package com.th4.edge.decoboon.finance.service.payables.update;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 更新应付单。 */
public interface Update extends ModelService<Payable, Payable>, Security {
}
