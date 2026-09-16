package com.th4.edge.decoboon.finance.service.settlements.page.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
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

/** 项目结算分页实现。 */
@Note("项目结算分页")
@Properties(allow = {
        @AllowProperties(model = Settlement.class,
                properties = {"settlementId", "project", "contractAmount", "changeAmount", "deductionAmount", "rewardAmount", "finalAmount", "retentionRate", "retentionAmount", "retentionReleaseAt", "retentionReleasedAt", "receivableAmount", "receivedAmount",
                        "payableAmount", "paidAmount", "materialAmount", "salaryAmount", "projectAmount", "expenseAmount", "totalCost",
                        "profitAmount", "grossMargin", "status", "confirmedBy", "confirmedAt", "closedAt", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/settlement/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.settlements.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String status;
    @AutoInject
    private String keyword;

    private final SettlementRepository settlementRepository;
    private final FinanceAccessContext accessContext;

    public Page(SettlementRepository settlementRepository, FinanceAccessContext accessContext) {
        this.settlementRepository = settlementRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Settlement> call(Settlement request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Settlement> items = settlementRepository.findAll(accessContext.visible(currentEmployee, Settlement.class))
                .stream()
                .filter(item -> projectId == null || projectId.equals(item.getProject().getProjectId()))
                .filter(item -> status == null || status.isBlank() || item.getStatus().name().equalsIgnoreCase(status))
                .filter(item -> kw.isBlank() || item.getProject().getName().toLowerCase().contains(kw))
                .sorted(Comparator.comparing(Settlement::getSettlementId,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
