package com.th4.edge.decoboon.finance.service.refunds.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.decoboon.finance.repository.ReceiptRefundRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Note("客户退款分页")
@Properties(allow = {
        @AllowProperties(model = ReceiptRefund.class,
                properties = {"refundId", "project", "receipt", "customer", "amount", "account", "refundAt", "reason", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = Receipt.class, properties = {"receiptId", "amount", "receivedAt", "payerName"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"})
})
@Service("finance/refund/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.refunds.page.Page {

    @User private Employee employee;
    @AutoInject private Long projectId;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final ReceiptRefundRepository repository;
    private final FinanceAccessContext accessContext;

    public Page(ReceiptRefundRepository repository, FinanceAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<ReceiptRefund> call(ReceiptRefund request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        var items = repository.findAll(accessContext.visible(currentEmployee, ReceiptRefund.class)).stream()
                .filter(item -> projectId == null || (item.getProject() != null
                        && projectId.equals(item.getProject().getProjectId())))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
