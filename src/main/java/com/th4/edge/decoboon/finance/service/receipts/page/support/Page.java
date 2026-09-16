package com.th4.edge.decoboon.finance.service.receipts.page.support;

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
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 收款分页实现。 */
@Note("收款分页")
@Properties(allow = {
        @AllowProperties(model = Receipt.class,
                properties = {"receiptId", "project", "receivable", "customer", "amount", "account", "receivedAt",
                        "payerName", "voucherUrl", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "name", "type", "amount", "receivedAmount", "dueDate", "status", "property" }),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status" }),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/receipt/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.receipts.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String keyword;

    private final ReceiptRepository receiptRepository;
    private final FinanceAccessContext accessContext;

    public Page(ReceiptRepository receiptRepository, FinanceAccessContext accessContext) {
        this.receiptRepository = receiptRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Receipt> call(Receipt request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Receipt> items = receiptRepository.findAll(accessContext.visible(currentEmployee, Receipt.class))
                .stream()
                .filter(item -> projectId == null || projectId.equals(item.getProject().getProjectId()))
                .filter(item -> kw.isBlank()
                        || item.getProject().getName().toLowerCase().contains(kw)
                        || (item.getPayerName() != null && item.getPayerName().toLowerCase().contains(kw))
                        || (item.getRemark() != null && item.getRemark().toLowerCase().contains(kw)))
                .sorted(Comparator.comparing(Receipt::getReceivedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
