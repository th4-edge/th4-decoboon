package com.th4.edge.decoboon.finance.service.receipts.page;

import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 收款分页。 */
public interface Page extends ModelService<Receipt, org.springframework.data.domain.Page<Receipt>>, Security {
}
