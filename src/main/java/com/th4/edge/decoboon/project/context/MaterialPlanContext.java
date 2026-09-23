package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 材料计划派生口径。
 *
 * <p>统一负责两件事：①材料标识归一化（供应商匹配用，不使用选材自由文本）；
 * ②需求量口径 {@code quantity = netQuantity + lossQuantity}。实体本身不承载业务逻辑。</p>
 */
@Note("材料计划派生口径")
@Component
public class MaterialPlanContext {

    /**
     * 结构化材料标识：目录项 + 品牌 + 规格 + 型号 + 单位。
     *
     * <p>按 DEC-MP-08，选材正文/JSON 为自由文本，不参与供应商匹配。</p>
     */
    public String materialKey(Long catalogItemId, String brand, String spec, String model, Unit unit) {
        List<String> parts = new ArrayList<>();
        if (catalogItemId != null) {
            parts.add("ci:" + catalogItemId);
        }
        add(parts, "brand", brand);
        add(parts, "spec", spec);
        add(parts, "model", model);
        if (unit != null) {
            parts.add("unit:" + unit.name());
        }
        String key = String.join("|", parts);
        return key.length() > 512 ? key.substring(0, 512) : key;
    }

    /** 刷新材料标识与需求量口径；缺省净用量时按现有计划数量回推，保持存量数据口径不变。 */
    public MaterialPlan applyDerived(MaterialPlan plan) {
        if (plan == null) {
            return null;
        }
        BigDecimal loss = value(plan.getLossQuantity());
        BigDecimal net = plan.getNetQuantity();
        if (net == null) {
            net = value(plan.getQuantity()).subtract(loss);
            if (net.signum() < 0) {
                net = BigDecimal.ZERO;
            }
        }
        plan.setNetQuantity(net);
        plan.setLossQuantity(loss);
        if (plan.getLossRate() == null) {
            plan.setLossRate(BigDecimal.ZERO);
        }
        plan.setQuantity(net.add(loss));
        plan.setMaterialKey(materialKey(
                plan.getCatalogItem() == null ? null : plan.getCatalogItem().getItemId(),
                plan.getSelectedBrand(),
                plan.getSelectedSpec(),
                plan.getSelectedModel(),
                plan.getUnit()));
        return plan;
    }

    /**
     * 按主材分类损耗率计算损耗量并写入净用量/损耗率/需求数量（DEC-MP-09）。
     *
     * <p>调用后应继续执行 {@link #applyDerived(MaterialPlan)} 以刷新材料标识。</p>
     */
    public MaterialPlan applyLossRate(MaterialPlan plan, java.math.BigDecimal lossRate) {
        if (plan == null) {
            return null;
        }
        BigDecimal net = plan.getNetQuantity() != null ? plan.getNetQuantity() : value(plan.getQuantity());
        BigDecimal rate = lossRate == null ? BigDecimal.ZERO : lossRate;
        BigDecimal loss = lossQuantity(net, rate);
        plan.setNetQuantity(net);
        plan.setLossRate(rate);
        plan.setLossQuantity(loss);
        plan.setQuantity(net.add(loss));
        return plan;
    }

    /** 按净用量与损耗率计算损耗量（四舍五入到 2 位）。 */
    public BigDecimal lossQuantity(BigDecimal netQuantity, BigDecimal lossRate) {
        BigDecimal net = value(netQuantity);
        BigDecimal rate = value(lossRate);
        if (net.signum() <= 0 || rate.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return net.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void add(List<String> parts, String label, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(label + ":" + value.trim().toLowerCase());
        }
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
