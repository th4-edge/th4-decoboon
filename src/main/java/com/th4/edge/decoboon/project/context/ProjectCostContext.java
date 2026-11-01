package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.carryover.configure.CarryoverCostStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverCostSnapshot;
import com.th4.edge.decoboon.carryover.repository.CarryoverCostSnapshotRepository;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.common.context.BusinessOriginFilter;
import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.ManualLedgerContext;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.ExpenseRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.material.configure.SettlementStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.model.MaterialSettlementItem;
import com.th4.edge.decoboon.material.repository.MaterialSettlementItemRepository;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ProjectCostStage;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectCostProjection;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 项目成本三态、来源分类和完整性的唯一计算入口。
 *
 * <p>DEC-092 / BR-058：预计只使用 P1；动态使用 P2/P3 材料与已确认应计；最终只使用 P3
 * 与最终确认应计。P1 永不回退为实际成本，Payable(Material/Supplier/Other) 与 Payment
 * 不直接进入成本，项目 Expense 与手工项目零星成本只选择一次。</p>
 */
@Component
public class ProjectCostContext {

    private final QuoteLineRepository quoteLineRepository;
    private final ContractLineRepository contractLineRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CarryoverCostSnapshotRepository carryoverCostRepository;
    private final ContractChangeItemRepository changeItemRepository;
    private final PurchaseOrderItemRepository purchaseItemRepository;
    private final MaterialSettlementItemRepository settlementItemRepository;
    private final PayableRepository payableRepository;
    private final ExpenseRepository expenseRepository;
    private final ManualLedgerContext manualLedgerContext;

    public ProjectCostContext(QuoteLineRepository quoteLineRepository,
                              ContractLineRepository contractLineRepository,
                              PurchaseOrderRepository purchaseOrderRepository) {
        this(quoteLineRepository, contractLineRepository, purchaseOrderRepository, null, null,
                null, null, null, null, null);
    }

    public ProjectCostContext(QuoteLineRepository quoteLineRepository,
                              ContractLineRepository contractLineRepository,
                              PurchaseOrderRepository purchaseOrderRepository,
                              CarryoverCostSnapshotRepository carryoverCostRepository) {
        this(quoteLineRepository, contractLineRepository, purchaseOrderRepository, carryoverCostRepository, null,
                null, null, null, null, null);
    }

    public ProjectCostContext(QuoteLineRepository quoteLineRepository,
                              ContractLineRepository contractLineRepository,
                              PurchaseOrderRepository purchaseOrderRepository,
                              CarryoverCostSnapshotRepository carryoverCostRepository,
                              ContractChangeItemRepository changeItemRepository) {
        this(quoteLineRepository, contractLineRepository, purchaseOrderRepository, carryoverCostRepository,
                changeItemRepository, null, null, null, null, null);
    }

    @Autowired
    public ProjectCostContext(QuoteLineRepository quoteLineRepository,
                              ContractLineRepository contractLineRepository,
                              PurchaseOrderRepository purchaseOrderRepository,
                              CarryoverCostSnapshotRepository carryoverCostRepository,
                              ContractChangeItemRepository changeItemRepository,
                              PurchaseOrderItemRepository purchaseItemRepository,
                              MaterialSettlementItemRepository settlementItemRepository,
                              PayableRepository payableRepository,
                              ExpenseRepository expenseRepository,
                              ManualLedgerContext manualLedgerContext) {
        this.quoteLineRepository = quoteLineRepository;
        this.contractLineRepository = contractLineRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.carryoverCostRepository = carryoverCostRepository;
        this.changeItemRepository = changeItemRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.settlementItemRepository = settlementItemRepository;
        this.payableRepository = payableRepository;
        this.expenseRepository = expenseRepository;
        this.manualLedgerContext = manualLedgerContext;
    }

    public CostSummary summarize(Project project) {
        if (project == null || project.getProjectId() == null) {
            List<QuoteLine> lines = project == null || project.getQuote() == null
                    ? List.of() : quoteLineRepository.findByQuote(project.getQuote());
            CostBreakdown post = summarizePostCarryover(project, List.of(), List.of(), List.of(),
                    List.of(), lines, List.of(), List.of(), List.of(), ManualLedgerContext.ManualAmounts.ZERO);
            return buildSummary(project, post, null);
        }
        return summarizeAll(List.of(project)).get(project.getProjectId());
    }

    /**
     * 批量汇总多项目成本；所有来源按项目集合一次取数，查询条数与项目数解耦。
     */
    public Map<Long, CostSummary> summarizeAll(Collection<Project> projects) {
        List<Project> list = projects == null ? List.of()
                : projects.stream().filter(item -> item != null && item.getProjectId() != null).toList();
        if (list.isEmpty()) {
            return Map.of();
        }
        List<Long> projectIds = list.stream().map(Project::getProjectId).distinct().toList();

        Map<Long, CarryoverCostSnapshot> snapshots = carryoverCostRepository == null
                ? Map.of()
                : carryoverCostRepository.findByProject_ProjectIdIn(projectIds).stream()
                        .filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                        .collect(Collectors.toMap(
                                item -> item.getProject().getProjectId(), item -> item, (left, right) -> left));
        Map<Long, List<PurchaseOrder>> ordersByProject =
                purchaseOrderRepository.findByProject_ProjectIdInOrderByPurchaseIdDesc(projectIds).stream()
                        .filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                        .collect(Collectors.groupingBy(item -> item.getProject().getProjectId()));
        Map<Long, List<ContractLine>> contractLinesByProject =
                contractLineRepository.findByContract_Project_ProjectIdInOrderBySortAsc(projectIds).stream()
                        .filter(item -> item.getContract() != null && item.getContract().getProject() != null)
                        .collect(Collectors.groupingBy(
                                item -> item.getContract().getProject().getProjectId()));
        Map<Long, List<ContractChangeItem>> changesByProject = changeItemRepository == null
                ? Map.of()
                : changeItemRepository.findEffectiveByProjectIds(projectIds, ContractChangeStatus.CustomerConfirmed).stream()
                        .filter(item -> item.getChange() != null && item.getChange().getProject() != null)
                        .collect(Collectors.groupingBy(
                                item -> item.getChange().getProject().getProjectId()));

        List<PurchaseOrder> orders = ordersByProject.values().stream().flatMap(List::stream).toList();
        Map<Long, List<PurchaseOrderItem>> purchaseItemsByOrder = purchaseItemsByOrder(orders);
        Map<Long, List<MaterialSettlementItem>> settlementsByPurchaseItem = settlementsByPurchaseItem(purchaseItemsByOrder);
        Map<Long, List<Payable>> payablesByProject = payablesByProject(projectIds);
        Map<Long, List<Expense>> expensesByProject = expensesByProject(projectIds);
        Map<Long, ManualLedgerContext.ManualAmounts> manualByProject = manualLedgerContext == null
                ? Map.of() : manualLedgerContext.amountsAll(list);

        Map<Long, CostSummary> result = new LinkedHashMap<>();
        for (Project project : list) {
            Long projectId = project.getProjectId();
            List<PurchaseOrder> projectOrders = ordersByProject.getOrDefault(projectId, List.of());
            List<PurchaseOrderItem> projectItems = projectItems(projectOrders, purchaseItemsByOrder);
            List<MaterialSettlementItem> projectSettlements = projectItems.stream()
                    .filter(item -> item.getPurchaseItemId() != null)
                    .flatMap(item -> settlementsByPurchaseItem.getOrDefault(item.getPurchaseItemId(), List.of()).stream())
                    .toList();
            List<ContractLine> signedLines = contractLinesByProject.getOrDefault(projectId, List.of());
            List<QuoteLine> quoteLines = !signedLines.isEmpty() || project.getQuote() == null
                    ? List.of() : quoteLineRepository.findByQuote(project.getQuote());

            List<ContractChangeItem> effectiveChanges = changesByProject.getOrDefault(projectId, List.of());
            CostBreakdown post = summarizePostCarryover(project, projectOrders, projectItems, projectSettlements,
                    signedLines, quoteLines, effectiveChanges,
                    payablesByProject.getOrDefault(projectId, List.of()),
                    expensesByProject.getOrDefault(projectId, List.of()),
                    manualByProject.getOrDefault(projectId, ManualLedgerContext.ManualAmounts.ZERO));
            post = applyChangeItems(post, effectiveChanges);
            result.put(projectId, buildSummary(project, post, snapshots.get(projectId)));
        }
        return result;
    }

    private CostBreakdown summarizePostCarryover(Project project,
                                                 List<PurchaseOrder> purchaseOrders,
                                                 List<PurchaseOrderItem> purchaseItems,
                                                 List<MaterialSettlementItem> settlementItems,
                                                 List<ContractLine> signedLines,
                                                 List<QuoteLine> quoteLines,
                                                 List<ContractChangeItem> effectiveChanges,
                                                 List<Payable> payables,
                                                 List<Expense> expenses,
                                                 ManualLedgerContext.ManualAmounts manual) {
        List<CostLine> costLines = costLines(signedLines, quoteLines);
        BigDecimal estimated = BigDecimal.ZERO;
        int estimatedIncomplete = 0;
        for (CostLine line : costLines) {
            BigDecimal lineCost = line.contract()
                    ? contractEstimatedCost(line.contractLine())
                    : estimatedCost(line.quoteLine());
            estimated = estimated.add(lineCost);
            if ((requiresCost(line.type()) && lineCost.signum() <= 0)
                    || costEqualsSale(line.cost(), line.amount())
                    || catalogCostEqualsSale(line.quoteLine())) {
                estimatedIncomplete++;
            }
        }
        if (costLines.isEmpty()) {
            BigDecimal fallback = money(value(project == null ? null : project.getCost()));
            estimated = fallback;
            boolean carryover = project != null && project.getBusinessOrigin() == BusinessOrigin.Carryover;
            estimatedIncomplete = carryover ? 0 : 1;
        }

        MaterialFacts material = materialFacts(costLines, effectiveChanges, purchaseOrders, purchaseItems, settlementItems);
        AccrualFacts accrual = accrualFacts(payables, expenses, manual);
        int dynamicIncomplete = material.dynamicIncomplete() + accrual.incomplete();
        int finalIncomplete = material.finalIncomplete() + accrual.incomplete();

        if (requiresLabor(costLines)
                && accrual.workerLaborCost().add(accrual.employeeSalaryCost()).signum() <= 0) {
            dynamicIncomplete++;
            finalIncomplete++;
        }
        if (costLines.isEmpty()
                && (project == null || project.getBusinessOrigin() != BusinessOrigin.Carryover)) {
            dynamicIncomplete++;
            finalIncomplete++;
        }

        return new CostBreakdown(
                money(estimated), estimatedIncomplete,
                money(material.dynamicMaterialCost()), money(material.finalMaterialCost()),
                money(accrual.workerLaborCost()), money(accrual.employeeSalaryCost()),
                money(accrual.employeeCommissionCost()), money(accrual.projectExpenseCost()),
                money(accrual.otherProjectAccruedCost()),
                dynamicIncomplete, finalIncomplete, material.provisional());
    }

    private List<CostLine> costLines(List<ContractLine> signedLines, List<QuoteLine> quoteLines) {
        if (signedLines != null && !signedLines.isEmpty()) {
            return signedLines.stream().filter(Objects::nonNull).map(line -> new CostLine(
                    line.getType(), line.getSourceQuoteLine(), value(line.getQuantity()),
                    value(line.getAmount()), value(line.getCost()), line, true)).toList();
        }
        if (quoteLines == null) {
            return List.of();
        }
        return quoteLines.stream().filter(Objects::nonNull).map(line -> new CostLine(
                line.getType(), line, value(line.getQuantity()), value(line.getAmount()),
                value(line.getCost()), null, false)).toList();
    }

    /**
     * P2/P3 材料成本。P3 取确认结算明细；未结算到场且已定价的部分只形成 P2，
     * 并让最终成本保持不完整。
     */
    private MaterialFacts materialFacts(List<CostLine> costLines,
                                        List<ContractChangeItem> effectiveChanges,
                                        List<PurchaseOrder> purchaseOrders,
                                        List<PurchaseOrderItem> purchaseItems,
                                        List<MaterialSettlementItem> settlementItems) {
        BigDecimal p2 = BigDecimal.ZERO;
        BigDecimal p3 = BigDecimal.ZERO;
        int dynamicIncomplete = 0;
        int finalIncomplete = 0;
        Map<Long, List<PurchaseOrderItem>> itemsByOrder = purchaseItems == null ? Map.of()
                : purchaseItems.stream().filter(item -> item.getPurchaseOrder() != null
                        && item.getPurchaseOrder().getPurchaseId() != null)
                .collect(Collectors.groupingBy(item -> item.getPurchaseOrder().getPurchaseId()));
        Map<Long, List<MaterialSettlementItem>> settlementsByItem = settlementItems == null ? Map.of()
                : settlementItems.stream().filter(item -> item.getPurchaseItem() != null
                        && item.getPurchaseItem().getPurchaseItemId() != null)
                .collect(Collectors.groupingBy(item -> item.getPurchaseItem().getPurchaseItemId()));
        Map<String, BigDecimal> receivedByLine = new LinkedHashMap<>();
        Map<String, List<PurchaseOrder>> ordersByLine = new LinkedHashMap<>();

        for (PurchaseOrder order : purchaseOrders == null ? List.<PurchaseOrder>of() : purchaseOrders) {
            if (order == null || terminalCancelled(order.getStatus())) {
                continue;
            }
            String lineKey = quoteLineKey(order.getQuoteLine());
            if (lineKey != null) {
                ordersByLine.computeIfAbsent(lineKey, key -> new ArrayList<>()).add(order);
            }
            List<PurchaseOrderItem> items = order.getPurchaseId() == null
                    ? List.of() : itemsByOrder.getOrDefault(order.getPurchaseId(), List.of());
            if (items.isEmpty()) {
                receivedByLine.merge(lineKey, value(order.getReceivedQuantity()), BigDecimal::add);
                OrderMaterialFacts facts = orderLevelMaterial(order);
                p2 = p2.add(facts.p2());
                dynamicIncomplete += facts.dynamicIncomplete();
                finalIncomplete += facts.finalIncomplete();
                continue;
            }

            BigDecimal orderReceived = BigDecimal.ZERO;
            for (PurchaseOrderItem item : items) {
                BigDecimal received = value(item.getReceivedQuantity());
                orderReceived = orderReceived.add(received);
                if (received.signum() <= 0) {
                    dynamicIncomplete++;
                    finalIncomplete++;
                    continue;
                }

                SettlementQuantity settled = settled(item, settlementsByItem.getOrDefault(
                        item.getPurchaseItemId(), List.of()));
                BigDecimal settledQuantity = settled.quantity();
                if (settledQuantity.compareTo(received) > 0) {
                    dynamicIncomplete++;
                    finalIncomplete++;
                    settledQuantity = received;
                }
                p3 = p3.add(settled.amount());
                if (settledQuantity.signum() > 0 && settled.amount().signum() <= 0) {
                    dynamicIncomplete++;
                    finalIncomplete++;
                }

                BigDecimal unresolved = received.subtract(settledQuantity);
                if (unresolved.signum() > 0) {
                    BigDecimal unitPrice = purchaseUnitPrice(item);
                    if (priced(item.getPriceState()) && unitPrice.signum() > 0) {
                        p2 = p2.add(money(unitPrice.multiply(unresolved)));
                    } else {
                        dynamicIncomplete++;
                    }
                    finalIncomplete++;
                }
            }
            receivedByLine.merge(lineKey, orderReceived, BigDecimal::add);
        }

        for (CostLine line : costLines) {
            if (!requiresMaterial(line.type())) {
                continue;
            }
            String key = quoteLineKey(line.quoteLine());
            List<PurchaseOrder> linked = key == null ? List.of() : ordersByLine.getOrDefault(key, List.of());
            BigDecimal received = key == null ? BigDecimal.ZERO
                    : receivedByLine.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal effectiveQuantity = effectiveMaterialQuantity(line, effectiveChanges);
            if (effectiveQuantity.signum() <= 0) {
                // 已完全减项时不再要求新的采购；但已经到场的遗留事实必须通过退货/调整闭环。
                if (received.signum() > 0) {
                    dynamicIncomplete++;
                    finalIncomplete++;
                }
                continue;
            }
            if (linked.isEmpty()
                    || received.compareTo(effectiveQuantity) < 0
                    || linked.stream().anyMatch(order -> order.getStatus() == PurchaseStatus.PartiallyReceived)) {
                dynamicIncomplete++;
                finalIncomplete++;
            }
        }

        return new MaterialFacts(money(p2.add(p3)), money(p3),
                dynamicIncomplete, finalIncomplete, p2.signum() > 0);
    }

    private BigDecimal effectiveMaterialQuantity(CostLine line, List<ContractChangeItem> effectiveChanges) {
        if (line.contractLine() == null) {
            return line.quantity();
        }
        return ContractChangeRangeContext.effectiveRemainingQuantityForCost(
                line.contractLine(), effectiveChanges);
    }

    private OrderMaterialFacts orderLevelMaterial(PurchaseOrder order) {
        BigDecimal received = value(order.getReceivedQuantity());
        if (received.signum() <= 0) {
            return new OrderMaterialFacts(BigDecimal.ZERO, 1, 1);
        }
        // 旧数据没有明细时，只能用采购价形成动态 P2；不能把采购价伪装成 P3。
        BigDecimal unitPrice = order.getUnitPrice() == null || order.getUnitPrice().signum() <= 0
                ? divide(order.getAmount(), order.getQuantity())
                : order.getUnitPrice();
        if (priced(order.getPriceState()) && unitPrice.signum() > 0) {
            return new OrderMaterialFacts(money(unitPrice.multiply(received)), 0, 1);
        }
        return new OrderMaterialFacts(BigDecimal.ZERO, 1, 1);
    }

    private SettlementQuantity settled(PurchaseOrderItem item, List<MaterialSettlementItem> source) {
        List<MaterialSettlementItem> confirmed = source.stream()
                .filter(line -> line.getSettlement() == null
                        || line.getSettlement().getStatus() == SettlementStatus.Confirmed)
                .toList();
        if (!confirmed.isEmpty()) {
            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal amount = BigDecimal.ZERO;
            for (MaterialSettlementItem line : confirmed) {
                BigDecimal lineQuantity = value(line.getQuantity());
                quantity = quantity.add(lineQuantity);
                amount = amount.add(line.getAmount() == null || line.getAmount().signum() <= 0
                        ? money(value(line.getSettlementPrice()).multiply(lineQuantity))
                        : money(line.getAmount()));
            }
            return new SettlementQuantity(quantity, money(amount));
        }
        // 兼容已回填 settlementPrice、但历史结算明细缺失的记录；正式路径仍以结算明细为准。
        if (item.getPriceState() == SheetPriceState.Settled
                && item.getSettlementPrice() != null && item.getSettlementPrice().signum() > 0) {
            BigDecimal quantity = value(item.getReceivedQuantity());
            return new SettlementQuantity(quantity,
                    money(item.getSettlementPrice().multiply(quantity)));
        }
        return new SettlementQuantity(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private AccrualFacts accrualFacts(List<Payable> payables,
                                      List<Expense> expenses,
                                      ManualLedgerContext.ManualAmounts manual) {
        BigDecimal worker = BigDecimal.ZERO;
        BigDecimal salary = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        int incomplete = 0;
        for (Payable payable : payables == null ? List.<Payable>of() : payables) {
            if (payable == null || !BusinessOriginFilter.isNormal(payable.getBusinessOrigin())
                    || payable.getProject() == null || Boolean.TRUE.equals(payable.getEstimated())) {
                continue;
            }
            BigDecimal amount = value(payable.getAmount());
            if (amount.signum() < 0) {
                incomplete++;
                continue;
            }
            PayableType type = payable.getType();
            if (type == null) {
                continue;
            }
            switch (type) {
                case Salary -> worker = worker.add(amount);
                case EmployeeSalary -> salary = salary.add(amount);
                case EmployeeCommission -> commission = commission.add(amount);
                case Project -> other = other.add(amount);
                // Material/Supplier/Other/Expense 均不得直接充当项目成本。
                default -> { }
            }
        }

        BigDecimal approvedExpense = BigDecimal.ZERO;
        for (Expense expense : expenses == null ? List.<Expense>of() : expenses) {
            if (expense == null || expense.getProject() == null
                    || (expense.getStatus() != ExpenseStatus.Approved
                    && expense.getStatus() != ExpenseStatus.Closed)) {
                continue;
            }
            BigDecimal amount = value(expense.getAmount());
            if (amount.signum() <= 0) {
                incomplete++;
            } else {
                approvedExpense = approvedExpense.add(amount);
            }
        }
        BigDecimal manualCost = manual == null ? BigDecimal.ZERO : value(manual.cost());
        BigDecimal projectExpense;
        if (approvedExpense.signum() > 0 && manualCost.signum() > 0) {
            // 两套来源同时存在时无法证明属于不同事实，成本保持不完整，只取明细化的 Expense 防止双计。
            projectExpense = approvedExpense;
            incomplete++;
        } else {
            projectExpense = approvedExpense.signum() > 0 ? approvedExpense : manualCost;
        }
        return new AccrualFacts(money(worker), money(salary), money(commission),
                money(projectExpense), money(other), incomplete);
    }

    private Map<Long, List<PurchaseOrderItem>> purchaseItemsByOrder(List<PurchaseOrder> orders) {
        if (purchaseItemRepository == null || orders == null || orders.isEmpty()) {
            return Map.of();
        }
        List<Long> orderIds = orders.stream().filter(Objects::nonNull)
                .map(PurchaseOrder::getPurchaseId).filter(Objects::nonNull).toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return purchaseItemRepository.findByPurchaseOrder_PurchaseIdIn(orderIds).stream()
                .filter(item -> item.getPurchaseOrder() != null
                        && item.getPurchaseOrder().getPurchaseId() != null)
                .collect(Collectors.groupingBy(item -> item.getPurchaseOrder().getPurchaseId()));
    }

    private Map<Long, List<MaterialSettlementItem>> settlementsByPurchaseItem(
            Map<Long, List<PurchaseOrderItem>> itemsByOrder) {
        if (settlementItemRepository == null || itemsByOrder.isEmpty()) {
            return Map.of();
        }
        List<Long> itemIds = itemsByOrder.values().stream().flatMap(List::stream)
                .map(PurchaseOrderItem::getPurchaseItemId).filter(Objects::nonNull).distinct().toList();
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return settlementItemRepository.findByPurchaseItem_PurchaseItemIdIn(itemIds).stream()
                .filter(item -> item.getPurchaseItem() != null
                        && item.getPurchaseItem().getPurchaseItemId() != null)
                .collect(Collectors.groupingBy(item -> item.getPurchaseItem().getPurchaseItemId()));
    }

    private List<PurchaseOrderItem> projectItems(List<PurchaseOrder> orders,
                                                 Map<Long, List<PurchaseOrderItem>> itemsByOrder) {
        return orders.stream()
                .filter(order -> order != null && order.getPurchaseId() != null)
                .map(order -> itemsByOrder.getOrDefault(order.getPurchaseId(), List.of()))
                .flatMap(List::stream).toList();
    }

    private Map<Long, List<Payable>> payablesByProject(List<Long> projectIds) {
        if (payableRepository == null) {
            return Map.of();
        }
        return payableRepository.findByProject_ProjectIdIn(projectIds).stream()
                .filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                .collect(Collectors.groupingBy(item -> item.getProject().getProjectId()));
    }

    private Map<Long, List<Expense>> expensesByProject(List<Long> projectIds) {
        if (expenseRepository == null) {
            return Map.of();
        }
        return expenseRepository.findByProject_ProjectIdInAndStatusIn(
                        projectIds, List.of(ExpenseStatus.Approved, ExpenseStatus.Closed)).stream()
                .filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                .collect(Collectors.groupingBy(item -> item.getProject().getProjectId()));
    }

    /** 未签约前使用报价参考成本；签约后优先使用合同行冻结成本。 */
    public BigDecimal estimatedCost(QuoteLine line) {
        if (line == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal snapshot = value(line.getCost());
        if (snapshot.signum() > 0) {
            return money(snapshot);
        }
        return catalogCost(line);
    }

    private BigDecimal contractEstimatedCost(ContractLine line) {
        if (line == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal frozen = value(line.getCost());
        if (frozen.signum() > 0) {
            return money(frozen);
        }
        return estimatedCost(line.getSourceQuoteLine());
    }

    private BigDecimal catalogCost(QuoteLine line) {
        if (line == null) {
            return BigDecimal.ZERO;
        }
        CatalogItem item = line.getCatalogItem();
        if (item == null || item.getCostPrice() == null || item.getCostPrice().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (line.getPricingMode() == PricingMethod.FixedTotal) {
            return money(item.getCostPrice());
        }
        return money(item.getCostPrice().multiply(value(line.getQuantity())));
    }

    private CostBreakdown applyChangeItems(CostBreakdown base, List<ContractChangeItem> changes) {
        if (changes == null || changes.isEmpty()) {
            return base;
        }
        BigDecimal estimated = base.estimatedCost();
        int incomplete = base.estimatedIncomplete();
        for (ContractChangeItem item : changes) {
            if (item == null || item.getCostDelta() == null) {
                incomplete++;
                continue;
            }
            // costDelta 只进预计。实际/最终成本必须由采购、工资/提成和项目费用等实际事实形成。
            estimated = estimated.add(item.getCostDelta());
        }
        return base.withEstimated(money(estimated), incomplete);
    }

    private CostSummary buildSummary(Project project, CostBreakdown post, CarryoverCostSnapshot snapshot) {
        BigDecimal carryover = snapshot == null ? BigDecimal.ZERO : money(value(snapshot.getTotalCost()));
        boolean carryoverIncomplete = snapshot != null && snapshot.getCostStatus() != CarryoverCostStatus.Complete;
        if (project != null && project.getBusinessOrigin() == BusinessOrigin.Carryover && snapshot == null) {
            carryoverIncomplete = true;
        }
        int carryoverIncrement = carryoverIncomplete ? 1 : 0;

        BigDecimal estimated = money(post.estimatedCost().add(carryover));
        BigDecimal dynamic = money(post.dynamicMaterialCost().add(post.workerLaborCost())
                .add(post.employeeSalaryCost()).add(post.employeeCommissionCost())
                .add(post.projectExpenseCost()).add(post.otherProjectAccruedCost()).add(carryover));
        BigDecimal finalCost = money(post.finalMaterialCost().add(post.workerLaborCost())
                .add(post.employeeSalaryCost()).add(post.employeeCommissionCost())
                .add(post.projectExpenseCost()).add(post.otherProjectAccruedCost()).add(carryover));
        int estimatedIncomplete = post.estimatedIncomplete() + carryoverIncrement;
        int dynamicIncomplete = post.dynamicIncomplete() + carryoverIncrement;
        int finalIncomplete = post.finalIncomplete() + carryoverIncrement;
        boolean estimatedComplete = estimatedIncomplete == 0;
        boolean dynamicComplete = dynamicIncomplete == 0;
        boolean finalComplete = finalIncomplete == 0;

        Map<String, Long> sourceVersions = new LinkedHashMap<>();
        sourceVersions.put(project == null || project.getProjectId() == null
                ? "costProjection" : "project:" + project.getProjectId(), 1L);
        ProjectCostProjection projection = ProjectCostProjection.builder(ProjectCostStage.Dynamic)
                .estimated(null, estimated, estimatedComplete, null, null)
                .dynamic(null, dynamic, dynamicComplete, post.provisional(), null, null)
                .finalized(null, finalCost, finalComplete, null, null)
                .breakdown(post.dynamicMaterialCost(), post.workerLaborCost(), post.employeeSalaryCost(),
                        post.employeeCommissionCost(), post.projectExpenseCost(),
                        post.otherProjectAccruedCost(), carryover)
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, LocalDateTime.now(), sourceVersions)
                .build();

        return new CostSummary(estimated, dynamic, dynamicIncomplete,
                estimatedComplete, estimatedIncomplete,
                finalCost, finalComplete, finalIncomplete, post.provisional(),
                post.dynamicMaterialCost(), post.workerLaborCost(), post.employeeSalaryCost(),
                post.employeeCommissionCost(), post.projectExpenseCost(),
                post.otherProjectAccruedCost(), carryover, projection);
    }

    private boolean requiresCost(ItemType type) {
        return type == ItemType.Construction
                || type == ItemType.Material
                || type == ItemType.Custom
                || type == ItemType.Soft
                || type == ItemType.Appliance;
    }

    private boolean requiresMaterial(ItemType type) {
        return type == ItemType.Material
                || type == ItemType.Custom
                || type == ItemType.Soft
                || type == ItemType.Appliance;
    }

    private boolean requiresLabor(List<CostLine> lines) {
        return lines.stream().anyMatch(line -> line.type() == ItemType.Construction);
    }

    private boolean priced(SheetPriceState state) {
        return state == null || state == SheetPriceState.Priced
                || state == SheetPriceState.Settled || state == SheetPriceState.Mixed;
    }

    private boolean terminalCancelled(PurchaseStatus status) {
        return status == PurchaseStatus.Cancelled || status == PurchaseStatus.Rejected;
    }

    private BigDecimal purchaseUnitPrice(PurchaseOrderItem item) {
        if (item.getUnitPrice() != null && item.getUnitPrice().signum() > 0) {
            return item.getUnitPrice();
        }
        return divide(item.getAmount(), item.getQuantity());
    }

    private BigDecimal divide(BigDecimal amount, BigDecimal quantity) {
        if (amount == null || quantity == null || quantity.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(quantity, 2, RoundingMode.HALF_UP);
    }

    private String quoteLineKey(QuoteLine line) {
        if (line == null) {
            return null;
        }
        return line.getItemId() == null
                ? "object:" + System.identityHashCode(line) : "id:" + line.getItemId();
    }

    /** 成本价与销售价相同视为成本未维护，不得据此计算毛利。 */
    private boolean costEqualsSale(BigDecimal cost, BigDecimal sale) {
        BigDecimal costValue = value(cost);
        BigDecimal saleValue = value(sale);
        return costValue.signum() > 0 && saleValue.signum() > 0 && costValue.compareTo(saleValue) == 0;
    }

    /** 目录标准成本等于材料+人工售价时，视为成本未维护。 */
    private boolean catalogCostEqualsSale(QuoteLine line) {
        if (line == null || line.getCatalogItem() == null) {
            return false;
        }
        CatalogItem item = line.getCatalogItem();
        if (!requiresCost(item.getType())) {
            return false;
        }
        BigDecimal sale = value(item.getMaterialPrice()).add(value(item.getManualPrice()));
        BigDecimal cost = value(item.getCostPrice());
        return sale.signum() > 0 && cost.signum() > 0 && cost.compareTo(sale) == 0;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value(value).setScale(2, RoundingMode.HALF_UP);
    }

    private record CostLine(ItemType type, QuoteLine quoteLine, BigDecimal quantity,
                            BigDecimal amount, BigDecimal cost, ContractLine contractLine, boolean contract) {
    }

    private record MaterialFacts(BigDecimal dynamicMaterialCost, BigDecimal finalMaterialCost,
                                 int dynamicIncomplete, int finalIncomplete, boolean provisional) {
    }

    private record OrderMaterialFacts(BigDecimal p2, int dynamicIncomplete, int finalIncomplete) {
    }

    private record SettlementQuantity(BigDecimal quantity, BigDecimal amount) {
    }

    private record AccrualFacts(BigDecimal workerLaborCost, BigDecimal employeeSalaryCost,
                                BigDecimal employeeCommissionCost, BigDecimal projectExpenseCost,
                                BigDecimal otherProjectAccruedCost, int incomplete) {
    }

    private record CostBreakdown(BigDecimal estimatedCost, int estimatedIncomplete,
                                 BigDecimal dynamicMaterialCost, BigDecimal finalMaterialCost,
                                 BigDecimal workerLaborCost, BigDecimal employeeSalaryCost,
                                 BigDecimal employeeCommissionCost, BigDecimal projectExpenseCost,
                                 BigDecimal otherProjectAccruedCost,
                                 int dynamicIncomplete, int finalIncomplete, boolean provisional) {
        private CostBreakdown withEstimated(BigDecimal value, int incomplete) {
            return new CostBreakdown(value, incomplete, dynamicMaterialCost, finalMaterialCost,
                    workerLaborCost, employeeSalaryCost, employeeCommissionCost, projectExpenseCost,
                    otherProjectAccruedCost, dynamicIncomplete, finalIncomplete, provisional);
        }
    }

    /**
     * 兼容既有读取面的成本汇总。actualCost 是 Dynamic 成本别名，不是 P1；
     * finalCost 仅在 finalCostComplete=true 时可作为最终成本事实继续被 D3 门禁消费。
     */
    public record CostSummary(
            BigDecimal estimatedCost,
            BigDecimal actualCost,
            int incompleteItemCount,
            boolean estimatedCostComplete,
            int estimatedIncompleteItemCount,
            BigDecimal finalCost,
            boolean finalCostComplete,
            int finalIncompleteItemCount,
            boolean dynamicCostProvisional,
            BigDecimal materialCost,
            BigDecimal workerLaborCost,
            BigDecimal employeeSalaryCost,
            BigDecimal employeeCommissionCost,
            BigDecimal projectExpenseCost,
            BigDecimal otherProjectAccruedCost,
            BigDecimal carryoverCost,
            ProjectCostProjection projection) {

        public CostSummary(BigDecimal estimatedCost, BigDecimal actualCost, int incompleteItemCount) {
            this(estimatedCost, actualCost, incompleteItemCount,
                    incompleteItemCount == 0, incompleteItemCount,
                    actualCost, incompleteItemCount == 0, incompleteItemCount, false,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), null);
        }

        /** 测试和旧读取面的分类兼容构造；生产路径始终由唯一投影生成。 */
        public CostSummary(BigDecimal estimatedCost, BigDecimal actualCost, int incompleteItemCount,
                           BigDecimal materialCost, BigDecimal workerLaborCost,
                           BigDecimal employeeSalaryCost, BigDecimal employeeCommissionCost,
                           BigDecimal projectExpenseCost, BigDecimal otherProjectAccruedCost,
                           BigDecimal carryoverCost) {
            this(estimatedCost, actualCost, incompleteItemCount,
                    incompleteItemCount == 0, incompleteItemCount,
                    actualCost, incompleteItemCount == 0, incompleteItemCount, false,
                    materialCost, workerLaborCost, employeeSalaryCost, employeeCommissionCost,
                    projectExpenseCost, otherProjectAccruedCost, carryoverCost, null);
        }

        public boolean complete() {
            return incompleteItemCount == 0;
        }

        public BigDecimal dynamicCost() {
            return actualCost;
        }

        public boolean dynamicCostComplete() {
            return complete();
        }
    }
}
