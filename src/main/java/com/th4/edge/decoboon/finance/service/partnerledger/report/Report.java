package com.th4.edge.decoboon.finance.service.partnerledger.report;

import com.th4.edge.decoboon.finance.model.PartnerLedger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 供应商应付台账报表。 */
public interface Report extends Provider<Payable, java.util.List<PartnerLedger>>, Security {
}
