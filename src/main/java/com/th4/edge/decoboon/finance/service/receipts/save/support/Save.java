package com.th4.edge.decoboon.finance.service.receipts.save.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 收款创建实现，收款与应收核销在同一事务内完成。 */
@Note("客户收款创建")
@Properties(allow = {
        @AllowProperties(model = Receipt.class,
                properties = {"receiptId", "amount", "account", "receivedAt", "payerName", "voucherUrl",
                        "remark", "createTime"}),
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "name", "type", "amount", "receivedAmount", "dueDate", "status", "property" }),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status" }),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/receipt/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.receipts.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long projectId;
    @AutoInject
    @NotNull
    private Long receivableId;
    @AutoInject
    @NotNull
    private BigDecimal amount;
    @AutoInject
    private FundAccount account;
    @AutoInject
    @NotNull
    private LocalDate receivedAt;
    @AutoInject
    private String payerName;
    @AutoInject
    private String voucherUrl;
    @AutoInject
    private String remark;

    private final ReceiptRepository receiptRepository;
    private final ReceivableRepository receivableRepository;
    private final FinanceAccessContext accessContext;

    public Save(ReceiptRepository receiptRepository,
                ReceivableRepository receivableRepository,
                FinanceAccessContext accessContext) {
        this.receiptRepository = receiptRepository;
        this.receivableRepository = receivableRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Receipt call(Receipt request) {
        Employee current = accessContext.requireEditor(employee);
        Project project = accessContext.resolveProject(current, projectId);
        Receivable receivable = receivableRepository.findById(receivableId)
                .filter(item -> item.getProject().getProjectId().equals(project.getProjectId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "应收节点不存在")));
        BigDecimal paid = amount == null ? BigDecimal.ZERO : amount;
        if (paid.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "收款金额必须大于 0"));
        }
        BigDecimal outstanding = accessContext.outstanding(receivable.getAmount(), receivable.getReceivedAmount());
        if (paid.compareTo(outstanding) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "收款金额不能超过应收余额"));
        }

        request.setCompany(current.getCompany());
        request.setProject(project);
        request.setReceivable(receivable);
        request.setAmount(paid);
        request.setAccount(account == null ? FundAccount.CompanyBank : account);
        request.setReceivedAt(receivedAt);
        request.setPayerName(payerName == null || payerName.isBlank() ? project.getCustomer().getName() : payerName);
        request.setVoucherUrl(voucherUrl);
        request.setRemark(remark);
        request.setCreatedBy(current);
        accessContext.applyOwnership(request, current, project);
        Receipt saved = receiptRepository.save(request);

        receivable.setReceivedAmount(accessContext.value(receivable.getReceivedAmount()).add(paid));
        accessContext.refreshReceivableStatus(receivable);
        receivableRepository.save(receivable);
        accessContext.recordLedger(current, project, LedgerDirection.Income,
                receivable.getType().getName(), LedgerSource.Receipt, saved.getReceiptId(), paid,
                saved.getAccount(), receivedAt, saved.getPayerName(),
                "收款 · " + receivable.getName(), remark);
        return saved;
    }
}
