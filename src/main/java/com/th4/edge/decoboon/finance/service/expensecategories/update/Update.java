package com.th4.edge.decoboon.finance.service.expensecategories.update;

import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 维护企业费用分类。 */
public interface Update extends ModelService<ExpenseCategory, ExpenseCategory>, Security { }
