package com.th4.edge.decoboon.finance.service.ledgercategories.update;

import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 维护企业自定义手工记账分类。 */
public interface Update extends ModelService<LedgerCategory, LedgerCategory>, Security {
}
