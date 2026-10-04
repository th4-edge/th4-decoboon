package com.th4.edge.decoboon.finance.service.ledgers.correct;

import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 企业负责人对手工记账资金事实执行冲正重记。 */
public interface Correct extends ModelService<Ledger, Ledger>, Security {
}
