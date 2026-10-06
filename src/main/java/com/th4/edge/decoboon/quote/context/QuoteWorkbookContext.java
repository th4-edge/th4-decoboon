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
    private final QuoteRevisionContext revisionContext;

    public QuoteWorkbookContext(QuoteLineRepository lineRepository,
                                QuoteCalcContext quoteCalcContext,
                                QuoteFormulaContext formulaContext,
                                QuoteAdjustmentContext adjustmentContext,
                                QuoteViewContext viewContext,
                                QuoteRevisionContext revisionContext) {
        this.lineRepository = lineRepository;
        this.quoteCalcContext = quoteCalcContext;
        this.formulaContext = formulaContext;
        this.adjustmentContext = adjustmentContext;
        this.viewContext = viewContext;
        this.revisionContext = revisionContext;
    }

    @Transactional
    public Quote update(Quote quote, Quote request) {
        return update(quote, request, null);
    }

    /**
     * 带并发基准的批量更新。
     *
     * `baselineToken` 为客户端读取时拿到的内容修订令牌：
     * - 为 null（App 或旧客户端）→ 跳过冲突校验，保持既有行为；
     * - 与当前内容指纹不一致 → **409，不落库、不重算、不覆盖**；
     * - 一致 → 正常执行，响应回传新的修订令牌。
     */
    @Transactional
    public Quote update(Quote quote, Quote request, String baselineToken) {
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
        // ── 并发冲突校验：必须在任何写入之前 ──
        if (baselineToken != null && !baselineToken.isBlank()
                && !baselineToken.equals(revisionContext.token(quote, lines))) {
            throw new DefinedException(new DefaultErrorCode("409",
                    "报价已被他人修改，请重新读取后再保存"));
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
            boolean hasFormula = change.getQuantityFormula() != null && !change.getQuantityFormula().isBlank();
            boolean quantityProvided = Boolean.TRUE.equals(change.getQuantityProvided());

            // ECD-014：工程量按「公式 > 显式传入 > 保留原值」三路取值，
            // 不再被实体默认值 1 覆盖。
            BigDecimal quantity;
            if (hasFormula) {
                quantity = formulaResults.get(target.getItemId());
            } else if (quantityProvided) {
                quantity = change.getQuantity();
            } else {
                quantity = oldQuantity;
            }
            if (quantity == null || quantity.signum() < 0) {
                throw new DefinedException(new DefaultErrorCode("400", "工程量不能为负数"));
            }
            target.setQuantity(quantity.setScale(3, RoundingMode.HALF_UP));
            if (hasFormula) {
                target.setQuantityFormula(change.getQuantityFormula().trim());
            } else if (quantityProvided) {
                target.setQuantityFormula(null);
            }
            // 价格：只有显式提供才写入（未提供保留原值），显式值合法性已在校验阶段判定
            if (Boolean.TRUE.equals(change.getMaterialPriceProvided()) && change.getMaterialPrice() != null) {
                target.setMaterialPrice(change.getMaterialPrice());
            }
            if (Boolean.TRUE.equals(change.getManualPriceProvided()) && change.getManualPrice() != null) {
                target.setManualPrice(change.getManualPrice());
            }
            if (Boolean.TRUE.equals(change.getFixedTotalPriceProvided()) && change.getFixedTotalPrice() != null) {
                target.setFixedTotalPrice(change.getFixedTotalPrice());
            }
            recalculateLine(target, unitCost);
            lineRepository.save(target);
        }
        adjustmentContext.recalculate(quote);
        List<QuoteLine> saved = lineRepository.findByQuote(quote);
        Quote view = viewContext.apply(quote, saved);
        view.setRevision(revisionContext.token(quote, saved));
        return view;
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
     * ECD-014 之后：由请求的**显式提供标记**（`*Provided`，@Transient 不持久化不序列化）
     * 区分「未传」与「显式传值」——未传保留原值（含 0），显式传 0/负数按下列规则校验：
     * 负数一律 400；显式 0 是否允许由计价模式的有效价格规则决定。
     */
    private void validatePriceInput(QuoteLine target, QuoteLine change) {
        PricingMethod mode = target.getPricingMode() == null
                ? PricingMethod.MaterialAndManual
                : target.getPricingMode();
        BigDecimal material = change.getMaterialPrice();
        BigDecimal manual = change.getManualPrice();
        BigDecimal fixed = change.getFixedTotalPrice();

        // ECD-014：以「显式提供标记」判断是否本次传入，不再依赖实体默认值推断
        boolean materialProvided = Boolean.TRUE.equals(change.getMaterialPriceProvided()) && material != null;
        boolean manualProvided = Boolean.TRUE.equals(change.getManualPriceProvided()) && manual != null;
        boolean fixedProvided = Boolean.TRUE.equals(change.getFixedTotalPriceProvided()) && fixed != null;

        // 负数：仅对显式传入的值判定
        if (materialProvided) rejectNegative(material, "材料单价");
        if (manualProvided) rejectNegative(manual, "人工单价");
        if (fixedProvided) rejectNegative(fixed, "固定总价");

        if (mode == PricingMethod.FixedTotal && ((materialProvided && material.signum() > 0) || (manualProvided && manual.signum() > 0))) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "固定总价计价的报价行不接受材料单价或人工单价，请修改固定总价"));
        }
        if (mode != PricingMethod.FixedTotal && fixedProvided && fixed.signum() > 0) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "非固定总价计价的报价行不接受固定总价"));
        }
        if (mode == PricingMethod.MaterialOnly && manualProvided && manual.signum() > 0) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "仅材料计价的报价行不接受人工单价"));
        }
        if (mode == PricingMethod.ManualOnly && materialProvided && material.signum() > 0) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "仅人工计价的报价行不接受材料单价"));
        }

        BigDecimal effectiveMaterial = materialProvided ? material : zero(target.getMaterialPrice());
        BigDecimal effectiveManual = manualProvided ? manual : zero(target.getManualPrice());
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
