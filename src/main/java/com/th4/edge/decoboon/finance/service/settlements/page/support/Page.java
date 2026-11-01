package com.th4.edge.decoboon.finance.service.settlements.page.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.finance.configure.SettlementStatus;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** 项目结算分页实现。 */
@Note("项目结算分页")
@Properties(allow = {
        @AllowProperties(model = Settlement.class,
                properties = {"settlementId", "project", "contractAmount", "changeAmount", "deductionAmount", "rewardAmount", "finalAmount", "retentionRate", "retentionAmount", "retentionReleaseAt", "retentionReleasedAt", "receivableAmount", "receivedAmount",
                        "payableAmount", "paidAmount", "materialAmount", "salaryAmount", "projectAmount", "expenseAmount", "totalCost",
                        "estimatedCost", "actualCost", "manualIncomeAmount", "manualCostAmount", "profitAmount", "costComplete", "costIncompleteCount", "grossMargin", "status", "confirmedBy", "confirmedAt", "closedAt", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status" }),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
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
    private final ProjectCostContext projectCostContext;

    public Page(SettlementRepository settlementRepository, FinanceAccessContext accessContext,
                ProjectCostContext projectCostContext) {
        this.settlementRepository = settlementRepository;
        this.accessContext = accessContext;
        this.projectCostContext = projectCostContext;
    }

    @Override
    @Transactional(readOnly = true)
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
        List<Settlement> pageItems = items.subList(from, to);
        List<Project> projects = pageItems.stream()
                .map(Settlement::getProject)
                .filter(item -> item != null && item.getProjectId() != null)
                .distinct()
                .toList();
        Map<Long, ProjectCostContext.CostSummary> costs = projectCostContext.summarizeAll(projects);
        pageItems.forEach(item -> hydrateCostProjection(item, costs.get(item.getProject().getProjectId())));
        return new PageImpl<>(pageItems, PageRequest.of(page, pageSize), items.size());
    }

    /** 与项目账本共用同一成本完整性投影，禁止列表实体使用 transient 默认 true/0。 */
    private void hydrateCostProjection(Settlement settlement, ProjectCostContext.CostSummary cost) {
        if (cost == null) {
            settlement.setCostComplete(false);
            settlement.setCostIncompleteCount(1);
            settlement.setEstimatedCost(null);
            settlement.setActualCost(settlement.getTotalCost());
            return;
        }
        boolean frozen = settlement.getStatus() != null && settlement.getStatus() != SettlementStatus.Draft;
        boolean complete = frozen ? settlement.getProfitAmount() != null : cost.complete();
        settlement.setCostComplete(complete);
        settlement.setCostIncompleteCount(complete ? 0 : Math.max(1, cost.incompleteItemCount()));
        settlement.setEstimatedCost(cost.estimatedCost());
        settlement.setActualCost(frozen ? settlement.getTotalCost() : cost.actualCost());
    }
}
