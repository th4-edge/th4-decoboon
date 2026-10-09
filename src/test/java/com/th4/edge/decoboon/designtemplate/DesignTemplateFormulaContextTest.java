package com.th4.edge.decoboon.designtemplate;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateFormulaContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 设计模版专业变量工程量公式：算例、命名空间与单位维度门禁（BR-040 §2/§4）。 */
class DesignTemplateFormulaContextTest {

    private final DesignTemplateFormulaContext context = new DesignTemplateFormulaContext();

    /** 主卧算例：L=3.6 W=4.2 H=2.8 DW=0.9 DA=1.89 WA=2.70。 */
    private DesignTemplateRoom masterBedroom() {
        DesignTemplateRoom room = new DesignTemplateRoom();
        room.setRoomKey("主卧");
        room.setName("主卧");
        room.setLength(new BigDecimal("3.60"));
        room.setWidth(new BigDecimal("4.20"));
        room.setHeight(new BigDecimal("2.80"));
        room.setDoorWidthTotal(new BigDecimal("0.90"));
        room.setDoorAreaTotal(new BigDecimal("1.89"));
        room.setWindowAreaTotal(new BigDecimal("2.70"));
        room.setWallRise(new BigDecimal("0.30"));
        room.setCount(1);
        return room;
    }

    @Test
    void evaluatesProfessionalVariablesForMasterBedroom() {
        DesignTemplateRoom room = masterBedroom();
        assertThat(context.evaluate(room, "=A")).isEqualByComparingTo("15.120");
        assertThat(context.evaluate(room, "=P")).isEqualByComparingTo("15.600");
        assertThat(context.evaluate(room, "=P-DW")).isEqualByComparingTo("14.700");
        assertThat(context.evaluate(room, "=P*H-DA-WA")).isEqualByComparingTo("39.090");
        assertThat(context.evaluate(room, "=A+P*WR")).isEqualByComparingTo("19.800");
    }

    @Test
    void rejectsCellReferenceBecauseTemplatesUseBusinessVariablesOnly() {
        assertThat(context.variablesUsed("=F2*2")).isEmpty();
        assertThatThrownBy(() -> context.evaluate(masterBedroom(), "=F2*2"))
                .hasMessageContaining("禁止单元格引用");
        assertThatThrownBy(() -> context.validate("=F2*2"))
                .hasMessageContaining("禁止单元格引用");
    }

    @Test
    void missingSizeIsNotTreatedAsZero() {
        DesignTemplateRoom room = masterBedroom();
        room.setHeight(null);
        assertThatThrownBy(() -> context.evaluate(room, "=P*H"))
                .hasMessageContaining("尺寸未填");
    }

    @Test
    void unitDimensionMustMatchFormulaVariables() {
        DesignTemplateRoom room = masterBedroom();
        // 面积单位只能用面积类变量
        context.requireDimension(Unit.Area, "=P*H-DA-WA");
        assertThatThrownBy(() -> context.requireDimension(Unit.Area, "=P*2"))
                .hasMessageContaining("维度不一致");
        // 长度单位只能用长度类变量
        context.requireDimension(Unit.Metre, "=P-DW");
        assertThatThrownBy(() -> context.requireDimension(Unit.Metre, "=A"))
                .hasMessageContaining("维度不一致");
        // 计数单位不允许任何业务变量
        assertThatThrownBy(() -> context.requireDimension(Unit.Plate, "=A*0.5"))
                .hasMessageContaining("只能填写固定数量");
    }

    @Test
    void wholeHouseAggregationUsesRegionSummedVariables() {
        Map<String, BigDecimal> wholeHouse = new HashMap<>();
        wholeHouse.put("A", new BigDecimal("85.05"));
        wholeHouse.put("P", new BigDecimal("102.60"));
        assertThat(context.evaluateWith("=SUM(A)*0.5", wholeHouse)).isEqualByComparingTo("42.525");
    }

    @Test
    void countRoomUsesScopeBasedRoomCount() {
        DesignTemplateRoom room = masterBedroom();
        // 区域分项：COUNT(room) 只统计当前区域，返回 1
        assertThat(context.evaluate(room, "=COUNT(room)")).isEqualByComparingTo("1.000");
        // 全屋共享项：COUNT(room) 返回模版区域数（由调用方传入）
        Map<String, BigDecimal> wholeHouse = new HashMap<>();
        wholeHouse.put(DesignTemplateFormulaContext.ROOM_COUNT_KEY, new BigDecimal("8"));
        assertThat(context.evaluateWith("=COUNT(room)*50", wholeHouse)).isEqualByComparingTo("400.000");
        // 只支持 COUNT(room)，不支持 COUNT(A)
        assertThatThrownBy(() -> context.evaluate(room, "=COUNT(A)"))
                .hasMessageContaining("只支持 COUNT(room)");
        // 计数单位允许 COUNT(room)；面积单位不匹配
        context.requireDimension(Unit.Count, "=COUNT(room)");
        assertThatThrownBy(() -> context.requireDimension(Unit.Area, "=COUNT(room)"))
                .hasMessageContaining("维度不一致");
    }

    @Test
    void resultMustBePositiveAndScaleIsThreeDecimals() {
        assertThatThrownBy(() -> context.evaluate(masterBedroom(), "=L-L"))
                .hasMessageContaining("必须大于 0");
        assertThatThrownBy(() -> context.evaluate(masterBedroom(), "=L/0"))
                .hasMessageContaining("除数为 0");
        assertThat(context.evaluate(masterBedroom(), "=L*W").scale()).isEqualTo(3);
    }

    @Test
    void unknownVariableIsRejected() {
        assertThatThrownBy(() -> context.evaluate(masterBedroom(), "=Q*2"))
                .hasMessageContaining("未识别的专业变量");
    }
}
