package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceStatus;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceType;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 维护报价草稿中的套餐扣减、升级、替换和增项。 */
@Note("报价草稿调整上下文")
@Component
public class QuoteAdjustmentContext {

    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository lineRepository;
    private final CatalogItemRepository catalogRepository;
    private final QuoteCalcContext quoteCalcContext;

    public QuoteAdjustmentContext(QuoteRepository quoteRepository,
                                  QuoteLineRepository lineRepository,
                                  CatalogItemRepository catalogRepository,
                                  QuoteCalcContext quoteCalcContext) {
        this.quoteRepository = quoteRepository;
        this.lineRepository = lineRepository;
        this.catalogRepository = catalogRepository;
        this.quoteCalcContext = quoteCalcContext;
    }

    @Transactional
    public QuoteLine save(Quote quote, QuoteLine source, boolean deduct,
                          Long baseLineId, Long catalogItemId, BigDecimal quantity) {
        requireDraft(quote);
        if (deduct) {
            if (baseLineId == null) {
                throw new DefinedException(new DefaultErrorCode("400", "请选择需要扣减的套餐项目"));
            }
            QuoteLine base = requireLine(quote, baseLineId);
            if (base.getPackageLine() == null || !Boolean.TRUE.equals(base.getPackageLine().getDeductible())) {
                throw new DefinedException(new DefaultErrorCode("400", "该报价项目不允许扣减"));
            }
            BigDecimal originalAmount = zero(base.getAmount());
            base.setIncluded(false);
            base.setDiscountAmount(originalAmount);
            base.setAmount(BigDecimal.ZERO);
            base.setCost(BigDecimal.ZERO);
            base.setRole(QuoteLineRole.Deduct);
            base.setDifferenceType(QuoteDifferenceType.Deduct);
            base.setDifferenceStatus(QuoteDifferenceStatus.Confirmed);
            QuoteLine saved = lineRepository.save(base);
            recalculate(quote);
            return saved;
        }
        if (catalogItemId == null || quantity == null || quantity.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择报价项目并填写有效数量"));
        }
        CatalogItem catalog = catalogRepository.findByItemIdAndCompany_ComId(
                        catalogItemId, quote.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价项目不存在或无权访问")));
        if (!Boolean.TRUE.equals(catalog.getActive())) {
            throw new DefinedException(new DefaultErrorCode("400", "报价项目已停用"));
        }
        QuoteLine replacedBase = null;
        if (baseLineId != null) {
            QuoteLine base = requireLine(quote, baseLineId);
            replacedBase = base;
            base.setIncluded(false);
            base.setDiscountAmount(zero(base.getAmount()));
            base.setAmount(BigDecimal.ZERO);
            base.setCost(BigDecimal.ZERO);
            base.setRole(source.getRole() == QuoteLineRole.Replace ? QuoteLineRole.Replace : QuoteLineRole.Upgrade);
            base.setDifferenceType(source.getRole() == QuoteLineRole.Replace
                    ? QuoteDifferenceType.Replace : QuoteDifferenceType.Upgrade);
            base.setDifferenceStatus(QuoteDifferenceStatus.Confirmed);
            lineRepository.save(base);
        }

        QuoteLine target = source.getItemId() == null ? new QuoteLine() : requireLine(quote, source.getItemId());
        QuoteLineRole role = source.getRole() == null || source.getRole() == QuoteLineRole.Base
                ? QuoteLineRole.Add : source.getRole();
        target.setCompany(quote.getCompany());
        target.setStore(quote.getStore());
        target.setTeam(quote.getTeam());
        target.setOwner(quote.getCreatedBy());
        target.setQuote(quote);
        target.setProperty(quote.getProperty());
        target.setCatalogItem(catalog);
        target.setSpace(source.getSpace());
        target.setPackageLine(replacedBase == null ? null : replacedBase.getPackageLine());
        target.setRole(role);
        target.setDifferenceType(role == QuoteLineRole.Replace ? QuoteDifferenceType.Replace
                : role == QuoteLineRole.Upgrade ? QuoteDifferenceType.Upgrade
                : source.getDifferenceType() == null || source.getDifferenceType() == QuoteDifferenceType.None
                ? QuoteDifferenceType.DesignOnly : source.getDifferenceType());
        target.setDifferenceStatus(QuoteDifferenceStatus.Confirmed);
        target.setIncluded(source.getIncluded() == null || source.getIncluded());
        target.setDiscountAmount(BigDecimal.ZERO);
        target.setType(catalog.getType());
        target.setCategory(catalog.getCategory());
        target.setName(source.getName() == null || source.getName().isBlank() ? catalog.getName() : source.getName());
        target.setBrand(source.getBrand() == null ? catalog.getBrand() : source.getBrand());
        target.setSpec(source.getSpec() == null ? catalog.getSpec() : source.getSpec());
        target.setUnit(source.getUnit() == null ? catalog.getUnit() : source.getUnit());
        target.setQuantity(quantity);
        target.setMaterialPrice(zero(catalog.getMaterialPrice()));
        target.setManualPrice(zero(catalog.getManualPrice()));
        target.setFixedTotalPrice(catalog.getFixedTotalPrice());
        target.setPricingMode(catalog.getPricingMode());
        target.setPrice(quoteCalcContext.unitPrice(catalog.getPricingMode(), quantity,
                zero(catalog.getMaterialPrice()), zero(catalog.getManualPrice()), catalog.getFixedTotalPrice()));
        target.setAmount(quoteCalcContext.amount(catalog.getPricingMode(), quantity,
                zero(catalog.getMaterialPrice()), zero(catalog.getManualPrice()), catalog.getFixedTotalPrice()));
        target.setCost(quoteCalcContext.cost(quantity, zero(catalog.getCostPrice())));
        target.setComponents(quoteCalcContext.components(catalog.getPricingMode(), quantity,
                zero(catalog.getMaterialPrice()), zero(catalog.getManualPrice()),
                zero(catalog.getCostPrice()), catalog.getFormula(), catalog.getFixedTotalPrice()));
        target.setFormula(catalog.getFormula());
        target.setCraft(catalog.getCraft());
        target.setSort(source.getSort() == null ? nextSort(quote) : source.getSort());
        QuoteLine saved = lineRepository.save(target);
        recalculate(quote);
        return saved;
    }

    @Transactional
    public QuoteLine updateDifference(Quote quote, Long lineId, QuoteDifferenceStatus status) {
        requireDraft(quote);
        if (status == null || status == QuoteDifferenceStatus.None || status == QuoteDifferenceStatus.Pending) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择确认或忽略"));
        }
        QuoteLine line = requireLine(quote, lineId);
        line.setDifferenceStatus(status);
        if (status == QuoteDifferenceStatus.Ignored
                && line.getDifferenceType() == QuoteDifferenceType.DesignOnly) {
            line.setIncluded(false);
            line.setAmount(BigDecimal.ZERO);
            line.setCost(BigDecimal.ZERO);
        }
        QuoteLine saved = lineRepository.save(line);
        recalculate(quote);
        return saved;
    }

    @Transactional
    public void delete(Quote quote, Long lineId) {
        requireDraft(quote);
        QuoteLine line = requireLine(quote, lineId);
        if (!QuoteMode.Custom.equals(quote.getMode())
                && line.getRole() == QuoteLineRole.Base
                && Boolean.TRUE.equals(line.getIncluded())) {
            throw new DefinedException(new DefaultErrorCode("400", "基础套餐项目不能直接删除，请使用扣减或替换"));
        }
        lineRepository.delete(line);
        recalculate(quote);
    }

    private QuoteLine requireLine(Quote quote, Long lineId) {
        return lineRepository.findByQuote(quote).stream()
                .filter(item -> item.getItemId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价明细不存在或不属于当前报价")));
    }

    private void requireDraft(Quote quote) {
        if (!QuoteStatus.Draft.equals(quote.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "只有草稿报价可以调整项目"));
        }
    }

    public void recalculate(Quote quote) {
        List<QuoteLine> lines = lineRepository.findByQuote(quote);
        BigDecimal subtotal = lines.stream()
                .filter(line -> line.getIncluded() == null || line.getIncluded())
                .map(line -> zero(line.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = lines.stream()
                .filter(line -> line.getIncluded() == null || line.getIncluded())
                .map(line -> zero(line.getCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discounted = money(subtotal.multiply(
                quote.getDiscount() == null ? BigDecimal.ONE : quote.getDiscount()));
        BigDecimal tax = money(discounted.multiply(
                quote.getTaxRate() == null ? new BigDecimal("0.05") : quote.getTaxRate()));
        quote.setSubtotal(money(subtotal));
        quote.setTaxAmount(tax);
        quote.setAmount(money(discounted.add(tax)));
        quote.setCost(money(cost));
        quote.setCostComplete(subtotal.signum() == 0 || cost.signum() > 0);
        quoteRepository.save(quote);
    }

    private int nextSort(Quote quote) {
        return lineRepository.findByQuote(quote).stream()
                .map(QuoteLine::getSort)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
