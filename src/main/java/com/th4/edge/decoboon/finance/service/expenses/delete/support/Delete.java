package com.th4.edge.decoboon.finance.service.expenses.delete.support;

import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.finance.context.ExpenseAccessContext;
import com.th4.edge.decoboon.finance.context.ExpenseProjectionContext;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.model.ExpenseAttachment;
import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.decoboon.finance.repository.ExpenseRepository;
import com.th4.edge.decoboon.finance.repository.ExpenseRevisionRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 删除从未提交的费用草稿。 */
@Note("删除费用草稿")
@Properties(allow = {
        @AllowProperties(model = Expense.class, properties = {"expenseId", "expenseNo", "status", "version"}),
        @AllowProperties(model = ExpenseCategory.class, properties = {"categoryId", "name"}),
        @AllowProperties(model = ExpenseAttachment.class, properties = {"attachmentId", "fileUrl"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "name"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("expense/expenses/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.finance.service.expenses.delete.Delete {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long expenseId;

    private final ExpenseRepository repository;
    private final ExpenseRevisionRepository revisionRepository;
    private final ExpenseAccessContext accessContext;
    private final ExpenseProjectionContext projectionContext;

    public Delete(ExpenseRepository repository,
                  ExpenseRevisionRepository revisionRepository,
                  ExpenseAccessContext accessContext,
                  ExpenseProjectionContext projectionContext) {
        this.repository = repository;
        this.revisionRepository = revisionRepository;
        this.accessContext = accessContext;
        this.projectionContext = projectionContext;
    }

    @Override
    @Transactional
    public Expense call(Expense request) {
        Employee current = accessContext.requireSubmitter(employee);
        Expense expense = accessContext.requireVisible(current, expenseId);
        accessContext.requireOwnerOrManager(expense, current);
        if (expense.getStatus() != ExpenseStatus.Draft) {
            throw new DefinedException(new DefaultErrorCode("409", "只有草稿费用可以删除"));
        }
        if (!revisionRepository.findByCompany_ComIdAndExpense_ExpenseIdOrderByVersionDesc(
                current.getCompany().getComId(), expenseId).isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("409", "存在提交版本的费用不能删除"));
        }
        repository.delete(expense);
        return projectionContext.apply(expense, current);
    }
}
