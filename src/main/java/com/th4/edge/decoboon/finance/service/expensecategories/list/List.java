package com.th4.edge.decoboon.finance.service.expensecategories.list;

import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 当前企业费用分类。 */
public interface List extends ModelService<ExpenseCategory, java.util.List<ExpenseCategory>>, Security { }
