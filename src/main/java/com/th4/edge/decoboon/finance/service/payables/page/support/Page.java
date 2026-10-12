package com.th4.edge.decoboon.finance.service.payables.page.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
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

/** 应付分页实现。 */
@Note("应付分页")
@Properties(allow = {
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "project", "materialPlan", "name", "type", "payeeEmployee", "payeePartner", "payeeName",
                        "period", "amount", "paidAmount", "dueDate", "status", "urgent", "sourceNo", "remark",
                        "createdBy", "createTime", "property" }),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status" }),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.MaterialPlan.class,
                properties = {"materialPlanId", "name", "quantity", "unit", "cost", "status", "requiredAt"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/payable/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.payables.page.Page {

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
    private Boolean urgent;

    /** 按供应商筛选（供应链台账联动）。 */
    @AutoInject
    private Long partnerId;
    @AutoInject
    private String keyword;
    /** 门店维度（DEC-065）：为空表示不限门店（企业范围）或本店（门店范围）。 */
    @AutoInject
    private Long storeId;

    private final PayableRepository payableRepository;
    private final FinanceAccessContext accessContext;

    public Page(PayableRepository payableRepository, FinanceAccessContext accessContext) {
        this.payableRepository = payableRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Payable> call(Payable request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(currentEmployee, storeId);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Payable> items = payableRepository.findAll(accessContext.visible(currentEmployee, Payable.class))
                .stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .filter(item -> projectId == null || (item.getProject() != null && projectId.equals(item.getProject().getProjectId())))
                .filter(item -> type == null || type.isBlank() || item.getType().name().equalsIgnoreCase(type))
                .filter(item -> status == null || status.isBlank() || item.getStatus().name().equalsIgnoreCase(status))
                .filter(item -> urgent == null || urgent.equals(item.getUrgent()))
                .filter(item -> partnerId == null
                        || (item.getPayeePartner() != null && partnerId.equals(item.getPayeePartner().getPartnerId())))
                .filter(item -> kw.isBlank()
                        || item.getName().toLowerCase().contains(kw)
                        || (item.getPayeeName() != null && item.getPayeeName().toLowerCase().contains(kw))
                        || (item.getProject() != null && item.getProject().getName().toLowerCase().contains(kw)))
                .sorted(Comparator.comparing(Payable::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
