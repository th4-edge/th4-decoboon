package com.th4.edge.decoboon.finance.service.expenses.info.support;

import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.finance.context.ExpenseAccessContext;
import com.th4.edge.decoboon.finance.context.ExpenseProjectionContext;
import com.th4.edge.decoboon.finance.context.ExpenseVoucherProjectionContext;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.model.ExpenseAttachment;
import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.ExpenseRevision;
import com.th4.edge.decoboon.finance.repository.ExpenseRepository;
import com.th4.edge.decoboon.finance.repository.ExpenseRevisionRepository;
import com.th4.edge.decoboon.finance.model.Payment;
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

import java.util.List;

/** 费用/报销详情实现。 */
@Note("费用报销详情")
@Properties(allow = {
        @AllowProperties(model = Expense.class, properties = {
                "expenseId", "expenseNo", "paymentMode", "amount", "expenseDate", "description", "status",
                "version", "submittedAt", "approvedAt", "approvalNote", "selfApproved", "category", "project",
                "store", "requester", "owner", "payeeEmployee", "approvedBy", "payable", "paymentStatus", "paidAmount",
                "unpaidAmount", "attachments", "allowedActions", "nextAction", "revisions", "createTime", "updateTime",
                "payments", "vouchers"
        }),
        @AllowProperties(model = ExpenseCategory.class,
                properties = {"categoryId", "code", "name", "description", "builtIn", "active", "sort"}),
        @AllowProperties(model = ExpenseAttachment.class, properties = {
                "attachmentId", "documentType", "fileUrl", "fileName", "sha256", "size", "uploadedBy", "uploadedAt"
        }),
        @AllowProperties(model = ExpenseRevision.class, properties = {
                "revisionId", "version", "snapshotJson", "submittedBy", "submittedAt", "returnedBy", "returnedAt", "returnReason"
        }),
        @AllowProperties(model = Payable.class, properties = {
                "payableId", "name", "type", "amount", "paidAmount", "dueDate", "status", "payeeEmployee", "payeeName"
        }),
        @AllowProperties(model = Payment.class, properties = {
                "paymentId", "amount", "account", "paidAt", "payeeName", "transactionNo", "remark"
        }),
        @AllowProperties(model = VoucherRecord.class, properties = {
                "voucherId", "voucherNo", "title", "sourceType", "sourceId", "templateVersion", "totalAmount",
                "amountWords", "partyName", "status", "fileName", "contentType", "size", "sha256", "generatedAt",
                "paymentId", "url", "urlExpiresAt"
        }),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("expense/expenses/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.finance.service.expenses.info.Info {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long expenseId;

    private final ExpenseRepository repository;
    private final ExpenseRevisionRepository revisionRepository;
    private final ExpenseAccessContext accessContext;
    private final ExpenseProjectionContext projectionContext;
    private final ExpenseVoucherProjectionContext voucherProjectionContext;

    public Info(ExpenseRepository repository,
                ExpenseRevisionRepository revisionRepository,
                ExpenseAccessContext accessContext,
                ExpenseProjectionContext projectionContext,
                ExpenseVoucherProjectionContext voucherProjectionContext) {
        this.repository = repository;
        this.revisionRepository = revisionRepository;
        this.accessContext = accessContext;
        this.projectionContext = projectionContext;
        this.voucherProjectionContext = voucherProjectionContext;
    }

    @Override
    @Transactional(readOnly = true)
    public Expense call(Expense request) {
        Employee current = accessContext.requireSubmitter(employee);
        Expense visible = accessContext.requireVisible(current, expenseId);
        Expense detail = repository.findDetailByCompany_ComIdAndExpenseId(
                        current.getCompany().getComId(), visible.getExpenseId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "费用单不存在或无权访问")));
        repository.findDetailPeopleByIdIn(List.of(visible.getExpenseId()));
        detail.setRevisions(revisionRepository.findByCompany_ComIdAndExpense_ExpenseIdOrderByVersionDesc(
                current.getCompany().getComId(), visible.getExpenseId()));
        projectionContext.apply(detail, current);
        return voucherProjectionContext.apply(detail, current);
    }
}
