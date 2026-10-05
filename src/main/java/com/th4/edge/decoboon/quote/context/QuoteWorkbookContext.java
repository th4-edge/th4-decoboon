package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Web Excel 式报价批量草稿更新。 */
@Note("报价工作表批量更新")
@Component
public class QuoteWorkbookContext {

    private static final int MAX_LINES = 200;

    private final QuoteLineRepository lineRepository;
    private final QuoteCalcContext quoteCalcContext;
    private final QuoteFormulaContext formulaContext;
    private final QuoteAdjustmentContext adjustmentContext;
    private final QuoteViewContext viewContext;

    public QuoteWorkbookContext(QuoteLineRepository lineRepository,
                                QuoteCalcContext quoteCalcContext,
                                QuoteFormulaContext formulaContext,
                                QuoteAdjustmentContext adjustmentContext,
                                QuoteViewContext viewContext) {
        this.lineRepository = lineRepository;
        this.quoteCalcContext = quoteCalcContext;
        this.formulaContext = formulaContext;
        this.adjustmentContext = adjustmentContext;
        this.viewContext = viewContext;
    }

    @Transactional
    public Quote update(Quote quote, Quote request) {
        requireDraft(quote);
        List<QuoteLine> lines = lineRepository.findByQuote(quote);
        if (lines.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "当前报价没有可编辑分项"));
        }
        List<QuoteLine> changes = request.getItems() == null ? List.of() : request.getItems();
        if (changes.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "没有需要保存的报价分项"));
        }
        if (changes.size() > MAX_LINES) {
            throw new DefinedException(new DefaultErrorCode("400", "单次最多保存 " + MAX_LINES + " 行报价"));
        }
        Map<Long, QuoteLine> existingById = lines.stream()
                .collect(Collectors.toMap(QuoteLine::getItemId, Function.identity()));
        Map<Long, String> formulas = new HashMap<>();
        for (QuoteLine change : changes) {
            if (change.getItemId() == null || !existingById.containsKey(change.getItemId())) {
                throw new DefinedException(new DefaultErrorCode("404", "报价明细不存在或不属于当前报价"));
            }
            if (change.getQuantityFormula() != null && !change.getQuantityFormula().isBlank()) {
                formulas.put(change.getItemId(), change.getQuantityFormula().trim());
            }
        }
        Map<Long, BigDecimal> formulaResults = formulaContext.evaluate(lines, formulas);

        // ── 校验阶段：任何一条不合法都不得落库（先全量校验，再统一应用）──
        for (QuoteLine change : changes) {
            validatePriceInput(existingById.get(change.getItemId()), change);
        }

        // ── 应用阶段 ──
        for (QuoteLine change : changes) {
            QuoteLine target = existingById.get(change.getItemId());
            BigDecimal oldQuantity = zero(target.getQuantity());
            BigDecimal unitCost = oldQuantity.signum() > 0
                    ? zero(target.getCost()).divide(oldQuantity, 12, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal quantity = formulaResults.get(target.getItemId());
            if (quantity == null) quantity = change.getQuantity() == null ? oldQuantity : change.getQuantity();
            if (quantity.signum() <= 0) {
                throw new DefinedException(new DefaultErrorCode("400", "工程量必须大于零"));
            }
            target.setQuantity(quantity.setScale(3, RoundingMode.HALF_UP));
            boolean hasFormula = change.getQuantityFormula() != null && !change.getQuantityFormula().isBlank();
            if (hasFormula) {
                target.setQuantityFormula(change.getQuantityFormula().trim());
            } else if (change.getQuantity() != null && change.getQuantity().signum() > 0) {
                target.setQuantityFormula(null);
            }
            // 冻结口径：价格为 0 视为「未修改」（见 validatePriceInput 注释）
            if (change.getMaterialPrice() != null && change.getMaterialPrice().signum() > 0) {
                target.setMaterialPrice(change.getMaterialPrice());
            }
            if (change.getManualPrice() != null && change.getManualPrice().signum() > 0) {
                target.setManualPrice(change.getManualPrice());
            }
            if (change.getFixedTotalPrice() != null && change.getFixedTotalPrice().signum() > 0) {
                target.setFixedTotalPrice(change.getFixedTotalPrice());
            }
            recalculateLine(target, unitCost);
            lineRepository.save(target);
        }
        adjustmentContext.recalculate(quote);
        return viewContext.apply(quote, lineRepository.findByQuote(quote));
    }

    private void recalculateLine(QuoteLine line, BigDecimal unitCost) {
        BigDecimal quantity = line.getQuantity();
        BigDecimal material = zero(line.getMaterialPrice());
        BigDecimal manual = zero(line.getManualPrice());
        BigDecimal fixed = line.getFixedTotalPrice();
        BigDecimal cost = money(quantity.multiply(unitCost));
        String formulaSnapshot = line.getQuantityFormula() == null ? line.getFormula() : line.getQuantityFormula();
        line.setPrice(quoteCalcContext.unitPrice(line.getPricingMode(), quantity, material, manual, fixed));
        line.setAmount(quoteCalcContext.amount(line.getPricingMode(), quantity, material, manual, fixed));
        line.setCost(cost);
        line.setComponents(quoteCalcContext.components(line.getPricingMode(), quantity, material, manual, cost,
                formulaSnapshot, fixed));
    }

    /**
     * 价格与计价模式校验（ANS-BIZ-20261002-002 Q4）。
     *
     * 规则：
     * 1. 负数一律阻断；
     * 2. 按计价模式校验有效价格：MaterialOnly 要求材料价 > 0；ManualOnly 要求人工价 > 0；
     *    FixedTotal 要求固定总价 > 0；MaterialAndManual 允许单侧为 0，但材料+人工合计必须 > 0；
     * 3. 明确传了该计价模式**不适用**的非零价格时必须报错——不允许"返成功但字段没生效"。
     *
     * 传输口径（测试冻结，非静默忽略）：
     * `QuoteLine.materialPrice / manualPrice` 的实体默认值是 `0`（数据库列 NOT NULL 且无默认），
     * 因此请求中「未传该字段」与「显式传 0」在绑定层**无法区分**。
     * 冻结为：**价格字段传 0 视为「未修改」，保持原值**。
     * 需要某个计价侧为零，应使用不适用该侧的计价模式（如 MaterialOnly 的人工价天然为 0），
     * 而不是把该侧显式改成 0。负价一律阻断。
     */
    private void validatePriceInput(QuoteLine target, QuoteLine change) {
        PricingMethod mode = target.getPricingMode() == null
                ? PricingMethod.MaterialAndManual
                : target.getPricingMode();
        BigDecimal material = change.getMaterialPrice();
        BigDecimal manual = change.getManualPrice();
        BigDecimal fixed = change.getFixedTotalPrice();

        rejectNegative(material, "材料单价");
        rejectNegative(manual, "人工单价");
        rejectNegative(fixed, "固定总价");

        // materialPrice / manualPrice：实体默认值为 0，只有 > 0 才视为「本次提供」（0 视为未修改）
        boolean materialUsed = material != null && material.signum() > 0;
        boolean manualUsed = manual != null && manual.signum() > 0;
        // fixedTotalPrice：实体默认为 null，因此非 null 即视为「本次提供」，传 0 也要按无效价阻断
        boolean fixedProvided = fixed != null;

        if (mode == PricingMethod.FixedTotal && (materialUsed || manualUsed)) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "固定总价计价的报价行不接受材料单价或人工单价，请修改固定总价"));
        }
        if (mode != PricingMethod.FixedTotal && fixedProvided && fixed.signum() > 0) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "非固定总价计价的报价行不接受固定总价"));
        }
        if (mode == PricingMethod.MaterialOnly && manualUsed) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "仅材料计价的报价行不接受人工单价"));
        }
        if (mode == PricingMethod.ManualOnly && materialUsed) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "仅人工计价的报价行不接受材料单价"));
        }

        BigDecimal effectiveMaterial = materialUsed ? material : zero(target.getMaterialPrice());
        BigDecimal effectiveManual = manualUsed ? manual : zero(target.getManualPrice());
        BigDecimal effectiveFixed = fixedProvided ? fixed : target.getFixedTotalPrice();

        switch (mode) {
            case MaterialOnly -> {
                if (effectiveMaterial.signum() <= 0) {
                    throw new DefinedException(new DefaultErrorCode("400", "仅材料计价要求材料单价大于零"));
                }
            }
            case ManualOnly -> {
                if (effectiveManual.signum() <= 0) {
                    throw new DefinedException(new DefaultErrorCode("400", "仅人工计价要求人工单价大于零"));
                }
            }
            case FixedTotal -> {
                if (effectiveFixed == null || effectiveFixed.signum() <= 0) {
                    throw new DefinedException(new DefaultErrorCode("400", "固定总价计价要求固定总价大于零"));
                }
            }
            default -> {
                if (effectiveMaterial.add(effectiveManual).signum() <= 0) {
                    throw new DefinedException(new DefaultErrorCode("400", "材料单价与人工单价之和必须大于零"));
                }
            }
        }
    }

    private void rejectNegative(BigDecimal value, String label) {
        if (value != null && value.signum() < 0) {
            throw new DefinedException(new DefaultErrorCode("400", label + "不能为负"));
        }
    }

    private void requireDraft(Quote quote) {
        if (!QuoteStatus.Draft.equals(quote.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "只有草稿报价可以编辑工程量"));
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
