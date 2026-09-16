package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.model.Project;
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

    public ProjectCostContext(QuoteLineRepository quoteLineRepository) {
        this.quoteLineRepository = quoteLineRepository;
    }

    public CostSummary summarize(Project project) {
        if (project == null || project.getQuote() == null) {
            BigDecimal fallback = value(project == null ? null : project.getCost());
            return new CostSummary(fallback, fallback, fallback.signum() > 0 ? 0 : 1);
        }
        List<QuoteLine> lines = quoteLineRepository.findByQuote(project.getQuote());
        BigDecimal estimated = BigDecimal.ZERO;
        BigDecimal actual = BigDecimal.ZERO;
        int incomplete = 0;
        for (QuoteLine line : lines) {
            BigDecimal estimatedLine = estimatedCost(line);
            BigDecimal actualLine = actualCost(line, estimatedLine);
            estimated = estimated.add(estimatedLine);
            actual = actual.add(actualLine);
            if (requiresCost(line.getType()) && estimatedLine.signum() <= 0 && value(line.getActualCost()).signum() <= 0) {
                incomplete++;
            }
        }
        return new CostSummary(money(estimated), money(actual), incomplete);
    }

    /** 报价快照成本优先；缺少快照时用当前供应商采购价形成的目录标准成本回算。 */
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
