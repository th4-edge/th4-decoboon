package com.th4.edge.decoboon.finance.service.ledgercategories.tree;

import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

/** 手工记账业务分类树。 */
public interface Tree extends Provider<LedgerCategory, List<LedgerCategory>>, Security {
}
