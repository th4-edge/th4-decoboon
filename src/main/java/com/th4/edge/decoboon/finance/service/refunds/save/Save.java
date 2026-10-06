package com.th4.edge.decoboon.finance.service.refunds.save;

import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Save extends ModelService<ReceiptRefund, ReceiptRefund>, Security {
}
