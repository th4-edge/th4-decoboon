package com.th4.edge.decoboon.finance.service.ledgercategories.save;

import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 企业自定义手工记账分类。 */
public interface Save extends ModelService<LedgerCategory, LedgerCategory>, Security {
}
