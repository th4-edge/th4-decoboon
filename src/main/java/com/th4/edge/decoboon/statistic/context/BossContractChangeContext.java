package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.statistic.model.BossContractChangeGroup;
import com.th4.edge.decoboon.statistic.model.BossContractChangeItem;
import com.th4.edge.decoboon.statistic.model.BossContractChangePeriod;
import com.th4.edge.decoboon.statistic.model.BossContractChangeQuery;
import com.th4.edge.decoboon.statistic.model.BossContractChangeReport;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 企业负责人增减项经营聚合。
 *
 * <p>只读聚合既有合同变更事实，不新增状态或金额口径：金额取 ContractChange.amount 与
 * ProjectContract.currentAmount；成本取 ContractChange.costDelta；变更后预计成本使用
 * ProjectCostContext.estimatedCost + 已确认变更成本；成本不完整时利润与毛利率为空
 * （MET-002 / GAP-003）。所有查询在同一 Specification 内应用项目数据范围。</p>
 */
@Note("企业负责人增减项经营聚合")
@Component
public class BossContractChangeContext {

    private static final long APPROVAL_OVERDUE_HOURS = 48L;
    private static final long CONFIRM_OVERDUE_HOURS = 72L;
    private static final Set<EmployeeRole> ALLOWED_ROLES = EnumSet.of(
            EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.TeamLeader,
            EmployeeRole.Finance);

    private final ContractChangeRepository changeRepository;
    private final ContractChangeItemRepository itemRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final ReceivableRepository receivableRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectCostContext costContext;
    private final CatalogCostContext catalogCostContext;

    public BossContractChangeContext(ContractChangeRepository changeRepository,
                                     ContractChangeItemRepository itemRepository,
                                     MaterialPlanRepository materialPlanRepository,
                                     ReceivableRepository receivableRepository,
                                     ProjectAccessContext accessContext,
                                     ProjectCostContext costContext,
                                     CatalogCostContext catalogCostContext) {
        this.changeRepository = changeRepository;
        this.itemRepository = itemRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.receivableRepository = receivableRepository;
        this.accessContext = accessContext;
        this.costContext = costContext;
        this.catalogCostContext = catalogCostContext;
    }

    @Transactional(readOnly = true)
    public BossContractChangeReport report(Employee employee, BossContractChangeQuery query) {
        Employee current = accessContext.requireEmployee(employee);
        if (current.getRole() == null || !ALLOWED_ROLES.contains(current.getRole())) {
            throw new DefinedException(new DefaultErrorCode("403", "增减项经营工作台仅对负责人、店长、团队负责人与财务开放"));
        }
        BossContractChangeQuery condition = query == null ? new BossContractChangeQuery() : query;
        boolean costVisible = catalogCostContext.canViewCost(current);
        LocalDateTime now = LocalDateTime.now();

        List<ContractChange> changes = changeRepository.findAll(
                specification(current, condition), Sort.by(Sort.Direction.DESC, "changeId"));
        List<Long> changeIds = changes.stream().map(ContractChange::getChangeId).toList();
        Map<Long, List<ContractChangeItem>> itemMap = items(changeIds);
        Map<Long, MaterialPlan> planByChangeItem = materialPlans(changes);
        Map<Long, List<Receivable>> receivables = changeReceivables(changes);
        Map<Long, ProjectProjection> projections = projections(changes);

        List<BossContractChangeItem> allItems = changes.stream()
                .map(change -> item(change, itemMap, planByChangeItem, projections, costVisible, now))
                .toList();
        List<BossContractChangeItem> viewItems = allItems.stream()
                .filter(item -> matchesView(item, condition.getView()))
                .sorted(ITEM_ORDER)
                .toList();

        BossContractChangeReport report = new BossContractChangeReport();
        report.setAsOf(now);
        report.setCostVisible(costVisible);
        report.setItems(viewItems);
        report.setGroups(groups(viewItems, projections, receivables, costVisible));
        report.setPeriods(periods(changes, now));
        summary(report, allItems, changes, projections, receivables, costVisible);
        return report;
    }

    private Specification<ContractChange> specification(Employee employee, BossContractChangeQuery query) {
        Specification<ContractChange> spec = accessContext.visibleChild(employee, ContractChange.class, "project");
        if (query.getProjectId() != null) {
            spec = spec.and((root, q, builder) -> builder.equal(root.get("project").get("projectId"), query.getProjectId()));
        }
        if (query.getOwnerId() != null) {
            spec = spec.and((root, q, builder) -> builder.equal(root.get("owner").get("empId"), query.getOwnerId()));
        }
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim().toLowerCase(Locale.ROOT);
        if (!keyword.isBlank()) {
            spec = spec.and((root, q, builder) -> {
                var project = root.join("project", JoinType.LEFT);
                var property = root.join("property", JoinType.LEFT);
                var customer = project.join("customer", JoinType.LEFT);
                String like = "%" + keyword + "%";
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(builder.like(builder.lower(root.get("no")), like));
                predicates.add(builder.like(builder.lower(root.get("reason")), like));
                predicates.add(builder.like(builder.lower(project.get("name")), like));
                predicates.add(builder.like(builder.lower(customer.get("name")), like));
                predicates.add(builder.like(builder.lower(property.get("community")), like));
                return builder.or(predicates.toArray(new Predicate[0]));
            });
        }
        return spec;
    }

    private Map<Long, List<ContractChangeItem>> items(List<Long> changeIds) {
        Map<Long, List<ContractChangeItem>> map = new LinkedHashMap<>();
        if (changeIds.isEmpty()) {
            return map;
        }
        for (ContractChangeItem item : itemRepository.findByChange_ChangeIdInOrderByChangeItemIdAsc(changeIds)) {
            if (item.getChange() == null || item.getChange().getChangeId() == null) {
                continue;
            }
            map.computeIfAbsent(item.getChange().getChangeId(), key -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private Map<Long, MaterialPlan> materialPlans(List<ContractChange> changes) {
        List<Long> projectIds = changes.stream()
                .map(change -> change.getProject() == null ? null : change.getProject().getProjectId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MaterialPlan> map = new LinkedHashMap<>();
        if (projectIds.isEmpty()) {
            return map;
        }
        for (MaterialPlan plan : materialPlanRepository.findByProject_ProjectIdIn(projectIds)) {
            if (plan.getChangeItem() != null && plan.getChangeItem().getChangeItemId() != null) {
                map.putIfAbsent(plan.getChangeItem().getChangeItemId(), plan);
            }
        }
        return map;
    }

    private Map<Long, List<Receivable>> changeReceivables(List<ContractChange> changes) {
        List<Long> projectIds = changes.stream()
                .map(change -> change.getProject() == null ? null : change.getProject().getProjectId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, List<Receivable>> map = new LinkedHashMap<>();
        if (projectIds.isEmpty()) {
            return map;
        }
        for (Receivable receivable : receivableRepository.findByProject_ProjectIdInOrderByDueDateAsc(projectIds)) {
            if (!"合同变更".equals(receivable.getStageName())) {
                continue;
            }
            Long projectId = receivable.getProject() == null ? null : receivable.getProject().getProjectId();
            if (projectId != null) {
                map.computeIfAbsent(projectId, key -> new ArrayList<>()).add(receivable);
            }
        }
        return map;
    }

    private Map<Long, ProjectProjection> projections(List<ContractChange> changes) {
        Map<Long, List<ContractChange>> byProject = new LinkedHashMap<>();
        for (ContractChange change : changes) {
            if (change.getProject() == null || change.getProject().getProjectId() == null) {
                continue;
            }
            byProject.computeIfAbsent(change.getProject().getProjectId(), key -> new ArrayList<>()).add(change);
        }
        Map<Long, ProjectProjection> map = new LinkedHashMap<>();
        for (Map.Entry<Long, List<ContractChange>> entry : byProject.entrySet()) {
            Project project = entry.getValue().get(0).getProject();
            ProjectProjection projection = projection(project, entry.getValue());
            map.put(entry.getKey(), projection);
        }
        return map;
    }

    private ProjectProjection projection(Project project, List<ContractChange> changes) {
        ProjectContract contract = project.getContract();
        BigDecimal current = money(contract == null ? project.getContractAmount() : contract.getCurrentAmount());
        BigDecimal inFlight = BigDecimal.ZERO;
        BigDecimal confirmedCost = BigDecimal.ZERO;
        BigDecimal projectedCostDelta = BigDecimal.ZERO;
        boolean hasConfirmed = false;
        for (ContractChange change : changes) {
            BigDecimal amount = money(change.getAmount());
            if (change.getStatus() == ContractChangeStatus.Pending
                    || change.getStatus() == ContractChangeStatus.Approved) {
                inFlight = inFlight.add(amount);
                projectedCostDelta = projectedCostDelta.add(money(change.getCostDelta()));
            } else if (change.getStatus() == ContractChangeStatus.CustomerConfirmed) {
                hasConfirmed = true;
                confirmedCost = confirmedCost.add(money(change.getCostDelta()));
                projectedCostDelta = projectedCostDelta.add(money(change.getCostDelta()));
            }
        }
        ProjectCostContext.CostSummary summary = costContext.summarize(project);
        boolean complete = summary != null && summary.complete() && project.getQuote() != null;
        // 预计毛利与"全部在途确认后预计合同额"口径一致：预计成本同时纳入在途与已确认变更成本。
        BigDecimal estimatedAfter = summary == null ? BigDecimal.ZERO : summary.estimatedCost().add(projectedCostDelta);
        BigDecimal projectedContract = current.add(inFlight);
        BigDecimal profit = complete ? money(projectedContract.subtract(estimatedAfter)) : null;
        BigDecimal margin = complete && projectedContract.signum() > 0
                ? rate(profit, projectedContract) : null;
        return new ProjectProjection(project, contract, current, money(inFlight), money(projectedContract),
                money(confirmedCost), money(estimatedAfter), profit, margin, complete, hasConfirmed);
    }

    private BossContractChangeItem item(ContractChange change,
                                        Map<Long, List<ContractChangeItem>> itemMap,
                                        Map<Long, MaterialPlan> planByChangeItem,
                                        Map<Long, ProjectProjection> projections,
                                        boolean costVisible,
                                        LocalDateTime now) {
        Project project = change.getProject();
        ProjectContract contract = change.getContract();
        ProjectProjection projection = project == null ? null : projections.get(project.getProjectId());
        List<ContractChangeItem> lines = itemMap.getOrDefault(change.getChangeId(), List.of());

        int construction = 0;
        int material = 0;
        int materialPending = 0;
        int materialOrdered = 0;
        for (ContractChangeItem line : lines) {
            ItemType type = line.getItemType();
            if (type == ItemType.Construction) {
                construction++;
            } else if (type == ItemType.Material) {
                material++;
                MaterialPlan plan = planByChangeItem.get(line.getChangeItemId());
                if (plan == null) {
                    materialPending++;
                } else if (isOrdered(plan.getStatus())) {
                    materialOrdered++;
                } else if (plan.getStatus() != MaterialPlanStatus.Closed) {
                    materialPending++;
                }
            }
        }

        BossContractChangeItem item = new BossContractChangeItem();
        item.setChangeId(change.getChangeId());
        item.setChangeNo(change.getNo());
        ContractChangeStatus status = change.getStatus();
        item.setStatus(statusValue(status));
        item.setStatusName(status == null ? null : status.getName());
        item.setType(change.getType() == null ? null : change.getType().name().toLowerCase(Locale.ROOT));
        item.setTypeName(change.getType() == null ? null : change.getType().getName());
        if (project != null) {
            item.setProjectId(project.getProjectId());
            item.setProjectNo(project.getNo());
            item.setProjectName(project.getName());
            item.setProjectStatus(project.getStatus() == null ? null : project.getStatus().name().toLowerCase(Locale.ROOT));
            item.setCustomerName(customerName(project, contract));
        }
        Property property = change.getProperty();
        if (property != null) {
            item.setCommunity(property.getCommunity());
            item.setBuilding(property.getBuilding());
            item.setRoomNo(property.getRoomNo());
            item.setAddress(property.getAddress());
        }
        if (contract != null) {
            item.setContractId(contract.getContractId());
            item.setContractNo(contract.getNo());
        }
        item.setContractCurrentAmount(projection == null ? null : projection.currentAmount());
        item.setProjectedContractAmount(projection == null ? null : projection.projectedContractAmount());
        item.setAmount(money(change.getAmount()));
        item.setCostDelta(costVisible ? money(change.getCostDelta()) : null);
        item.setCostComplete(costVisible && projection != null ? projection.complete() : null);
        item.setProjectedGrossProfit(costVisible && projection != null ? projection.profit() : null);
        item.setProjectedGrossMargin(costVisible && projection != null ? projection.margin() : null);
        item.setItemCount(lines.size());
        item.setConstructionItemCount(construction);
        item.setMaterialItemCount(material);
        item.setMaterialPendingCount(materialPending);
        item.setMaterialOrderedCount(materialOrdered);
        item.setRequestedById(change.getRequestedBy() == null ? null : change.getRequestedBy().getEmpId());
        item.setRequestedByName(change.getRequestedBy() == null ? null : change.getRequestedBy().getName());
        item.setApprovedById(change.getApprovedBy() == null ? null : change.getApprovedBy().getEmpId());
        item.setApprovedByName(change.getApprovedBy() == null ? null : change.getApprovedBy().getName());
        item.setCreateTime(change.getCreateTime() == null ? null : change.getCreateTime().toLocalDateTime());
        item.setApprovedAt(change.getApprovedAt());
        // DEC-068：报表不再表达“客户确认时间”，该字段承载**生效时间**（历史数据回退旧值）。
        item.setCustomerConfirmedAt(effectiveAt(change));
        item.setEvidenceImageCount(change.getEvidenceImages() == null ? 0 : change.getEvidenceImages().size());
        item.setHasSignature(change.getSignatureUrl() != null && !change.getSignatureUrl().isBlank());
        item.setLineSummary(lineSummary(lines));

        long waitingHours = waitingHours(change, now);
        item.setWaitingHours(waitingHours);
        risk(item, status, materialPending, costVisible && projection != null && !projection.complete());
        return item;
    }

    private void risk(BossContractChangeItem item, ContractChangeStatus status,
                      int materialPending, boolean costIncomplete) {
        if (status == ContractChangeStatus.Pending) {
            if (item.getWaitingHours() != null && item.getWaitingHours() >= APPROVAL_OVERDUE_HOURS) {
                high(item, "待审批超 48 小时", "尽快审批变更单");
            } else {
                low(item, "待审批", "审批变更单");
            }
        } else if (status == ContractChangeStatus.Approved) {
            if (item.getWaitingHours() != null && item.getWaitingHours() >= CONFIRM_OVERDUE_HOURS) {
                high(item, "审批通过后超 72 小时未客户确认", "催客户确认");
            } else {
                low(item, "待客户确认", "登记客户确认");
            }
        } else if (status == ContractChangeStatus.CustomerConfirmed) {
            if (materialPending > 0) {
                medium(item, "有 " + materialPending + " 项主材未下单", "跟进材料下单");
            } else if (costIncomplete) {
                medium(item, "成本未完整", "补齐成本");
            } else {
                low(item, "履约正常", "查看项目财务");
            }
        } else if (status == ContractChangeStatus.Rejected) {
            low(item, "已驳回", "查看处理记录");
        } else {
            low(item, "已取消", "查看处理记录");
        }
    }

    private BossContractChangeReport summary(BossContractChangeReport report,
                                             List<BossContractChangeItem> allItems,
                                             List<ContractChange> changes,
                                             Map<Long, ProjectProjection> projections,
                                             Map<Long, List<Receivable>> receivables,
                                             boolean costVisible) {
        report.setItemCount(allItems.size());
        report.setPendingCount(countStatus(changes, ContractChangeStatus.Pending));
        report.setPendingAmount(money(sumAmount(changes, ContractChangeStatus.Pending)));
        report.setApprovedCount(countStatus(changes, ContractChangeStatus.Approved));
        report.setApprovedAmount(money(sumAmount(changes, ContractChangeStatus.Approved)));
        report.setConfirmedCount(countStatus(changes, ContractChangeStatus.CustomerConfirmed));
        report.setConfirmedNetAmount(money(sumAmount(changes, ContractChangeStatus.CustomerConfirmed)));
        report.setRejectedCount(countStatus(changes, ContractChangeStatus.Rejected));
        report.setCancelledCount(countStatus(changes, ContractChangeStatus.Cancelled));
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        int monthCount = 0;
        BigDecimal monthAmount = BigDecimal.ZERO;
        for (ContractChange change : changes) {
            if (change.getStatus() == ContractChangeStatus.CustomerConfirmed
                    && change.getCustomerConfirmedAt() != null
                    && !change.getCustomerConfirmedAt().isBefore(monthStart)) {
                monthCount++;
                monthAmount = monthAmount.add(money(change.getAmount()));
            }
        }
        report.setThisMonthConfirmedCount(monthCount);
        report.setThisMonthConfirmedAmount(money(monthAmount));

        BigDecimal confirmedCost = BigDecimal.ZERO;
        int costIncomplete = 0;
        for (ContractChange change : changes) {
            if (change.getStatus() != ContractChangeStatus.CustomerConfirmed) {
                continue;
            }
            confirmedCost = confirmedCost.add(money(change.getCostDelta()));
            ProjectProjection projection = change.getProject() == null ? null
                    : projections.get(change.getProject().getProjectId());
            if (projection == null || !projection.complete()) {
                costIncomplete++;
            }
        }

        BigDecimal projectedContract = projections.values().stream()
                .map(ProjectProjection::projectedContractAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean allComplete = !projections.isEmpty()
                && projections.values().stream().allMatch(ProjectProjection::complete);
        BigDecimal projectedProfit = projections.isEmpty() ? BigDecimal.ZERO
                : (allComplete ? projections.values().stream()
                        .map(ProjectProjection::profit)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : null);
        report.setProjectedContractAmount(money(projectedContract));
        report.setConfirmedCostDelta(costVisible ? money(confirmedCost) : null);
        report.setProjectedGrossProfit(costVisible && projectedProfit != null ? money(projectedProfit) : null);
        report.setProjectedGrossMargin(costVisible && projectedProfit != null && projectedContract.signum() > 0
                ? rate(projectedProfit, projectedContract) : null);
        report.setCostIncompleteCount(costVisible ? costIncomplete : null);

        report.setOverdueApprovalCount((int) allItems.stream()
                .filter(item -> item.getWaitingHours() != null
                        && "pending".equals(item.getStatus())
                        && item.getWaitingHours() >= APPROVAL_OVERDUE_HOURS)
                .count());
        report.setOverdueConfirmCount((int) allItems.stream()
                .filter(item -> item.getWaitingHours() != null
                        && "approved".equals(item.getStatus())
                        && item.getWaitingHours() >= CONFIRM_OVERDUE_HOURS)
                .count());
        report.setMaterialPendingCount(allItems.stream()
                .mapToInt(item -> item.getMaterialPendingCount() == null ? 0 : item.getMaterialPendingCount()).sum());
        report.setMaterialOrderedCount(allItems.stream()
                .mapToInt(item -> item.getMaterialOrderedCount() == null ? 0 : item.getMaterialOrderedCount()).sum());

        BigDecimal receivableTotal = BigDecimal.ZERO;
        BigDecimal receivedTotal = BigDecimal.ZERO;
        BigDecimal outstandingTotal = BigDecimal.ZERO;
        BigDecimal overdueTotal = BigDecimal.ZERO;
        int overdueCount = 0;
        boolean receivableGenerated = false;
        LocalDate today = LocalDate.now();
        for (List<Receivable> list : receivables.values()) {
            receivableGenerated = receivableGenerated || !list.isEmpty();
            for (Receivable receivable : list) {
                BigDecimal amount = money(receivable.getAmount());
                BigDecimal received = money(receivable.getReceivedAmount());
                BigDecimal outstanding = amount.subtract(received);
                if (outstanding.signum() < 0) {
                    outstanding = BigDecimal.ZERO;
                }
                receivableTotal = receivableTotal.add(amount);
                receivedTotal = receivedTotal.add(received);
                outstandingTotal = outstandingTotal.add(outstanding);
                if (isOverdue(receivable, today) && outstanding.signum() > 0) {
                    overdueCount++;
                    overdueTotal = overdueTotal.add(outstanding);
                }
            }
        }
        report.setReceivableGenerated(receivableGenerated);
        report.setReceivableChangeTotal(money(receivableTotal));
        report.setReceivableReceivedTotal(money(receivedTotal));
        report.setReceivableOutstandingTotal(money(outstandingTotal));
        report.setReceivableOverdueTotal(money(overdueTotal));
        report.setReceivableOverdueCount(overdueCount);
        return report;
    }

    private List<BossContractChangeGroup> groups(List<BossContractChangeItem> items,
                                                  Map<Long, ProjectProjection> projections,
                                                  Map<Long, List<Receivable>> receivables,
                                                  boolean costVisible) {
        Map<Long, List<BossContractChangeItem>> byProject = new LinkedHashMap<>();
        for (BossContractChangeItem item : items) {
            if (item.getProjectId() == null) {
                continue;
            }
            byProject.computeIfAbsent(item.getProjectId(), key -> new ArrayList<>()).add(item);
        }
        List<BossContractChangeGroup> groups = new ArrayList<>();
        for (Map.Entry<Long, List<BossContractChangeItem>> entry : byProject.entrySet()) {
            List<BossContractChangeItem> list = entry.getValue();
            BossContractChangeItem first = list.get(0);
            ProjectProjection projection = projections.get(entry.getKey());
            BossContractChangeGroup group = new BossContractChangeGroup();
            group.setProjectId(first.getProjectId());
            group.setProjectNo(first.getProjectNo());
            group.setProjectName(first.getProjectName());
            group.setProjectStatus(first.getProjectStatus());
            group.setCustomerName(first.getCustomerName());
            group.setCommunity(first.getCommunity());
            group.setBuilding(first.getBuilding());
            group.setRoomNo(first.getRoomNo());
            group.setAddress(first.getAddress());
            group.setContractId(first.getContractId());
            group.setContractNo(first.getContractNo());
            group.setItems(list);
            group.setTotalCount(list.size());
            group.setPendingCount((int) list.stream().filter(item -> "pending".equals(item.getStatus())).count());
            group.setApprovedCount((int) list.stream().filter(item -> "approved".equals(item.getStatus())).count());
            group.setConfirmedCount((int) list.stream().filter(item -> "customerConfirmed".equals(item.getStatus())).count());
            group.setPendingAmount(money(list.stream()
                    .filter(item -> "pending".equals(item.getStatus()) || "approved".equals(item.getStatus()))
                    .map(BossContractChangeItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
            group.setConfirmedNetAmount(money(list.stream()
                    .filter(item -> "customerConfirmed".equals(item.getStatus()))
                    .map(BossContractChangeItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
            group.setCostDelta(costVisible && projection != null ? projection.confirmedCostDelta() : null);
            group.setCostComplete(costVisible && projection != null ? projection.complete() : null);
            group.setProjectedGrossProfit(costVisible && projection != null ? projection.profit() : null);
            group.setProjectedGrossMargin(costVisible && projection != null ? projection.margin() : null);
            group.setMaterialPendingCount(list.stream()
                    .mapToInt(item -> item.getMaterialPendingCount() == null ? 0 : item.getMaterialPendingCount()).sum());
            group.setMaterialOrderedCount(list.stream()
                    .mapToInt(item -> item.getMaterialOrderedCount() == null ? 0 : item.getMaterialOrderedCount()).sum());
            int risks = (int) list.stream().filter(item -> !"low".equals(item.getRiskLevel())).count();
            group.setRiskCount(risks);
            group.setRiskLevel(list.stream().anyMatch(item -> "high".equals(item.getRiskLevel())) ? "high"
                    : list.stream().anyMatch(item -> "medium".equals(item.getRiskLevel())) ? "medium" : "low");
            group.setRiskText(list.stream().filter(item -> !"low".equals(item.getRiskLevel()))
                    .map(BossContractChangeItem::getRiskText).findFirst().orElse(null));
            if (projection != null) {
                group.setContractCurrentAmount(projection.currentAmount());
                group.setInFlightAmount(projection.inFlightAmount());
                group.setProjectedContractAmount(projection.projectedContractAmount());
            }
            receivables(group, receivables.get(entry.getKey()));
            groups.add(group);
        }
        groups.sort(Comparator
                .comparing((BossContractChangeGroup group) -> riskRank(group.getRiskLevel()))
                .thenComparing(BossContractChangeGroup::getTotalCount, Comparator.reverseOrder())
                .thenComparing(group -> group.getConfirmedNetAmount() == null ? BigDecimal.ZERO : group.getConfirmedNetAmount().abs(),
                        Comparator.reverseOrder()));
        return groups;
    }

    private void receivables(BossContractChangeGroup group, List<Receivable> list) {
        List<Receivable> records = list == null ? List.of() : list;
        group.setReceivableGenerated(!records.isEmpty());
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;
        int overdueCount = 0;
        LocalDate today = LocalDate.now();
        for (Receivable receivable : records) {
            BigDecimal amount = money(receivable.getAmount());
            BigDecimal paid = money(receivable.getReceivedAmount());
            BigDecimal pending = amount.subtract(paid);
            if (pending.signum() < 0) {
                pending = BigDecimal.ZERO;
            }
            total = total.add(amount);
            received = received.add(paid);
            outstanding = outstanding.add(pending);
            if (isOverdue(receivable, today) && pending.signum() > 0) {
                overdueCount++;
                overdue = overdue.add(pending);
            }
        }
        group.setReceivableTotal(money(total));
        group.setReceivableReceivedTotal(money(received));
        group.setReceivableOutstandingTotal(money(outstanding));
        group.setReceivableOverdueTotal(money(overdue));
        group.setReceivableOverdueCount(overdueCount);
    }

    private List<BossContractChangePeriod> periods(List<ContractChange> changes, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        return List.of(
                period("today", "日增", changes, today.atStartOfDay(), now),
                period("yesterday", "昨日", changes, today.minusDays(1).atStartOfDay(), today.atStartOfDay()),
                period("last7Days", "近7日增", changes, today.minusDays(6).atStartOfDay(), now),
                period("month", "月增", changes, today.withDayOfMonth(1).atStartOfDay(), now)
        );
    }

    private BossContractChangePeriod period(String key, String label, List<ContractChange> changes,
                                            LocalDateTime start, LocalDateTime end) {
        BossContractChangePeriod period = new BossContractChangePeriod();
        period.setKey(key);
        period.setLabel(label);
        period.setCreatedCount((int) changes.stream()
                .filter(change -> inRange(createdAt(change), start, end)).count());
        period.setConfirmedCount((int) changes.stream()
                .filter(change -> change.getStatus() == ContractChangeStatus.CustomerConfirmed
                        && inRange(effectiveAt(change), start, end)).count());
        period.setConfirmedAmount(money(changes.stream()
                .filter(change -> change.getStatus() == ContractChangeStatus.CustomerConfirmed
                        && inRange(effectiveAt(change), start, end))
                .map(ContractChange::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        return period;
    }

    private String statusValue(ContractChangeStatus status) {
        if (status == null) return null;
        return switch (status) {
            case Pending -> "pending";
            case Approved -> "approved";
            case CustomerConfirmed -> "customerConfirmed";
            case Rejected -> "rejected";
            case Cancelled -> "cancelled";
        };
    }

    private boolean matchesView(BossContractChangeItem item, String view) {
        if (view == null || view.isBlank() || "all".equalsIgnoreCase(view)) {
            return true;
        }
        return switch (view.toLowerCase(Locale.ROOT)) {
            case "pending" -> "pending".equals(item.getStatus());
            case "approved" -> "approved".equals(item.getStatus());
            case "confirmed" -> "customerConfirmed".equals(item.getStatus());
            case "closed" -> "rejected".equals(item.getStatus()) || "cancelled".equals(item.getStatus());
            case "risk" -> !"low".equals(item.getRiskLevel());
            default -> true;
        };
    }

    private static final Comparator<BossContractChangeItem> ITEM_ORDER = Comparator
            .comparing((BossContractChangeItem item) -> riskRank(item.getRiskLevel()))
            .thenComparing(item -> item.getWaitingHours() == null ? 0L : item.getWaitingHours(), Comparator.reverseOrder())
            .thenComparing(BossContractChangeItem::getChangeId, Comparator.reverseOrder());

    private static int riskRank(String riskLevel) {
        if ("high".equals(riskLevel)) return 0;
        if ("medium".equals(riskLevel)) return 1;
        return 2;
    }

    private void high(BossContractChangeItem item, String text, String action) {
        item.setRiskLevel("high");
        item.setRiskText(text);
        item.setNextAction(action);
    }

    private void medium(BossContractChangeItem item, String text, String action) {
        item.setRiskLevel("medium");
        item.setRiskText(text);
        item.setNextAction(action);
    }

    private void low(BossContractChangeItem item, String text, String action) {
        item.setRiskLevel("low");
        item.setRiskText(text);
        item.setNextAction(action);
    }

    private long waitingHours(ContractChange change, LocalDateTime now) {
        LocalDateTime anchor = switch (change.getStatus()) {
            case Pending -> createdAt(change);
            case Approved -> change.getApprovedAt();
            case CustomerConfirmed -> change.getCustomerConfirmedAt();
            default -> createdAt(change);
        };
        return anchor == null ? 0L : Math.max(0L, Duration.between(anchor, now).toHours());
    }

    private LocalDateTime createdAt(ContractChange change) {
        return change.getCreateTime() == null ? null : change.getCreateTime().toLocalDateTime();
    }

    /**
     * 生效时间（DEC-068）：审批通过即生效，以 {@code approvedAt} 为准；
     * 历史数据回退到旧 {@code customerConfirmedAt}，保证存量统计口径不出现空档。
     */
    private LocalDateTime effectiveAt(ContractChange change) {
        return change.getApprovedAt() != null ? change.getApprovedAt() : change.getCustomerConfirmedAt();
    }

    private String customerName(Project project, ProjectContract contract) {
        if (project.getCustomer() != null) {
            return project.getCustomer().getName();
        }
        if (contract != null && contract.getCustomer() != null) {
            return contract.getCustomer().getName();
        }
        Property property = project.getProperty();
        return property == null || property.getCustomer() == null ? null : property.getCustomer().getName();
    }

    private String lineSummary(List<ContractChangeItem> lines) {
        if (lines.isEmpty()) {
            return null;
        }
        String names = lines.stream().limit(2).map(ContractChangeItem::getName)
                .filter(name -> name != null && !name.isBlank())
                .reduce((left, right) -> left + "、" + right).orElse("变更明细");
        return lines.size() > 2 ? names + " 等" + lines.size() + "项" : names;
    }

    private boolean isOrdered(MaterialPlanStatus status) {
        return status == MaterialPlanStatus.Ordered
                || status == MaterialPlanStatus.PartiallyArrived
                || status == MaterialPlanStatus.Arrived;
    }

    private boolean isOverdue(Receivable receivable, LocalDate today) {
        if (receivable == null) return false;
        ReceivableStatus status = receivable.getStatus();
        if (status == ReceivableStatus.Received || status == ReceivableStatus.Closed) return false;
        if (status == ReceivableStatus.Overdue) return true;
        return receivable.getDueDate() != null && receivable.getDueDate().isBefore(today);
    }

    private boolean inRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    private int countStatus(List<ContractChange> changes, ContractChangeStatus status) {
        return (int) changes.stream().filter(change -> change.getStatus() == status).count();
    }

    private BigDecimal sumAmount(List<ContractChange> changes, ContractChangeStatus status) {
        return changes.stream().filter(change -> change.getStatus() == status)
                .map(ContractChange::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() == 0) return null;
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private record ProjectProjection(Project project,
                                     ProjectContract contract,
                                     BigDecimal currentAmount,
                                     BigDecimal inFlightAmount,
                                     BigDecimal projectedContractAmount,
                                     BigDecimal confirmedCostDelta,
                                     BigDecimal estimatedCostAfter,
                                     BigDecimal profit,
                                     BigDecimal margin,
                                     boolean complete,
                                     boolean hasConfirmed) {
    }
}
