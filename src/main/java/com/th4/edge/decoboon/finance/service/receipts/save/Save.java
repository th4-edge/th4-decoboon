package com.th4.edge.decoboon.finance.service.receipts.save;

import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 新建客户收款。 */
public interface Save extends ModelService<Receipt, Receipt>, Security {
}
