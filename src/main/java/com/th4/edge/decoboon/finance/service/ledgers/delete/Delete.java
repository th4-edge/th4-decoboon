package com.th4.edge.decoboon.finance.service.ledgers.delete;

import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 企业负责人对已作废手工流水执行逻辑作废。 */
public interface Delete extends ModelService<Ledger, Ledger>, Security {
}
