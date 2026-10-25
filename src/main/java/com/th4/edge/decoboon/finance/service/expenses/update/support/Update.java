package com.th4.edge.decoboon.finance.service.expenses.update.support;

import com.th4.edge.decoboon.finance.configure.ExpensePaymentMode;
import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.finance.context.ExpenseAccessContext;
import com.th4.edge.decoboon.finance.context.ExpenseCategoryContext;
import com.th4.edge.decoboon.finance.context.ExpenseLifecycleContext;
import com.th4.edge.decoboon.finance.context.ExpenseProjectionContext;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.model.ExpenseAttachment;
import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.ExpenseRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 修改费用草稿并按目标状态推进生命周期。 */
@Note("修改费用报销并推进状态")
@Properties(allow = {
        @AllowProperties(model = Expense.class, properties = {
                "expenseId", "expenseNo", "paymentMode", "amount", "expenseDate", "description", "status",
                "version", "submittedAt", "approvedAt", "approvalNote", "selfApproved", "category", "project",
                "store", "requester", "owner", "payeeEmployee", "approvedBy", "attachments", "allowedActions", "nextAction", "createTime", "updateTime"
        }),
        @AllowProperties(model = ExpenseCategory.class,
                properties = {"categoryId", "code", "name", "description", "builtIn", "active", "sort"}),
        @AllowProperties(model = ExpenseAttachment.class, properties = {
                "attachmentId", "documentType", "fileUrl", "fileName", "sha256", "size", "uploadedBy", "uploadedAt"
        }),
        @AllowProperties(model = Payable.class, properties = {
                "payableId", "name", "type", "amount", "paidAmount", "dueDate", "status", "payeeEmployee", "payeeName"
        }),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("expense/expenses/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.finance.service.expenses.update.Update {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long expenseId;
    @AutoInject
    private String status;
    @AutoInject
    @Length(max = 5000)
    private String approvalNote;
    @AutoInject
    private ExpensePaymentMode paymentMode;
    @AutoInject
    private BigDecimal amount;
    @AutoInject
    private LocalDate expenseDate;
    @AutoInject
    @Length(max = 5000)
    private String description;
    @AutoInject
    private Long categoryId;
    @AutoInject
    private Long projectId;
    @AutoInject
    private Long storeId;
    @AutoInject
    private Long employeeId;
    @AutoInject
    private Long payeeEmployeeId;
    @AutoInject
    private List<ExpenseAttachment> attachments;

    private final ExpenseRepository repository;
    private final ExpenseAccessContext accessContext;
    private final ExpenseCategoryContext categoryContext;
    private final ExpenseLifecycleContext lifecycleContext;
    private final ExpenseProjectionContext projectionContext;

    public Update(ExpenseRepository repository,
                  ExpenseAccessContext accessContext,
                  ExpenseCategoryContext categoryContext,
                  ExpenseLifecycleContext lifecycleContext,
                  ExpenseProjectionContext projectionContext) {
        this.repository = repository;
        this.accessContext = accessContext;
        this.categoryContext = categoryContext;
        this.lifecycleContext = lifecycleContext;
        this.projectionContext = projectionContext;
    }

    @Override
    @Transactional
    public Expense call(Expense request) {
        Employee current = accessContext.requireSubmitter(employee);
        Expense expense = accessContext.requireVisible(current, expenseId);
        ExpenseStatus target = lifecycleContext.parseTarget(status);

        if (target == null || target == ExpenseStatus.Draft) {
            requireEditable(expense);
            accessContext.requireOwnerOrManager(expense, current);
            applyEdits(expense, current);
            Expense saved = repository.save(expense);
            return detail(current, saved);
        }
        if (target == ExpenseStatus.Submitted) {
            requireEditable(expense);
            accessContext.requireOwnerOrManager(expense, current);
            applyEdits(expense, current);
            return detail(current, lifecycleContext.submit(expense, current));
        }
        if (target == ExpenseStatus.Cancelled) {
            return detail(current, lifecycleContext.cancel(expense, current));
        }
        if (target == ExpenseStatus.Returned) {
            return detail(current, lifecycleContext.returnForCorrection(expense, current, approvalNote));
        }
        if (target == ExpenseStatus.Approved) {
            return detail(current, lifecycleContext.approve(expense, current, approvalNote));
        }
        if (target == ExpenseStatus.Rejected) {
            return detail(current, lifecycleContext.reject(expense, current, approvalNote));
        }
        throw new DefinedException(new DefaultErrorCode("409", "不支持的费用状态转换"));
    }

    private void requireEditable(Expense expense) {
        if (expense.getStatus() != ExpenseStatus.Draft && expense.getStatus() != ExpenseStatus.Returned) {
            throw new DefinedException(new DefaultErrorCode("409", "当前费用状态不能修改"));
        }
    }

    private void applyEdits(Expense expense, Employee current) {
        if (employeeId != null) {
            Employee owner = accessContext.resolveOwner(current, employeeId);
            expense.setOwner(owner);
            if (expense.getPaymentMode() == ExpensePaymentMode.Reimbursement
                    || paymentMode == ExpensePaymentMode.Reimbursement) {
                expense.setPayeeEmployee(owner);
            }
        }
        if (paymentMode != null) expense.setPaymentMode(paymentMode);
        if (amount != null) expense.setAmount(normalizedAmount(amount));
        if (expenseDate != null) expense.setExpenseDate(expenseDate);
        if (description != null) expense.setDescription(description.trim());
        if (categoryId != null) expense.setCategory(categoryContext.select(current, categoryId));

        if (projectId != null) {
            Project project = accessContext.resolveProject(current, projectId);
            expense.setProject(project);
            expense.setStore(accessContext.resolveStore(current, storeId, project));
        } else if (expense.getProject() != null) {
            expense.setStore(expense.getProject().getStore() == null ? expense.getStore() : expense.getProject().getStore());
        } else if (storeId != null) {
            expense.setStore(accessContext.resolveStore(current, storeId, null));
        }

        if (expense.getOwner() == null) expense.setOwner(current);
        Employee payee = accessContext.resolvePayee(current, expense.getOwner(), payeeEmployeeId,
                expense.getPaymentMode());
        if (expense.getPaymentMode() == ExpensePaymentMode.Reimbursement) {
            expense.setPayeeEmployee(payee);
        } else if (payeeEmployeeId != null) {
            expense.setPayeeEmployee(payee);
        } else {
            expense.setPayeeEmployee(null);
        }
        if (attachments != null) {
            lifecycleContext.replaceAttachments(expense, attachments, current);
        }
    }

    private BigDecimal normalizedAmount(BigDecimal value) {
        BigDecimal normalized = value.setScale(2, java.math.RoundingMode.HALF_UP);
        if (normalized.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "费用金额必须大于 0"));
        }
        return normalized;
    }

    private Expense detail(Employee current, Expense expense) {
        Expense detail = repository.findDetailByCompany_ComIdAndExpenseId(
                        current.getCompany().getComId(), expense.getExpenseId())
                .orElse(expense);
        repository.findDetailPeopleByIdIn(List.of(detail.getExpenseId()));
        return projectionContext.apply(detail, current);
    }
}
