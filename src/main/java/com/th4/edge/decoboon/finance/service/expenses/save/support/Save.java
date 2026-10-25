package com.th4.edge.decoboon.finance.service.expenses.save.support;

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
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 创建费用/报销草稿。 */
@Note("新建费用报销草稿")
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
@Service("expense/expenses/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.expenses.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private ExpensePaymentMode paymentMode;
    @AutoInject
    @NotNull
    private BigDecimal amount;
    @AutoInject
    @NotNull
    private LocalDate expenseDate;
    @AutoInject
    @NotEmpty
    @Length(max = 5000)
    private String description;
    @AutoInject
    @NotNull
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
    @NotEmpty
    @Length(max = 128)
    private String idempotencyKey;
    @AutoInject
    private List<ExpenseAttachment> attachments;

    private final ExpenseRepository repository;
    private final ExpenseAccessContext accessContext;
    private final ExpenseCategoryContext categoryContext;
    private final ExpenseLifecycleContext lifecycleContext;
    private final ExpenseProjectionContext projectionContext;

    public Save(ExpenseRepository repository,
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
        BigDecimal normalizedAmount = normalizedAmount(amount);
        Project project = accessContext.resolveProject(current, projectId);
        Employee owner = accessContext.resolveOwner(current, employeeId);
        Store store = accessContext.resolveStore(current, storeId, project);
        ExpenseCategory category = categoryContext.select(current, categoryId);
        Employee payee = accessContext.resolvePayee(current, owner, payeeEmployeeId, paymentMode);

        Expense candidate = new Expense();
        candidate.setCompany(current.getCompany());
        candidate.setProject(project);
        candidate.setPaymentMode(paymentMode);
        candidate.setAmount(normalizedAmount);
        candidate.setExpenseDate(expenseDate);
        candidate.setDescription(description.trim());
        candidate.setCategory(category);
        candidate.setPayeeEmployee(payee);
        accessContext.applyOwnership(candidate, current, owner, project, store);
        lifecycleContext.replaceAttachments(candidate, attachments == null ? List.of() : attachments, current);

        Expense existing = repository.findByCompany_ComIdAndIdempotencyKey(
                current.getCompany().getComId(), idempotencyKey.trim()).orElse(null);
        if (existing != null) {
            if (!sameIntent(existing, candidate, current)) {
                throw new DefinedException(new DefaultErrorCode("409", "该幂等键已用于其他费用提交"));
            }
            return detail(current, existing);
        }

        candidate.setExpenseNo(expenseNo());
        candidate.setStatus(ExpenseStatus.Draft);
        candidate.setVersion(0);
        candidate.setSelfApproved(Boolean.FALSE);
        candidate.setIdempotencyKey(idempotencyKey.trim());
        Expense saved = repository.save(candidate);
        return detail(current, saved);
    }

    private boolean sameIntent(Expense existing, Expense candidate, Employee requester) {
        return existing.getPaymentMode() == candidate.getPaymentMode()
                && existing.getAmount() != null && candidate.getAmount() != null
                && existing.getAmount().compareTo(candidate.getAmount()) == 0
                && Objects.equals(existing.getExpenseDate(), candidate.getExpenseDate())
                && Objects.equals(existing.getDescription(), candidate.getDescription())
                && sameId(existing.getCategory(), candidate.getCategory(), ExpenseCategory::getCategoryId)
                && sameId(existing.getProject(), candidate.getProject(), Project::getProjectId)
                && sameId(existing.getStore(), candidate.getStore(), Store::getStoreId)
                && sameId(existing.getOwner(), candidate.getOwner(), Employee::getEmpId)
                && sameId(existing.getPayeeEmployee(), candidate.getPayeeEmployee(), Employee::getEmpId)
                && sameId(existing.getRequester(), requester, Employee::getEmpId)
                && attachmentFingerprint(existing.getAttachments()).equals(attachmentFingerprint(candidate.getAttachments()));
    }

    private <T> boolean sameId(T left, T right, java.util.function.Function<T, Long> id) {
        if (left == null || right == null) return left == right;
        return Objects.equals(id.apply(left), id.apply(right));
    }

    private String attachmentFingerprint(List<ExpenseAttachment> attachments) {
        return attachments.stream()
                .map(item -> String.join("|",
                        String.valueOf(item.getDocumentType()),
                        String.valueOf(item.getFileUrl()),
                        String.valueOf(item.getFileName()),
                        String.valueOf(item.getSize())))
                .sorted()
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private BigDecimal normalizedAmount(BigDecimal value) {
        if (value == null) {
            throw new DefinedException(new DefaultErrorCode("400", "费用金额必须大于 0"));
        }
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

    private String expenseNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "BX" + time + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
