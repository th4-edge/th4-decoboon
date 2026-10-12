package com.th4.edge.decoboon.finance.service.receivables.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
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

/** 应收计划分页实现。 */
@Note("应收计划分页")
@Properties(allow = {
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "project", "property", "customer", "name", "stageName", "stageCode", "triggerType", "type", "amount",
                        "receivedAmount", "dueDate", "status", "invoiceRequired", "invoiceNo", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name", "role"})
})
@Service("finance/receivable/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.receivables.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String type;
    @AutoInject
    private String status;
    @AutoInject
    private String keyword;
    /** 门店维度（DEC-065）：为空表示不限门店（企业范围）或本店（门店范围）。 */
    @AutoInject
    private Long storeId;

    private final ReceivableRepository receivableRepository;
    private final FinanceAccessContext accessContext;

    public Page(ReceivableRepository receivableRepository, FinanceAccessContext accessContext) {
        this.receivableRepository = receivableRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Receivable> call(Receivable request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(currentEmployee, storeId);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Receivable> items = receivableRepository.findAll(accessContext.visible(currentEmployee, Receivable.class))
                .stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .filter(item -> projectId == null || projectId.equals(item.getProject().getProjectId()))
                .filter(item -> type == null || type.isBlank() || item.getType().name().equalsIgnoreCase(type))
                .filter(item -> status == null || status.isBlank() || item.getStatus().name().equalsIgnoreCase(status))
                .filter(item -> kw.isBlank()
                        || item.getProject().getName().toLowerCase().contains(kw)
                        || item.getName().toLowerCase().contains(kw)
                        || (item.getStageName() != null && item.getStageName().toLowerCase().contains(kw)))
                .sorted(Comparator.comparing(Receivable::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
