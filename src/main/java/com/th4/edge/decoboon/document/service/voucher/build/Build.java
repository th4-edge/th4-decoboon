package com.th4.edge.decoboon.document.service.voucher.build;

import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 生成凭证 PDF 并落库。 */
public interface Build extends Provider<Ledger, VoucherRecord>, Security {
}
