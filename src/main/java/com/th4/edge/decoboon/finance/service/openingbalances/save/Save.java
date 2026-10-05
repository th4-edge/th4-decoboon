package com.th4.edge.decoboon.finance.service.openingbalances.save;

import com.th4.edge.decoboon.finance.model.OpeningBalance;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 企业负责人/财务保存期初资金余额。 */
public interface Save extends ModelService<OpeningBalance, OpeningBalance>, Security {
}
