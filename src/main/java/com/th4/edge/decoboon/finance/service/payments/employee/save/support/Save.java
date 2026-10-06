package com.th4.edge.decoboon.finance.service.payments.employee.save.support;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.payroll.context.EmployeePaymentContext;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 员工工资/提成发放 Provider。 */
@Note("员工工资/提成发放")
@Properties(allow = {
        @AllowProperties(model = Payment.class,
                properties = {"paymentId", "project", "payable", "amount", "account", "paidAt", "payeeName", "payeeEmployee",
                        "batchNo", "transactionNo", "voucherUrl", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "name", "type", "amount", "paidAmount", "status", "sourceType", "sourceId"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/payment/employee/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.payments.employee.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private List<Long> payableIds;
    @AutoInject
    @NotNull
    private BigDecimal amount;
    @AutoInject
    private FundAccount account;
    @AutoInject
    private LocalDate paidAt;
    @AutoInject
    @Length(max = 64)
    private String batchNo;
    @AutoInject
    @Length(max = 128)
    private String idempotencyKey;
    @AutoInject
    private String remark;

    private final EmployeePaymentContext paymentContext;

    public Save(EmployeePaymentContext paymentContext) {
        this.paymentContext = paymentContext;
    }

    @Override
    public Payment call(Payment request) {
        return paymentContext.pay(employee, payableIds, amount, account, paidAt, batchNo, idempotencyKey, remark);
    }
}
