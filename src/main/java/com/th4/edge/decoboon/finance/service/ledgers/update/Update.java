package com.th4.edge.decoboon.finance.service.ledgers.update;

import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 企业负责人更正手工记账元数据。 */
public interface Update extends ModelService<Ledger, Ledger>, Security {
}
