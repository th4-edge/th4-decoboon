package com.th4.edge.decoboon.finance.service.receipts.info.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
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

/** 收款详情实现。 */
@Note("收款详情")
@Properties(allow = {
        @AllowProperties(model = Receipt.class,
                properties = {"receiptId", "project", "receivable", "customer", "amount", "account", "receivedAt",
                        "payerName", "voucherUrl", "remark", "createdBy", "status", "reversedAt", "reversedBy",
                        "reversalReason", "createTime"}),
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "name", "type", "amount", "receivedAmount", "dueDate", "status", "property"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/receipt/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.finance.service.receipts.info.Info {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long receiptId;

    private final ReceiptRepository receiptRepository;
    private final FinanceAccessContext accessContext;

    public Info(ReceiptRepository receiptRepository, FinanceAccessContext accessContext) {
        this.receiptRepository = receiptRepository;
        this.accessContext = accessContext;
    }

    @Override
    public Receipt call(Receipt request) {
        Employee current = accessContext.requireReader(employee);
        Receipt receipt = receiptRepository.findByCompany_ComIdAndReceiptId(
                        current.getCompany().getComId(), receiptId)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "收款记录不存在或无权访问")));
        accessContext.requireReceiptScope(current, receipt);
        return receipt;
    }
}
