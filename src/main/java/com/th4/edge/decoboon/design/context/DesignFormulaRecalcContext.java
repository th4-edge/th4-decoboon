package com.th4.edge.decoboon.design.context;

import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.design.configure.QuantitySource;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateFormulaContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 空间尺寸变化后的公式分项重算。手工量不覆盖。 */
@Component
public class DesignFormulaRecalcContext {

    private final SpaceRepository spaceRepository;
    private final DesignItemRepository itemRepository;
    private final DesignTemplateFormulaContext formulaContext;
    private final QuoteCalcContext quoteCalcContext;

    public DesignFormulaRecalcContext(SpaceRepository spaceRepository,
                                      DesignItemRepository itemRepository,
                                      DesignTemplateFormulaContext formulaContext,
                                      QuoteCalcContext quoteCalcContext) {
        this.spaceRepository = spaceRepository;
        this.itemRepository = itemRepository;
        this.formulaContext = formulaContext;
        this.quoteCalcContext = quoteCalcContext;
    }

    /**
     * 重算方案：先按区域**实测面积**汇总方案面积，再重算公式分项工程量。
     *
     * <p>{@code DEC-062}：方案面积 = 已录入区域实测面积合计（估算值与未录入不参与），
     * 区域新增、修改、删除都必须经过本方法，避免方案面积与区域事实脱节
     * （方案面积会作为混合报价的面积比例基数）。</p>
     */
    @Transactional
    public void recalculate(Design design) {
        List<Space> spaces = spaceRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId());
        List<DesignItem> items = itemRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId());
        design.setArea(spaces.stream()
                .filter(space -> space.getAreaSource() == AreaSource.Measured)
                .map(Space::getArea)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        Map<Long, Space> spacesById = new HashMap<>();
        for (Space space : spaces) {
            spacesById.put(space.getSpaceId(), space);
        }
        for (DesignItem item : items) {
            if (!formulaDriven(item)) {
                continue;
            }
            String stored = item.getFormula() == null ? "" : item.getFormula().trim();
            boolean pending = stored.startsWith(DesignTemplateFormulaContext.PENDING_MEASURE_MARKER + "|");
            String expression = pending
                    ? stored.substring(DesignTemplateFormulaContext.PENDING_MEASURE_MARKER.length() + 1).trim()
                    : stored;
            if (expression.isBlank()) {
                continue;
            }
            if (!formulaContext.isSupportedFormula(expression)) {
                // GAP-042：历史中文公式（如「(人工单价) × 工程量」）无法按专业变量解析，
                // 降级为不参与重算并标记「公式待修订」，保留既有工程量与金额，不阻断保存。
                item.setFormulaRevisionNeeded(true);
                continue;
            }
            try {
                Space space = item.getSpace() == null ? null : spacesById.get(item.getSpace().getSpaceId());
                BigDecimal quantity = space == null
                        ? formulaContext.evaluateWith(expression, wholeHouseVariables(spaces, expression))
                        : formulaContext.evaluate(toTemplateRoom(space), expression,
                                measuredArea(space), measuredPerimeter(space));
                item.setQuantity(quantity);
                item.setFormula(expression);
                item.setFormulaRevisionNeeded(false);
            } catch (DefinedException exception) {
                if (exception.getErrorCode() != null
                        && "TEMPLATE_SIZE_MISSING".equals(exception.getErrorCode().getCode())) {
                    item.setQuantity(BigDecimal.ZERO);
                    item.setFormula(DesignTemplateFormulaContext.PENDING_MEASURE_MARKER + "|" + expression);
                } else {
                    throw exception;
                }
            }
            quoteCalcContext.calculate(item);
            itemRepository.save(item);
        }
    }

    private boolean formulaDriven(DesignItem item) {
        if (item.getQuantitySource() == QuantitySource.Manual) {
            return false;
        }
        if (item.getQuantitySource() == QuantitySource.Formula) {
            return item.getFormula() != null && !item.getFormula().isBlank();
        }
        return item.getFormula() != null && !item.getFormula().isBlank();
    }

    /**
     * 可用于工程量计算的实测面积。
     *
     * <p>{@code DEC-062}：只有设计师实测录入（{@code AreaSource.Measured}）的值才允许作为工程量基数；
     * 历史按长宽推算的估算值只用于提示复核，返回 {@code null} 让公式按「未就绪」处理。</p>
     */
    private BigDecimal measuredArea(Space space) {
        return space.getAreaSource() == AreaSource.Measured ? space.getArea() : null;
    }

    /** 可用于工程量计算的实测周长，口径同 {@link #measuredArea(Space)}。 */
    private BigDecimal measuredPerimeter(Space space) {
        return space.getAreaSource() == AreaSource.Measured ? space.getPerimeter() : null;
    }

    private DesignTemplateRoom toTemplateRoom(Space space) {
        DesignTemplateRoom room = new DesignTemplateRoom();
        room.setRoomKey(String.valueOf(space.getSpaceId()));
        room.setName(space.getName());
        room.setLength(space.getLength());
        room.setWidth(space.getWidth());
        room.setHeight(space.getHeight());
        // 当前方案区域暂未采集门窗洞和防水上返变量；先按 0 计算，后续字段补齐后再替换。
        room.setDoorWidthTotal(BigDecimal.ZERO);
        room.setDoorAreaTotal(BigDecimal.ZERO);
        room.setWindowAreaTotal(BigDecimal.ZERO);
        room.setWallRise(BigDecimal.ZERO);
        room.setCount(1);
        return room;
    }

    /**
     * 全屋共享分项变量：按区域**实测值**聚合。
     *
     * <p>{@code DEC-062}：任一区域缺少实测面积/周长时，全屋聚合按「未就绪」处理（整项待补），
     * 不允许用部分区域求和的偏小值冒充已完成工程量。</p>
     */
    private Map<String, BigDecimal> wholeHouseVariables(List<Space> spaces, String formula) {
        java.util.Set<String> used = formulaContext.variablesUsed(formula);
        boolean needsArea = used.contains("A") || used.contains("CP");
        boolean needsPerimeter = used.contains("P") || used.contains("WP");
        boolean needsHeight = used.contains("WP");
        for (Space space : spaces) {
            if (needsArea && measuredArea(space) == null) {
                throw new DefinedException(new com.th4.edge.framework.exception.DefaultErrorCode(
                        "TEMPLATE_SIZE_MISSING", "全屋聚合项所需实测面积未录入：" + space.getName()));
            }
            if (needsPerimeter && measuredPerimeter(space) == null) {
                throw new DefinedException(new com.th4.edge.framework.exception.DefaultErrorCode(
                        "TEMPLATE_SIZE_MISSING", "全屋聚合项所需实测周长未录入：" + space.getName()));
            }
            if (needsHeight && space.getHeight() == null) {
                throw new DefinedException(new com.th4.edge.framework.exception.DefaultErrorCode(
                        "TEMPLATE_SIZE_MISSING", "全屋聚合项所需层高未录入：" + space.getName()));
            }
        }
        BigDecimal area = BigDecimal.ZERO;
        BigDecimal perimeter = BigDecimal.ZERO;
        BigDecimal walls = BigDecimal.ZERO;
        for (Space space : spaces) {
            Map<String, BigDecimal> variables = formulaContext.variablesOf(toTemplateRoom(space),
                    measuredArea(space), measuredPerimeter(space));
            area = area.add(variables.getOrDefault("A", BigDecimal.ZERO));
            perimeter = perimeter.add(variables.getOrDefault("P", BigDecimal.ZERO));
            walls = walls.add(variables.getOrDefault("WP", BigDecimal.ZERO));
        }
        Map<String, BigDecimal> whole = new HashMap<>();
        whole.put("A", area);
        whole.put("CP", area);
        whole.put("P", perimeter);
        whole.put("WP", walls);
        whole.put("N", BigDecimal.ONE);
        whole.put(DesignTemplateFormulaContext.ROOM_COUNT_KEY, BigDecimal.valueOf(spaces.size()));
        return whole;
    }
}
