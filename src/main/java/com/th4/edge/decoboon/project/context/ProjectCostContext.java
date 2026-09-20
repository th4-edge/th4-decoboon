package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
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

    public ProjectCostContext(QuoteLineRepository quoteLineRepository,
                              ContractLineRepository contractLineRepository,
                              PurchaseOrderRepository purchaseOrderRepository) {
        this.quoteLineRepository = quoteLineRepository;
        this.contractLineRepository = contractLineRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    public CostSummary summarize(Project project) {
        if (project == null || project.getQuote() == null) {
            BigDecimal fallback = value(project == null ? null : project.getCost());
            return new CostSummary(fallback, fallback, fallback.signum() > 0 ? 0 : 1);
        }
        List<PurchaseOrder> purchaseOrders = project.getProjectId() == null
                ? List.of()
                : purchaseOrderRepository.findByProjectOrderByPurchaseIdDesc(project);
        List<ContractLine> signedLines = contractLineRepository
                .findByContract_Project_ProjectIdOrderBySortAsc(project.getProjectId());
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
                        && actualLine.signum() <= 0)) {
                    signedIncomplete++;
                }
            }
            return new CostSummary(money(signedEstimated), money(signedActual), signedIncomplete);
        }
        List<QuoteLine> lines = quoteLineRepository.findByQuote(project.getQuote());
        if (lines == null || lines.isEmpty()) {
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
                    && value(line.getActualCost()).signum() <= 0)) {
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
