package com.th4.edge.decoboon.finance.service.receipts.reverse.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.ReceiptReversalReasonType;
import com.th4.edge.decoboon.finance.context.ReceiptReversalContext;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.ReceiptReversal;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** 收款冲正实现。 */
@Note("收款冲正")
@Properties(allow = {
        @AllowProperties(model = ReceiptReversal.class,
                properties = {"reversalId", "receipt", "project", "customer", "receivable", "amount", "reversalAt",
                        "reasonType", "reason", "reversalLedger", "replacementReceipt", "operator", "createTime"}),
        @AllowProperties(model = Receipt.class,
                properties = {"receiptId", "amount", "account", "receivedAt", "payerName", "remark", "status",
                        "reversedAt", "reversedBy", "reversalReason"}),
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "name", "type", "amount", "receivedAmount", "dueDate", "status", "property"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Ledger.class,
                properties = {"ledgerId", "direction", "category", "source", "sourceId", "amount", "account",
                        "occurredAt", "counterparty", "summary", "remark", "originalLedger", "correctionGroupId"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class, properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"})
})
@Service("finance/receipt/reverse")
@Scope(ScopeTypes.PROTOTYPE)
public class Reverse implements com.th4.edge.decoboon.finance.service.receipts.reverse.Reverse {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long receiptId;

    @AutoInject
    @NotNull
    private ReceiptReversalReasonType reasonType;

    @AutoInject
    @NotEmpty
    @Length(max = 500)
    private String reason;

    @AutoInject
    private LocalDate reversalAt;

    @AutoInject
    @Length(max = 128)
    private String idempotencyKey;

    private final ReceiptReversalContext reversalContext;

    public Reverse(ReceiptReversalContext reversalContext) {
        this.reversalContext = reversalContext;
    }

    @Override
    public ReceiptReversal call(ReceiptReversal request) {
        return reversalContext.reverse(employee, receiptId, reasonType, reason, reversalAt, idempotencyKey);
    }
}
