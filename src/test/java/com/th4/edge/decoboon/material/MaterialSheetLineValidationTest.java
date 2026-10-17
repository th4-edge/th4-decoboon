package com.th4.edge.decoboon.material;

import com.th4.edge.decoboon.material.context.MaterialSheetContext;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.framework.exception.DefinedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 作业单行校验守卫（OBJ-008 / BR-044）。
 *
 * <p>核心不变量：行**只能来源材料计划**（合同外主材必须先走增减项，DEC-072 第 6 项）、
 * 数量必须大于 0、必须有安装位置与单位。</p>
 */
class MaterialSheetLineValidationTest {

    private final MaterialSheetContext context = new MaterialSheetContext(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null);

    private MaterialOrderSheetItem line(Long planId, BigDecimal quantity, String location) {
        MaterialOrderSheetItem item = new MaterialOrderSheetItem();
        if (planId != null) {
            MaterialPlan plan = new MaterialPlan();
            plan.setPlanId(planId);
            item.setMaterialPlan(plan);
        }
        item.setQuantity(quantity);
        item.setUnit(com.th4.edge.decoboon.bim.configure.Unit.Sheet);
        item.setLocationName(location);
        return item;
    }

    @Test
    void validLineShouldPass() {
        assertThatCode(() -> context.validateLine(line(1L, new BigDecimal("12"), "客厅地砖")))
                .doesNotThrowAnyException();
    }

    @Test
    void lineWithoutPlanMustBeRejected() {
        assertThatThrownBy(() -> context.validateLine(line(null, BigDecimal.ONE, "客厅地砖")))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("必须来源材料计划");
    }

    @Test
    void quantityMustBePositive() {
        assertThatThrownBy(() -> context.validateLine(line(1L, BigDecimal.ZERO, "客厅地砖")))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("数量必须大于 0");
        assertThatThrownBy(() -> context.validateLine(line(1L, null, "客厅地砖")))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("数量必须大于 0");
    }

    @Test
    void locationMustBePresent() {
        assertThatThrownBy(() -> context.validateLine(line(1L, BigDecimal.ONE, "  ")))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("安装位置");
    }

    @Test
    void unitMustBePresent() {
        MaterialOrderSheetItem item = line(1L, BigDecimal.ONE, "客厅地砖");
        item.setUnit(null);
        assertThatThrownBy(() -> context.validateLine(item))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("单位");
    }

    @Test
    void nullLineMustBeRejected() {
        assertThatThrownBy(() -> context.validateLine(null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不能为空");
    }
}
