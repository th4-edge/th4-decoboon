package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.common.support.CraftText;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.pack.context.PackageContext;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceStatus;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceType;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 将套餐报价基线与设计分项合并为一份 Hybrid 报价。 */
@Note("混合报价构建上下文")
@Component
public class HybridQuoteBuilder {

    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository lineRepository;
    private final PackageLineRepository packageLineRepository;
    private final PackageContext packageContext;
    private final QuoteCalcContext quoteCalcContext;
    private final ProjectRepository projectRepository;
    private final DesignItemRepository designItemRepository;

    public HybridQuoteBuilder(QuoteRepository quoteRepository,
                              QuoteLineRepository lineRepository,
                              PackageLineRepository packageLineRepository,
                              PackageContext packageContext,
                              QuoteCalcContext quoteCalcContext,
                              ProjectRepository projectRepository,
                              DesignItemRepository designItemRepository) {
        this.quoteRepository = quoteRepository;
        this.lineRepository = lineRepository;
        this.packageLineRepository = packageLineRepository;
        this.packageContext = packageContext;
        this.quoteCalcContext = quoteCalcContext;
        this.projectRepository = projectRepository;
        this.designItemRepository = designItemRepository;
    }

    @Transactional
    public Quote build(Design design, PackageTemplate template, BigDecimal discount) {
        validate(design, template);
        BigDecimal ratio = design.getArea().divide(template.getArea(), 6, RoundingMode.HALF_UP);
        List<PackageLine> packageLines = packageLineRepository
                .findByTemplate_TemplateIdOrderBySortAscItemIdAsc(template.getTemplateId());
        if (packageLines.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "套餐尚未配置项目行，不能用于设计方案"));
        }
        BigDecimal packageCost = packageLines.stream()
                .map(packageContext::calculateItemCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(ratio);
        BigDecimal packageAmount = zero(template.getPrice()).multiply(ratio);
        BigDecimal rate = discount == null ? template.getDiscount() : discount;
        validateDiscount(rate);

        Quote quote = new Quote();
        quote.setNo("Q" + System.currentTimeMillis());
        quote.setTitle(design.getName() + "·" + template.getName() + "混合报价");
        quote.setCompany(design.getCompany());
        quote.setStore(design.getStore());
        quote.setTeam(design.getTeam());
        quote.setProperty(design.getProperty());
        quote.setCustomer(design.getCustomer());
        quote.setDesign(design);
        quote.setPackageTemplate(template);
        quote.setCreatedBy(design.getOwner());
        quote.setVersion((int) quoteRepository.countByDesign_DesignId(design.getDesignId()) + 1);
        quote.setMode(QuoteMode.Hybrid);
        quote.setArea(zero(design.getArea()));
        quote.setSubtotal(money(packageAmount));
        quote.setDiscount(rate);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setStatus(QuoteStatus.Draft);
        quote = quoteRepository.save(quote);

        List<QuoteLine> result = new ArrayList<>();
        List<DesignItem> designItems = designItemRepository(design.getDesignId());
        Set<Long> usedDesignItems = new HashSet<>();
        for (int index = 0; index < packageLines.size(); index++) {
            PackageLine packageLine = packageLines.get(index);
            QuoteLine base = baseLine(quote, packageLine, ratio, packageAmount, packageCost, index, packageLines.size());
            DesignItem match = match(packageLine, designItems, usedDesignItems);
            if (match == null) {
                base.setDifferenceType(QuoteDifferenceType.PackageOnly);
                base.setDifferenceStatus(QuoteDifferenceStatus.Pending);
            } else {
                usedDesignItems.add(match.getItemId());
                base.setDesignItem(match);
                boolean quantityChanged = compare(base.getQuantity(), match.getQuantity()) != 0;
                base.setDifferenceType(quantityChanged ? QuoteDifferenceType.QuantityChanged : QuoteDifferenceType.Matched);
                base.setDifferenceStatus(quantityChanged ? QuoteDifferenceStatus.Pending : QuoteDifferenceStatus.Confirmed);
            }
            result.add(base);
            if (base.getDifferenceType() == QuoteDifferenceType.QuantityChanged && match != null) {
                result.add(quantityAdjustment(quote, packageLine, base, match, index));
            }
        }
        int sort = result.size();
        for (DesignItem designItem : designItems) {
            if (designItem.getItemId() == null || usedDesignItems.contains(designItem.getItemId())) continue;
            QuoteLine line = designOnlyLine(quote, designItem, sort++);
            result.add(line);
        }
        lineRepository.saveAll(result);
        recalculate(quote, result);
        quote.setItems(result);
        return quote;
    }

    private void validate(Design design, PackageTemplate template) {
        if (design == null || design.getDesignId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "设计方案不能为空"));
        }
        if (!List.of(DesignStatus.Ready, DesignStatus.Presented, DesignStatus.Selected).contains(design.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "设计方案尚未完成，不能应用套餐"));
        }
        if (template == null || template.getTemplateId() == null || template.getArea() == null || template.getArea().signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "套餐标准面积配置无效"));
        }
        if (!design.getCompany().getComId().equals(template.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "设计方案与套餐不属于同一企业"));
        }
        if (design.getArea() == null || design.getArea().signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "设计方案面积无效"));
        }
        Long propertyId = design.getProperty().getPropertyId();
        Long comId = design.getCompany().getComId();
        if (projectRepository.existsByCompany_ComIdAndProperty_PropertyId(comId, propertyId)
                || quoteRepository.existsByProperty_PropertyIdAndStatusIn(propertyId,
                List.of(QuoteStatus.Accepted, QuoteStatus.Converted))) {
            throw new DefinedException(new DefaultErrorCode("400", "该房屋已有客户确认报价或项目，不能继续生成报价"));
        }
    }

    private List<DesignItem> designItemRepository(Long designId) {
        return designItemRepository.findByDesign_DesignIdOrderBySortAsc(designId);
    }

    private QuoteLine baseLine(Quote quote, PackageLine packageLine, BigDecimal ratio,
                               BigDecimal packageAmount, BigDecimal packageCost, int sort, int packageLineCount) {
        BigDecimal itemCost = packageContext.calculateItemCost(packageLine).multiply(ratio);
        BigDecimal itemAmount = packageCost.signum() == 0
                ? packageAmount.divide(BigDecimal.valueOf(Math.max(1, packageLineCount)), 2, RoundingMode.HALF_UP)
                : packageAmount.multiply(itemCost).divide(packageCost, 2, RoundingMode.HALF_UP);
        BigDecimal quantity = zero(packageLine.getQuantityFactor()).multiply(ratio);
        BigDecimal price = quantity.signum() == 0 ? itemAmount : itemAmount.divide(quantity, 2, RoundingMode.HALF_UP);

        QuoteLine line = new QuoteLine();
        line.setCompany(quote.getCompany());
        line.setStore(quote.getStore());
        line.setTeam(quote.getTeam());
        line.setOwner(quote.getCreatedBy());
        line.setQuote(quote);
        line.setProperty(quote.getProperty());
        line.setPackageLine(packageLine);
        line.setCatalogItem(packageLine.getItem());
        line.setCraft(packageLine.getItem() == null ? null : packageLine.getItem().getCraft());
        line.setType(packageLine.getItem() == null ? ItemType.General : packageLine.getItem().getType());
        line.setCategory(packageLine.getSection() == null ? "综合项目" : packageLine.getSection().getName());
        line.setName(packageLine.getName());
        line.setBrand(packageLine.getBrand());
        line.setSpec(packageLine.getSpec());
        line.setUnit(packageLine.getUnit());
        line.setQuantity(quantity);
        line.setPrice(money(price));
        line.setAmount(money(itemAmount));
        line.setCost(money(itemCost));
        line.setPricingMode(packageLine.getPricingMethod());
        line.setFormula("套餐模板分摊");
        line.setRole(QuoteLineRole.Base);
        line.setIncluded(true);
        line.setDifferenceType(QuoteDifferenceType.None);
        line.setDifferenceStatus(QuoteDifferenceStatus.None);
        line.setSort(sort);
        return line;
    }

    private DesignItem match(PackageLine packageLine, List<DesignItem> items, Set<Long> used) {
        List<DesignItem> candidates = items.stream()
                .filter(item -> item.getItemId() != null && !used.contains(item.getItemId()))
                .filter(item -> sameCatalog(packageLine, item))
                .toList();
        if (candidates.size() == 1) return candidates.get(0);
        if (!candidates.isEmpty()) {
            DesignItem preferred = candidates.stream().filter(item -> sameSpaceType(packageLine, item)).findFirst().orElse(null);
            if (preferred != null) return preferred;
        }
        return items.stream()
                .filter(item -> item.getItemId() != null && !used.contains(item.getItemId()))
                .filter(item -> sameBusinessKey(packageLine, item))
                .findFirst()
                .orElse(null);
    }

    private boolean sameCatalog(PackageLine line, DesignItem item) {
        return line.getItem() != null && item.getCatalogItem() != null
                && line.getItem().getItemId().equals(item.getCatalogItem().getItemId());
    }

    private boolean sameBusinessKey(PackageLine line, DesignItem item) {
        return line.getItem() != null && item.getCatalogItem() != null
                && line.getItem().getCategory().equals(item.getCatalogItem().getCategory())
                && line.getName().equals(item.getName());
    }

    private boolean sameSpaceType(PackageLine line, DesignItem item) {
        PackageSection section = line.getSection();
        Space space = item.getSpace();
        return section != null && space != null && section.getSpaceTypes() != null
                && section.getSpaceTypes().contains(space.getType());
    }

    private QuoteLine quantityAdjustment(Quote quote, PackageLine packageLine, QuoteLine base,
                                         DesignItem designItem, int sort) {
        BigDecimal delta = zero(designItem.getQuantity()).subtract(zero(base.getQuantity()));
        BigDecimal quantity = delta.abs();
        BigDecimal unitSale = zero(designItem.getAmount()).divide(
                zero(designItem.getQuantity()).signum() == 0 ? BigDecimal.ONE : zero(designItem.getQuantity()), 2, RoundingMode.HALF_UP);
        BigDecimal unitCost = zero(designItem.getCost()).divide(
                zero(designItem.getQuantity()).signum() == 0 ? BigDecimal.ONE : zero(designItem.getQuantity()), 2, RoundingMode.HALF_UP);
        BigDecimal sign = delta.signum() < 0 ? BigDecimal.valueOf(-1) : BigDecimal.ONE;

        QuoteLine line = new QuoteLine();
        line.setCompany(quote.getCompany());
        line.setStore(quote.getStore());
        line.setTeam(quote.getTeam());
        line.setOwner(quote.getCreatedBy());
        line.setQuote(quote);
        line.setProperty(quote.getProperty());
        line.setDesignItem(designItem);
        line.setCatalogItem(designItem.getCatalogItem());
        line.setSpace(designItem.getSpace());
        line.setPackageLine(packageLine);
        line.setRole(delta.signum() > 0 ? QuoteLineRole.Add : QuoteLineRole.Deduct);
        line.setDifferenceType(QuoteDifferenceType.QuantityChanged);
        line.setDifferenceStatus(QuoteDifferenceStatus.Pending);
        line.setIncluded(true);
        line.setType(designItem.getType());
        line.setCategory(designItem.getCategory());
        line.setName(designItem.getName() + (delta.signum() > 0 ? "（增量）" : "（减量）"));
        line.setBrand(designItem.getBrand());
        line.setSpec(designItem.getSpec());
        line.setUnit(designItem.getUnit());
        line.setQuantity(quantity);
        line.setMaterialPrice(zero(designItem.getMaterialPrice()));
        line.setManualPrice(zero(designItem.getManualPrice()));
        line.setPricingMode(designItem.getPricingMode());
        line.setPrice(money(unitSale));
        line.setAmount(money(unitSale.multiply(quantity).multiply(sign)));
        line.setCost(money(unitCost.multiply(quantity).multiply(sign)));
        line.setFormula("设计数量差异");
        line.setCraft(CraftText.firstNonBlank(designItem.getCraft(),
                designItem.getCatalogItem() == null ? null : designItem.getCatalogItem().getCraft()));
        line.setSort(sort + 10000);
        return line;
    }

    private QuoteLine designOnlyLine(Quote quote, DesignItem designItem, int sort) {
        QuoteLine line = new QuoteLine();
        line.setCompany(quote.getCompany());
        line.setStore(quote.getStore());
        line.setTeam(quote.getTeam());
        line.setOwner(quote.getCreatedBy());
        line.setQuote(quote);
        line.setProperty(quote.getProperty());
        line.setDesignItem(designItem);
        line.setCatalogItem(designItem.getCatalogItem());
        line.setSpace(designItem.getSpace());
        line.setRole(QuoteLineRole.Add);
        line.setDifferenceType(QuoteDifferenceType.DesignOnly);
        line.setDifferenceStatus(QuoteDifferenceStatus.Pending);
        line.setIncluded(true);
        line.setType(designItem.getType());
        line.setCategory(designItem.getCategory());
        line.setName(designItem.getName());
        line.setBrand(designItem.getBrand());
        line.setSpec(designItem.getSpec());
        line.setUnit(designItem.getUnit());
        line.setQuantity(zero(designItem.getQuantity()));
        line.setMaterialPrice(zero(designItem.getMaterialPrice()));
        line.setManualPrice(zero(designItem.getManualPrice()));
        line.setPricingMode(designItem.getPricingMode());
        line.setPrice(quoteCalcContext.unitPrice(designItem.getPricingMode(), zero(designItem.getQuantity()),
                zero(designItem.getMaterialPrice()), zero(designItem.getManualPrice())));
        line.setAmount(zero(designItem.getAmount()));
        line.setCost(zero(designItem.getCost()));
        line.setComponents(quoteCalcContext.components(designItem.getPricingMode(), zero(designItem.getQuantity()),
                zero(designItem.getMaterialPrice()), zero(designItem.getManualPrice()), zero(designItem.getCost()),
                designItem.getFormula()));
        line.setFormula("设计新增项目");
        line.setCraft(CraftText.firstNonBlank(designItem.getCraft(),
                designItem.getCatalogItem() == null ? null : designItem.getCatalogItem().getCraft()));
        line.setSort(sort);
        return line;
    }

    private void recalculate(Quote quote, List<QuoteLine> lines) {
        BigDecimal subtotal = lines.stream().filter(line -> line.getIncluded() == null || line.getIncluded())
                .map(line -> zero(line.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = lines.stream().filter(line -> line.getIncluded() == null || line.getIncluded())
                .map(line -> zero(line.getCost())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discounted = money(subtotal.multiply(quote.getDiscount()));
        BigDecimal tax = money(discounted.multiply(quote.getTaxRate()));
        quote.setSubtotal(money(subtotal));
        quote.setTaxAmount(tax);
        quote.setAmount(money(discounted.add(tax)));
        quote.setCost(money(cost));
        quote.setCostComplete(subtotal.signum() == 0 || cost.signum() > 0);
        quoteRepository.save(quote);
    }

    private long compare(BigDecimal left, BigDecimal right) {
        return zero(left).compareTo(zero(right));
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return zero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateDiscount(BigDecimal rate) {
        if (rate == null || rate.signum() <= 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "折扣率必须在0到1之间"));
        }
    }
}
