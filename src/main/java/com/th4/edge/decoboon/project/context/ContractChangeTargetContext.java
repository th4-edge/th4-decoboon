package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 增减项目标报价项目快照生成器。
 *
 * <p>新增和替换只能引用当前企业启用 CatalogItem，金额与成本由服务端按企业计价规则计算，
 * 不接受前端名称、单价和成本。</p>
 */
@Note("增减项目标报价项目快照")
@Component
public class ContractChangeTargetContext {

    private final CatalogItemRepository catalogRepository;
    private final QuoteCalcContext quoteCalcContext;

    public ContractChangeTargetContext(CatalogItemRepository catalogRepository,
                                       QuoteCalcContext quoteCalcContext) {
        this.catalogRepository = catalogRepository;
        this.quoteCalcContext = quoteCalcContext;
    }

    public TargetSnapshot resolve(Long companyId, Long catalogItemId, BigDecimal quantity) {
        if (companyId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "企业上下文缺失"));
        }
        if (catalogItemId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择报价项目库项目"));
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "工程量必须大于 0"));
        }
        CatalogItem catalog = catalogRepository.findByItemIdAndCompany_ComId(catalogItemId, companyId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价项目不存在或无权访问")));
        if (!Boolean.TRUE.equals(catalog.getActive())) {
            throw new DefinedException(new DefaultErrorCode("400", "报价项目已停用，不能新增或替换"));
        }
        PricingMethod mode = catalog.getPricingMode() == null
                ? PricingMethod.MaterialAndManual : catalog.getPricingMode();
        BigDecimal unitPrice = quoteCalcContext.unitPrice(mode, quantity,
                catalog.getMaterialPrice(), catalog.getManualPrice(), catalog.getFixedTotalPrice());
        BigDecimal amount = quoteCalcContext.amount(mode, quantity,
                catalog.getMaterialPrice(), catalog.getManualPrice(), catalog.getFixedTotalPrice());
        BigDecimal costAmount = cost(mode, quantity, catalog.getCostPrice());
        boolean costComplete = catalog.getCostPrice() != null;
        return new TargetSnapshot(catalog, quantity, unitPrice, amount, costAmount, costComplete);
    }

    public void apply(ContractChangeItem item, TargetSnapshot target) {
        CatalogItem catalog = target.catalog();
        item.setTargetCatalogItem(catalog);
        item.setTargetName(catalog.getName());
        item.setTargetType(catalog.getType());
        item.setTargetCategory(catalog.getCategory());
        item.setTargetBrand(catalog.getBrand());
        item.setTargetSeries(catalog.getSeries());
        item.setTargetSpec(catalog.getSpec());
        Unit unit = catalog.getUnit();
        item.setTargetUnit(unit);
        item.setTargetPricingMode(catalog.getPricingMode());
        item.setTargetMaterialPrice(catalog.getMaterialPrice());
        item.setTargetManualPrice(catalog.getManualPrice());
        item.setTargetFixedTotalPrice(catalog.getFixedTotalPrice());
        item.setTargetFormula(catalog.getFormula());
        item.setTargetCraft(catalog.getCraft());
        item.setTargetCriterion(catalog.getCriterion());
        item.setTargetQuantity(target.quantity());
        item.setTargetAmount(target.amount());
        item.setTargetCostPrice(catalog.getCostPrice());
        item.setTargetCostAmount(target.costAmount());
        item.setTargetCostComplete(target.costComplete());
    }

    public BigDecimal sourceRemainingCost(ContractLineSnapshot source, BigDecimal remainingQuantity) {
        if (source == null || remainingQuantity == null || remainingQuantity.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (source.totalCost() == null || source.totalQuantity() == null
                || source.totalQuantity().signum() == 0) {
            return null;
        }
        return money(source.totalCost()
                .multiply(remainingQuantity)
                .divide(source.totalQuantity(), 6, RoundingMode.HALF_UP));
    }

    private BigDecimal cost(PricingMethod mode, BigDecimal quantity, BigDecimal costPrice) {
        if (costPrice == null) {
            return null;
        }
        if (mode == PricingMethod.FixedTotal) {
            return money(costPrice);
        }
        return money(quantity.multiply(costPrice));
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    public record TargetSnapshot(CatalogItem catalog, BigDecimal quantity, BigDecimal unitPrice,
                                 BigDecimal amount, BigDecimal costAmount, boolean costComplete) {
    }

    public record ContractLineSnapshot(BigDecimal totalQuantity, BigDecimal totalCost) {
    }
}
