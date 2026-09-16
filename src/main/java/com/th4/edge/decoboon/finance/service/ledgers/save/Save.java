package com.th4.edge.decoboon.finance.service.ledgers.save;

import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 手工记账。 */
public interface Save extends ModelService<Ledger, Ledger>, Security {
}
