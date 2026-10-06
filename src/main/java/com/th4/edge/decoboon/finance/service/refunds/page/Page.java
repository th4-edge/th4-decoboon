package com.th4.edge.decoboon.finance.service.refunds.page;

import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Page extends ModelService<ReceiptRefund, org.springframework.data.domain.Page<ReceiptRefund>>, Security {
}
