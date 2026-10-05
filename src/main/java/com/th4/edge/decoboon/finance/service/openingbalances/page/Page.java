package com.th4.edge.decoboon.finance.service.openingbalances.page;

import com.th4.edge.decoboon.finance.model.OpeningBalance;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 企业负责人/财务查看期初资金余额。 */
public interface Page extends ModelService<OpeningBalance, org.springframework.data.domain.Page<OpeningBalance>>, Security {
}
