package com.th4.edge.decoboon.quote.context;

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
            if (change.getQuantityFormula() != null) {
                target.setQuantityFormula(change.getQuantityFormula().isBlank() ? null : change.getQuantityFormula().trim());
            }
            if (change.getMaterialPrice() != null) target.setMaterialPrice(change.getMaterialPrice());
            if (change.getManualPrice() != null) target.setManualPrice(change.getManualPrice());
            if (change.getFixedTotalPrice() != null) target.setFixedTotalPrice(change.getFixedTotalPrice());
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
