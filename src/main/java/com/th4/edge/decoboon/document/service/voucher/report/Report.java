package com.th4.edge.decoboon.document.service.voucher.report;

import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.document.model.VoucherReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 凭证筛选汇总。 */
public interface Report extends Provider<VoucherRecord, VoucherReport>, Security {
}
