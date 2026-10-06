package com.th4.edge.decoboon.finance.service.refunds.save.support;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.context.ReceiptRefundContext;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 登记客户退款并联动提成负向调整。 */
@Note("客户退款登记")
@Properties(allow = {
        @AllowProperties(model = ReceiptRefund.class,
                properties = {"refundId", "project", "receipt", "customer", "amount", "account", "refundAt", "reason", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class,
                properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.finance.model.Receipt.class,
                properties = {"receiptId", "amount", "receivedAt", "payerName"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.Customer.class,
                properties = {"custId", "name"})
})
@Service("finance/refund/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.refunds.save.Save {

    @User private Employee employee;
    @AutoInject @NotNull private Long receiptId;
    @AutoInject @NotNull private BigDecimal amount;
    @AutoInject private FundAccount account;
    @AutoInject private LocalDate refundAt;
    @AutoInject private String reason;
    @AutoInject private String idempotencyKey;

    private final ReceiptRefundContext refundContext;

    public Save(ReceiptRefundContext refundContext) {
        this.refundContext = refundContext;
    }

    @Override
    public ReceiptRefund call(ReceiptRefund request) {
        return refundContext.create(employee, receiptId, amount, account, refundAt, reason, idempotencyKey);
    }
}
