package com.th4.edge.decoboon.finance.service.receivables.update.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receivable;
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

/** 应收计划更新实现。 */
@Note("应收计划更新")
@Properties(allow = {
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "project", "property", "customer", "name", "stageName", "type", "amount",
                        "receivedAmount", "dueDate", "status", "invoiceRequired", "invoiceNo", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status" }),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/receivable/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.finance.service.receivables.update.Update {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long receivableId;
    @AutoInject
    private String name;
    @AutoInject
    private String stageName;
    @AutoInject
    private ReceivableType type;
    @AutoInject
    private BigDecimal amount;
    @AutoInject
    private LocalDate dueDate;
    @AutoInject
    private Boolean invoiceRequired;
    @AutoInject
    private String invoiceNo;
    @AutoInject
    private String remark;

    private final ReceivableRepository receivableRepository;
    private final FinanceAccessContext accessContext;

    public Update(ReceivableRepository receivableRepository, FinanceAccessContext accessContext) {
        this.receivableRepository = receivableRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Receivable call(Receivable request) {
        Employee current = accessContext.requireEditor(employee);
        Receivable receivable = receivableRepository.findById(receivableId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "应收计划不存在或无权访问")));
        if (receivable.getReceivedAmount() != null && receivable.getReceivedAmount().signum() > 0
                && amount != null && amount.compareTo(receivable.getReceivedAmount()) < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "调整后金额不能小于已收金额"));
        }
        if (name != null && !name.isBlank()) receivable.setName(name.trim());
        if (stageName != null) receivable.setStageName(stageName);
        if (type != null) receivable.setType(type);
        if (amount != null) receivable.setAmount(amount);
        if (dueDate != null) receivable.setDueDate(dueDate);
        if (invoiceRequired != null) receivable.setInvoiceRequired(invoiceRequired);
        if (invoiceNo != null) receivable.setInvoiceNo(invoiceNo);
        if (remark != null) receivable.setRemark(remark);
        accessContext.refreshReceivableStatus(receivable);
        return receivableRepository.save(receivable);
    }
}
