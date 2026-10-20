package com.th4.edge.decoboon.finance.service.receipts.reverse;

import com.th4.edge.decoboon.finance.model.ReceiptReversal;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 收款冲正。 */
public interface Reverse extends Provider<ReceiptReversal, ReceiptReversal>, Security {
}
