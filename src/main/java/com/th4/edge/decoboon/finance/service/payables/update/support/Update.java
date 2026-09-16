package com.th4.edge.decoboon.finance.service.payables.update.support;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
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

/** 应付单更新实现。 */
@Note("应付单更新")
@Properties(allow = {
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "project", "materialPlan", "name", "type", "payeeEmployee", "payeeName",
                        "period", "amount", "paidAmount", "dueDate", "status", "urgent", "sourceNo", "remark",
                        "createdBy", "createTime", "property" }),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "siteAddr", "contractAmount", "status", "property", "customer", "manager" }),
        @AllowProperties(model = MaterialPlan.class,
                properties = {"planId", "quantity", "unit", "unitPrice", "cost", "status", "requiredAt", "arrivedAt", "property" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/payable/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.finance.service.payables.update.Update {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long payableId;
    @AutoInject
    private String name;
    @AutoInject
    private BigDecimal amount;
    @AutoInject
    private LocalDate dueDate;
    @AutoInject
    private Boolean urgent;
    @AutoInject
    private String period;
    @AutoInject
    private String remark;

    private final PayableRepository payableRepository;
    private final FinanceAccessContext accessContext;

    public Update(PayableRepository payableRepository, FinanceAccessContext accessContext) {
        this.payableRepository = payableRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Payable call(Payable request) {
        Employee current = accessContext.requireEditor(employee);
        Payable payable = payableRepository.findById(payableId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "应付单不存在或无权访问")));
        if (amount != null && amount.compareTo(accessContext.value(payable.getPaidAmount())) < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "调整后金额不能小于已付金额"));
        }
        if (name != null && !name.isBlank()) payable.setName(name.trim());
        if (amount != null) payable.setAmount(amount);
        if (dueDate != null) payable.setDueDate(dueDate);
        if (urgent != null) payable.setUrgent(urgent);
        if (period != null) payable.setPeriod(period);
        if (remark != null) payable.setRemark(remark);
        accessContext.refreshPayableStatus(payable);
        return payableRepository.save(payable);
    }
}
