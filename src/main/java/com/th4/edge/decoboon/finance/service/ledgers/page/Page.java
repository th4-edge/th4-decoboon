package com.th4.edge.decoboon.finance.service.ledgers.page;

import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 资金流水分页。 */
public interface Page extends ModelService<Ledger, org.springframework.data.domain.Page<Ledger>>, Security {
}
