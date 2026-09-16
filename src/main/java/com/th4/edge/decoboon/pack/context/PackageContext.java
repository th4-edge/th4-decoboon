package com.th4.edge.decoboon.pack.context;

import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 套餐模板成本计算上下文。 */
@Component
public class PackageContext {

    public BigDecimal calculateCost(PackageTemplate packageTemplate) {
        BigDecimal total = BigDecimal.ZERO;
        if (packageTemplate == null || packageTemplate.getItems() == null) {
            return total;
        }
        for (PackageLine line : packageTemplate.getItems()) {
            total = total.add(calculateItemCost(line));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateItemCost(PackageLine line) {
        if (line == null || line.getItem() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal factor = line.getQuantityFactor() == null ? BigDecimal.ONE : line.getQuantityFactor();
        BigDecimal cost = line.getItem().getCostPrice() == null ? BigDecimal.ZERO : line.getItem().getCostPrice();
        return cost.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
