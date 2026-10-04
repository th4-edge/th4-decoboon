package com.th4.edge.decoboon.finance.service.ledgercategories.delete;

import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 企业自定义手工记账分类逻辑删除。 */
public interface Delete extends ModelService<LedgerCategory, LedgerCategory>, Security {
}
