package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.quote.configure.QuoteComponentType;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

/** 设计分项统一计价上下文。 */
@Note("设计报价计算")
@Component
public class QuoteCalcContext {

    /** 根据材料、人工、数量和计价公式计算含税前的分项金额（兼容历史固定总价快照）。 */
    public BigDecimal amount(PricingMethod mode, BigDecimal quantity,
                             BigDecimal materialPrice, BigDecimal manualPrice) {
        return amount(mode, quantity, materialPrice, manualPrice, null);
    }

    /** 含独立固定总价的金额计算：固定总价项目直接取固定总价，不乘工程量。 */
    public BigDecimal amount(PricingMethod mode, BigDecimal quantity,
                             BigDecimal materialPrice, BigDecimal manualPrice,
                             BigDecimal fixedTotalPrice) {
        BigDecimal qty = nonNegative(quantity);
        BigDecimal material = zero(materialPrice);
        BigDecimal manual = zero(manualPrice);
        BigDecimal unit = switch (mode == null ? PricingMethod.MaterialAndManual : mode) {
            case MaterialAndManual -> material.add(manual);
            case MaterialOnly -> material;
            case ManualOnly -> manual;
            case FixedTotal -> BigDecimal.ZERO;
        };
        if (mode == PricingMethod.FixedTotal) {
            return money(fixedTotalPrice != null ? fixedTotalPrice : material.add(manual));
        }
        return money(unit.multiply(qty));
    }

    /** 成本统一按成本单价乘工程量计算。 */
    public BigDecimal cost(BigDecimal quantity, BigDecimal costPrice) {
        return money(nonNegative(quantity).multiply(zero(costPrice)));
    }

    /** 将计算结果写回设计分项，避免前端直接指定金额。 */
    public DesignItem calculate(DesignItem item) {
        if (item == null) {
            throw new DefinedException(new DefaultErrorCode("400", "设计分项不能为空"));
        }
        item.setQuantity(nonNegative(item.getQuantity()));
        item.setAmount(amount(item.getPricingMode(), item.getQuantity(),
                item.getMaterialPrice(), item.getManualPrice(), item.getFixedTotalPrice()));
        item.setCost(cost(item.getQuantity(), item.getCostPrice()));
        return item;
    }

    public java.util.List<QuoteComponent> components(PricingMethod method, BigDecimal quantityValue,
                                                     BigDecimal materialPrice, BigDecimal manualPrice,
                                                     BigDecimal costValue, String formula) {
        return components(method, quantityValue, materialPrice, manualPrice, costValue, formula, null);
    }

    public java.util.List<QuoteComponent> components(PricingMethod method, BigDecimal quantityValue,
                                                     BigDecimal materialPrice, BigDecimal manualPrice,
                                                     BigDecimal costValue, String formula,
                                                     BigDecimal fixedTotalPrice) {
        java.util.List<QuoteComponent> result = new ArrayList<>();
        BigDecimal quantity = nonNegative(quantityValue);
        BigDecimal material = zero(materialPrice);
        BigDecimal manual = zero(manualPrice);
        PricingMethod mode = method == null ? PricingMethod.MaterialAndManual : method;
        if (mode == PricingMethod.FixedTotal) {
            QuoteComponent total = new QuoteComponent();
            total.setType(QuoteComponentType.Other);
            total.setName("固定总价");
            total.setSaleAmount(money(fixedTotalPrice != null ? fixedTotalPrice : material.add(manual)));
            total.setCostAmount(zero(costValue));
            total.setFormula(formula);
            result.add(total);
            return result;
        }
        if (mode != PricingMethod.ManualOnly && material.signum() > 0) {
            QuoteComponent value = new QuoteComponent();
            value.setType(QuoteComponentType.Material);
            value.setName("材料");
            value.setSaleAmount(money(material.multiply(quantity)));
            value.setFormula("材料单价 × 工程量");
            value.setSort(result.size());
            result.add(value);
        }
        if (mode != PricingMethod.MaterialOnly && manual.signum() > 0) {
            QuoteComponent value = new QuoteComponent();
            value.setType(QuoteComponentType.Labor);
            value.setName("人工");
            value.setSaleAmount(money(manual.multiply(quantity)));
            value.setFormula("人工单价 × 工程量");
            value.setSort(result.size());
            result.add(value);
        }
        QuoteComponent cost = new QuoteComponent();
        cost.setType(QuoteComponentType.Other);
        cost.setName("成本");
        cost.setSaleAmount(BigDecimal.ZERO);
        cost.setCostAmount(zero(costValue));
        cost.setSort(result.size());
        result.add(cost);
        return result;
    }

    public BigDecimal unitPrice(PricingMethod mode, BigDecimal quantity,
                                BigDecimal materialPrice, BigDecimal manualPrice) {
        return unitPrice(mode, quantity, materialPrice, manualPrice, null);
    }

    /**
     * 单价（项目库基价）。数量允许为 0（赠送）：0 不参与除法，仍返回项目库基价。
     */
    public BigDecimal unitPrice(PricingMethod mode, BigDecimal quantity,
                                BigDecimal materialPrice, BigDecimal manualPrice,
                                BigDecimal fixedTotalPrice) {
        BigDecimal material = zero(materialPrice);
        BigDecimal manual = zero(manualPrice);
        PricingMethod method = mode == null ? PricingMethod.MaterialAndManual : mode;
        if (method == PricingMethod.FixedTotal) {
            return money(fixedTotalPrice != null ? fixedTotalPrice : material.add(manual));
        }
        return switch (method) {
            case MaterialAndManual -> money(material.add(manual));
            case MaterialOnly -> money(material);
            case ManualOnly -> money(manual);
            case FixedTotal -> BigDecimal.ZERO;
        };
    }

    /** 工程量允许 0（赠送项目），负数一律阻断。 */
    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "工程量不能为负数"));
        }
        return value;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
