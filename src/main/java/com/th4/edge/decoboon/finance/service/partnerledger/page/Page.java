package com.th4.edge.decoboon.finance.service.partnerledger.page;

import com.th4.edge.decoboon.finance.model.PartnerLedger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 供应商往来分页。 */
public interface Page extends Provider<Payable, org.springframework.data.domain.Page<PartnerLedger>>, Security {
}
