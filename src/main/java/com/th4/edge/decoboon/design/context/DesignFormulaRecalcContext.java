package com.th4.edge.decoboon.design.context;

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

    @Transactional
    public void recalculate(Design design) {
        List<Space> spaces = spaceRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId());
        List<DesignItem> items = itemRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId());
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
            try {
                Space space = item.getSpace() == null ? null : spacesById.get(item.getSpace().getSpaceId());
                BigDecimal quantity = space == null
                        ? formulaContext.evaluateWith(expression, wholeHouseVariables(spaces, expression))
                        : formulaContext.evaluate(toTemplateRoom(space), expression);
                item.setQuantity(quantity);
                item.setFormula(expression);
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

    private Map<String, BigDecimal> wholeHouseVariables(List<Space> spaces, String formula) {
        java.util.Set<String> used = formulaContext.variablesUsed(formula);
        boolean needsArea = used.stream().anyMatch(name -> java.util.Set.of("A", "CP", "P", "WP").contains(name));
        boolean needsHeight = used.contains("WP");
        if (needsArea) {
            for (Space space : spaces) {
                if (space.getLength() == null || space.getWidth() == null
                        || (needsHeight && space.getHeight() == null)) {
                    throw new DefinedException(new com.th4.edge.framework.exception.DefaultErrorCode(
                            "TEMPLATE_SIZE_MISSING", "全屋聚合项所需尺寸未填：" + space.getName()));
                }
            }
        }
        BigDecimal area = BigDecimal.ZERO;
        BigDecimal perimeter = BigDecimal.ZERO;
        BigDecimal walls = BigDecimal.ZERO;
        for (Space space : spaces) {
            Map<String, BigDecimal> variables = formulaContext.variablesOf(toTemplateRoom(space));
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
