package com.th4.edge.decoboon.finance.service.expenses.page;

import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 费用/报销分页。 */
public interface Page extends ModelService<Expense, org.springframework.data.domain.Page<Expense>>, Security { }
