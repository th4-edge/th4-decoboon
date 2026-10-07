package com.th4.edge.decoboon.finance.service.ledgercategories.tree.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.LedgerCategoryContext;
import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 手工记账业务分类树实现。 */
@Note("手工记账业务分类树")
@Properties(allow = @AllowProperties(model = LedgerCategory.class,
        properties = {"categoryId", "name", "description", "direction", "code", "builtIn", "sort", "active", "children"}))
@Service("finance/ledger-category/tree")
@Scope(ScopeTypes.PROTOTYPE)
public class Tree implements com.th4.edge.decoboon.finance.service.ledgercategories.tree.Tree {

    @User
    private Employee employee;
    private final LedgerCategoryContext categoryContext;
    private final FinanceAccessContext accessContext;

    public Tree(LedgerCategoryContext categoryContext, FinanceAccessContext accessContext) {
        this.categoryContext = categoryContext;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public List<LedgerCategory> call(LedgerCategory request) {
        Employee current = accessContext.requireReader(employee);
        return categoryContext.tree(current);
    }
}
