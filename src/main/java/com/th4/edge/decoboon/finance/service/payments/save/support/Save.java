package com.th4.edge.decoboon.finance.service.payments.save.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
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

/** 付款创建实现，付款与应付核销在同一事务内完成。 */
@Note("业务付款创建")
@Properties(allow = {
        @AllowProperties(model = Payment.class,
                properties = {"paymentId", "project", "payable", "amount", "account", "paidAt", "payeeName",
                        "transactionNo", "voucherUrl", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "name", "type", "amount", "paidAmount", "dueDate", "status", "payeeName", "property" }),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/payment/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.payments.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long payableId;
    @AutoInject
    @NotNull
    private BigDecimal amount;
    @AutoInject
    private FundAccount account;
    @AutoInject
    @NotNull
    private LocalDate paidAt;
    @AutoInject
    private String payeeName;
    @AutoInject
    private String transactionNo;
    @AutoInject
    private String voucherUrl;
    @AutoInject
    private String remark;

    private final PaymentRepository paymentRepository;
    private final PayableRepository payableRepository;
    private final FinanceAccessContext accessContext;

    public Save(PaymentRepository paymentRepository,
                PayableRepository payableRepository,
                FinanceAccessContext accessContext) {
        this.paymentRepository = paymentRepository;
        this.payableRepository = payableRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Payment call(Payment request) {
        Employee current = accessContext.requireEditor(employee);
        Payable payable = payableRepository.findById(payableId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "应付单不存在或无权访问")));
        BigDecimal paid = accessContext.value(amount);
        if (paid.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "付款金额必须大于 0"));
        }
        BigDecimal outstanding = accessContext.outstanding(payable.getAmount(), payable.getPaidAmount());
        if (paid.compareTo(outstanding) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "付款金额不能超过应付余额"));
        }
        Project project = payable.getProject();
        if (project != null) {
            accessContext.resolveProject(current, project.getProjectId());
        }

        request.setProject(project);
        request.setPayable(payable);
        request.setAmount(paid);
        request.setAccount(account == null ? FundAccount.CompanyBank : account);
        request.setPaidAt(paidAt);
        request.setPayeeName(payeeName == null || payeeName.isBlank() ? payable.getPayeeName() : payeeName);
        request.setTransactionNo(transactionNo);
        request.setVoucherUrl(voucherUrl);
        request.setRemark(remark);
        request.setCreatedBy(current);
        accessContext.applyOwnership(request, current, project);
        Payment saved = paymentRepository.save(request);

        payable.setPaidAmount(accessContext.value(payable.getPaidAmount()).add(paid));
        accessContext.refreshPayableStatus(payable);
        payableRepository.save(payable);
        accessContext.recordLedger(current, project, LedgerDirection.Expense,
                payable.getType().getName(), LedgerSource.Payment, saved.getPaymentId(), paid,
                saved.getAccount(), paidAt, saved.getPayeeName(),
                "付款 · " + payable.getName(), remark);
        return saved;
    }
}
