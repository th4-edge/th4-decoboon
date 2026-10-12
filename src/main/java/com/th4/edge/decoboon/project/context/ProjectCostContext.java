package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.carryover.configure.CarryoverCostStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverCostSnapshot;
import com.th4.edge.decoboon.carryover.repository.CarryoverCostSnapshotRepository;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 项目估算成本、实际成本和成本完整性统一口径。 */
@Component
public class ProjectCostContext {

    private final QuoteLineRepository quoteLineRepository;
    private final ContractLineRepository contractLineRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CarryoverCostSnapshotRepository carryoverCostRepository;
    private final ContractChangeItemRepository changeItemRepository;

    public ProjectCostContext(QuoteLineRepository quoteLineRepository,
                              ContractLineRepository contractLineRepository,
                              PurchaseOrderRepository purchaseOrderRepository) {
        this(quoteLineRepository, contractLineRepository, purchaseOrderRepository, null, null);
    }

    public ProjectCostContext(QuoteLineRepository quoteLineRepository,
                              ContractLineRepository contractLineRepository,
                              PurchaseOrderRepository purchaseOrderRepository,
                              CarryoverCostSnapshotRepository carryoverCostRepository) {
        this(quoteLineRepository, contractLineRepository, purchaseOrderRepository, carryoverCostRepository, null);
    }

    @Autowired
    public ProjectCostContext(QuoteLineRepository quoteLineRepository,
                              ContractLineRepository contractLineRepository,
                              PurchaseOrderRepository purchaseOrderRepository,
                              CarryoverCostSnapshotRepository carryoverCostRepository,
                              ContractChangeItemRepository changeItemRepository) {
        this.quoteLineRepository = quoteLineRepository;
        this.contractLineRepository = contractLineRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.carryoverCostRepository = carryoverCostRepository;
        this.changeItemRepository = changeItemRepository;
    }

    public CostSummary summarize(Project project) {
        if (project == null || project.getProjectId() == null) {
            // 无项目主键（历史/测试夹具）时保持原单一项目语义：直接按报价行回退。
            List<QuoteLine> lines = project == null || project.getQuote() == null
                    ? List.of() : quoteLineRepository.findByQuote(project.getQuote());
            return summarizePostCarryover(project, List.of(), List.of(), lines);
        }
        return summarizeAll(List.of(project)).get(project.getProjectId());
    }

    /**
     * 批量汇总多项目成本（财务汇总等聚合读取用）。
     *
     * <p>原实现逐项目查询「接管快照 / 采购单 / 合同行 / 报价行」，项目数 N 时产生 4N 次查询；
     * 这里改为按项目集合批量取数后内存分组，查询条数与项目数解耦。金额与完整性判定
     * 完全复用同一套规则，{@link #summarize(Project)} 也委托到本方法，保证单项目/批量口径一致。</p>
     */
    public java.util.Map<Long, CostSummary> summarizeAll(java.util.Collection<Project> projects) {
        List<Project> list = projects == null ? List.of()
                : projects.stream().filter(item -> item != null && item.getProjectId() != null).toList();
        if (list.isEmpty()) {
            return java.util.Map.of();
        }
        List<Long> projectIds = list.stream().map(Project::getProjectId).distinct().toList();

        java.util.Map<Long, CarryoverCostSnapshot> snapshots = carryoverCostRepository == null
                ? java.util.Map.of()
                : carryoverCostRepository.findByProject_ProjectIdIn(projectIds).stream()
                        .filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                        .collect(java.util.stream.Collectors.toMap(
                                item -> item.getProject().getProjectId(), item -> item, (left, right) -> left));
        java.util.Map<Long, List<PurchaseOrder>> ordersByProject =
                purchaseOrderRepository.findByProject_ProjectIdInOrderByPurchaseIdDesc(projectIds).stream()
                        .filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                        .collect(java.util.stream.Collectors.groupingBy(item -> item.getProject().getProjectId()));
        java.util.Map<Long, List<ContractLine>> contractLinesByProject =
                contractLineRepository.findByContract_Project_ProjectIdInOrderBySortAsc(projectIds).stream()
                        .filter(item -> item.getContract() != null && item.getContract().getProject() != null)
                        .collect(java.util.stream.Collectors.groupingBy(
                                item -> item.getContract().getProject().getProjectId()));
        java.util.Map<Long, List<ContractChangeItem>> changesByProject = changeItemRepository == null
                ? java.util.Map.of()
                : changeItemRepository.findEffectiveByProjectIds(projectIds, ContractChangeStatus.CustomerConfirmed).stream()
                        .filter(item -> item.getChange() != null && item.getChange().getProject() != null)
                        .collect(java.util.stream.Collectors.groupingBy(
                                item -> item.getChange().getProject().getProjectId()));
        // 未签约项目（无合同行）才回退到报价行，属低频路径：仅对这类项目按报价查询，
        // 保持与原实现完全相同的回退语义；有合同行的项目不再触碰报价行。
        java.util.Map<Long, CostSummary> result = new java.util.LinkedHashMap<>();
        for (Project project : list) {
            CarryoverCostSnapshot snapshot = snapshots.get(project.getProjectId());
            CostSummary post = summarizePostCarryover(project,
                    ordersByProject.getOrDefault(project.getProjectId(), List.of()),
                    contractLinesByProject.getOrDefault(project.getProjectId(), List.of()),
                    contractLinesByProject.containsKey(project.getProjectId()) || project.getQuote() == null
                            ? List.of()
                            : quoteLineRepository.findByQuote(project.getQuote()));
            post = applyChangeItems(post, changesByProject.getOrDefault(project.getProjectId(), List.of()));
            if (snapshot == null) {
                result.put(project.getProjectId(), post);
                continue;
            }
            BigDecimal pre = money(snapshot.getTotalCost());
            int incomplete = post.incompleteItemCount()
                    + (snapshot.getCostStatus() == CarryoverCostStatus.Complete ? 0 : 1);
            result.put(project.getProjectId(), new CostSummary(money(post.estimatedCost().add(pre)),
                    money(post.actualCost().add(pre)), incomplete));
        }
        return result;
    }

    private CostSummary summarizePostCarryover(Project project,
                                              List<PurchaseOrder> purchaseOrders,
                                              List<ContractLine> signedLines,
                                              List<QuoteLine> lines) {
        if (project == null || project.getQuote() == null) {
            BigDecimal fallback = value(project == null ? null : project.getCost());
            return new CostSummary(fallback, fallback, fallback.signum() > 0 ? 0 : 1);
        }
        if (!signedLines.isEmpty()) {
            BigDecimal signedEstimated = BigDecimal.ZERO;
            BigDecimal signedActual = BigDecimal.ZERO;
            int signedIncomplete = 0;
            for (ContractLine line : signedLines) {
                BigDecimal estimatedLine = contractEstimatedCost(line);
                BigDecimal actualLine = contractActualCost(line, estimatedLine);
                signedEstimated = signedEstimated.add(estimatedLine);
                signedActual = signedActual.add(actualLine);
                if (hasIncompletePurchaseCoverage(line.getSourceQuoteLine(), line.getQuantity(), purchaseOrders)
                        || (requiresCost(line.getType())
                        && estimatedLine.signum() <= 0
                        && actualLine.signum() <= 0)
                        || costEqualsSale(line.getCost(), line.getAmount())
                        || catalogCostEqualsSale(line.getSourceQuoteLine())) {
                    signedIncomplete++;
                }
            }
            return new CostSummary(money(signedEstimated), money(signedActual), signedIncomplete);
        }
        if (lines == null || lines.isEmpty()) {
            if (project.getBusinessOrigin() == BusinessOrigin.Carryover) {
                return new CostSummary(BigDecimal.ZERO, BigDecimal.ZERO, 0);
            }
            BigDecimal fallback = value(project.getCost());
            return new CostSummary(money(fallback), money(fallback), 1);
        }
        BigDecimal estimated = BigDecimal.ZERO;
        BigDecimal actual = BigDecimal.ZERO;
        int incomplete = 0;
        for (QuoteLine line : lines) {
            BigDecimal estimatedLine = estimatedCost(line);
            BigDecimal actualLine = actualCost(line, estimatedLine);
            estimated = estimated.add(estimatedLine);
            actual = actual.add(actualLine);
            if (hasIncompletePurchaseCoverage(line, line.getQuantity(), purchaseOrders)
                    || (requiresCost(line.getType())
                    && estimatedLine.signum() <= 0
                    && value(line.getActualCost()).signum() <= 0)
                    || costEqualsSale(line.getCost(), line.getAmount())
                    || catalogCostEqualsSale(line)) {
                incomplete++;
            }
        }
        return new CostSummary(money(estimated), money(actual), incomplete);
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

    /** 实际成本优先采购入库等实际值；尚未发生时按当前采购价回算，采购价变化会同步影响毛利。 */
    public BigDecimal actualCost(QuoteLine line, BigDecimal fallbackEstimated) {
        BigDecimal actual = value(line == null ? null : line.getActualCost());
        if (actual.signum() > 0) {
            return money(actual);
        }
        BigDecimal catalogCost = catalogCost(line);
        return money(catalogCost.signum() > 0 ? catalogCost : fallbackEstimated);
    }

    private boolean hasIncompletePurchaseCoverage(QuoteLine line,
                                                  BigDecimal requiredQuantity,
                                                  List<PurchaseOrder> purchaseOrders) {
        if (line == null || purchaseOrders == null || purchaseOrders.isEmpty()) {
            return false;
        }
        List<PurchaseOrder> linked = purchaseOrders.stream()
                .filter(order -> order != null && sameQuoteLine(line, order.getQuoteLine()))
                .toList();
        if (linked.isEmpty()) {
            return false;
        }
        if (requiredQuantity == null || requiredQuantity.signum() <= 0) {
            return true;
        }
        if (linked.stream().anyMatch(order -> order.getStatus() == PurchaseStatus.PartiallyReceived)) {
            return true;
        }
        BigDecimal receivedQuantity = linked.stream()
                .map(order -> value(order.getReceivedQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return receivedQuantity.compareTo(requiredQuantity) < 0;
    }

    private boolean sameQuoteLine(QuoteLine left, QuoteLine right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.getItemId() != null && right.getItemId() != null) {
            return left.getItemId().equals(right.getItemId());
        }
        return left == right;
    }

    /** 已生效变更成本并入项目预计/实际成本；null 成本视为待维护，不伪造 0。 */
    private CostSummary applyChangeItems(CostSummary base, List<ContractChangeItem> changes) {
        if (changes == null || changes.isEmpty()) {
            return base;
        }
        BigDecimal estimated = base.estimatedCost();
        BigDecimal actual = base.actualCost();
        int incomplete = base.incompleteItemCount();
        for (ContractChangeItem item : changes) {
            if (item.getCostDelta() == null) {
                incomplete++;
                continue;
            }
            estimated = estimated.add(item.getCostDelta());
            actual = actual.add(item.getCostDelta());
        }
        return new CostSummary(money(estimated), money(actual), incomplete);
    }

    private BigDecimal contractEstimatedCost(ContractLine line) {
        if (line == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal frozen = value(line.getCost());
        if (frozen.signum() > 0) {
            return money(frozen);
        }
        // 合同行成本缺失时，报价快照是预计成本的首选回退来源。
        return estimatedCost(line.getSourceQuoteLine());
    }

    private BigDecimal contractActualCost(ContractLine line, BigDecimal fallbackEstimated) {
        if (line == null) {
            return money(fallbackEstimated);
        }
        QuoteLine source = line.getSourceQuoteLine();
        BigDecimal sourceActual = value(source == null ? null : source.getActualCost());
        if (sourceActual.signum() > 0) {
            return money(sourceActual);
        }
        BigDecimal frozen = value(line.getCost());
        if (frozen.signum() > 0) {
            return money(frozen);
        }
        BigDecimal sourceSnapshot = value(source == null ? null : source.getCost());
        if (sourceSnapshot.signum() > 0) {
            return money(sourceSnapshot);
        }
        BigDecimal catalogCost = catalogCost(source);
        return money(catalogCost.signum() > 0 ? catalogCost : fallbackEstimated);
    }

    private BigDecimal catalogCost(QuoteLine line) {
        if (line == null) {
            return BigDecimal.ZERO;
        }
        CatalogItem item = line.getCatalogItem();
        if (item == null || item.getCostPrice() == null || item.getCostPrice().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (line.getPricingMode() != null
                && line.getPricingMode() == com.th4.edge.decoboon.catalog.configure.PricingMethod.FixedTotal) {
            return money(item.getCostPrice());
        }
        return money(item.getCostPrice().multiply(value(line.getQuantity())));
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

    private boolean requiresCost(ItemType type) {
        return type == ItemType.Construction
                || type == ItemType.Material
                || type == ItemType.Custom
                || type == ItemType.Soft
                || type == ItemType.Appliance;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value(value).setScale(2, RoundingMode.HALF_UP);
    }

    /** 成本和完整性汇总。 */
    public record CostSummary(BigDecimal estimatedCost, BigDecimal actualCost, int incompleteItemCount) {
        public boolean complete() {
            return incompleteItemCount == 0;
        }
    }
}
