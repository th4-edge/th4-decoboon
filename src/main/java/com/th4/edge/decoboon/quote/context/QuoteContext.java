package com.th4.edge.decoboon.quote.context;

import com.th4.edge.components.lock.DistributedLocked;

import com.th4.edge.decoboon.common.support.CraftText;
import com.th4.edge.decoboon.pack.context.PackageContext;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.context.DesignLifecycleContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户报价领域上下文。
 *
 * <p>同时支持设计分项报价和旧套餐报价。两类报价的所有金额都由后端重新计算，
 * 前端传入金额不会直接生效。</p>
 */
@Component
public class QuoteContext {

    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository quoteItemRepository;
    private final PackageContext giftPackContext;
    private final CustomerRepository customerRepository;
    private final DesignItemRepository designItemRepository;
    private final DesignLifecycleContext designLifecycleContext;
    private final QuoteCalcContext quoteCalcContext;
    private final ProjectRepository projectRepository;
    private final PropertyContext propertyContext;
    private final PropertyRepository propertyRepository;

    public QuoteContext(QuoteRepository quoteRepository,
                        QuoteLineRepository quoteItemRepository,
                        PackageContext giftPackContext,
                        CustomerRepository customerRepository,
                        DesignItemRepository designItemRepository,
                        DesignLifecycleContext designLifecycleContext,
                        QuoteCalcContext quoteCalcContext,
                        ProjectRepository projectRepository,
                        PropertyContext propertyContext,
                        PropertyRepository propertyRepository) {
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.giftPackContext = giftPackContext;
        this.customerRepository = customerRepository;
        this.designItemRepository = designItemRepository;
        this.designLifecycleContext = designLifecycleContext;
        this.quoteCalcContext = quoteCalcContext;
        this.projectRepository = projectRepository;
        this.propertyContext = propertyContext;
        this.propertyRepository = propertyRepository;
    }

    /** 根据设计方案的分项清单生成客户报价快照。 */
    @Transactional
    public Quote create(Design design, BigDecimal discount) {
        if (design == null || design.getDesignId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "设计方案不能为空"));
        }
        if (!List.of(DesignStatus.Ready, DesignStatus.Presented, DesignStatus.Selected)
                .contains(design.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "设计方案尚未完成，不能生成客户报价"));
        }
        Property designProperty = design.getProperty() == null
                ? propertyContext.ensureForCustomer(design.getCustomer(), design.getOwner())
                : design.getProperty();
        ensurePropertyOpen(designProperty);
        List<DesignItem> designItems = designItemRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId());
        if (designItems.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "设计方案尚未配置施工或材料分项"));
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (DesignItem item : designItems) {
            subtotal = subtotal.add(effectiveAmount(item));
            totalCost = totalCost.add(zero(item.getCost()));
        }
        BigDecimal rate = discount == null ? BigDecimal.ONE : discount;
        if (rate.signum() <= 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "折扣率必须在0到1之间"));
        }
        BigDecimal discounted = money(subtotal.multiply(rate));
        BigDecimal tax = money(discounted.multiply(new BigDecimal("0.05")));

        Quote quote = new Quote();
        quote.setNo("Q" + System.currentTimeMillis());
        quote.setTitle(design.getName() + "设计报价");
        quote.setCompany(design.getCompany());
        quote.setStore(design.getStore());
        quote.setTeam(design.getTeam());
        quote.setProperty(designProperty);
        quote.setCustomer(design.getCustomer());
        quote.setDesign(design);
        quote.setCreatedBy(design.getOwner());
        quote.setVersion((int) quoteRepository.countByDesign_DesignId(design.getDesignId()) + 1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(zero(design.getArea()));
        quote.setSubtotal(money(subtotal));
        quote.setDiscount(rate);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setTaxAmount(tax);
        quote.setAmount(money(discounted.add(tax)));
        quote.setCost(money(totalCost));
        quote.setCostComplete(subtotal.signum() == 0 || totalCost.signum() > 0);
        quote.setStatus(QuoteStatus.Draft);
        quoteRepository.save(quote);

        List<QuoteLine> quoteItems = new ArrayList<>();
        for (DesignItem designItem : designItems) {
            quoteItems.add(snapshot(quote, designItem));
        }
        quoteItemRepository.saveAll(quoteItems);
        Customer customer = design.getCustomer();
        customer.setStatus(CustStatus.Quoted);
        customerRepository.save(customer);
        propertyContext.updateStatus(designProperty, com.th4.edge.decoboon.property.configure.PropertyStatus.Quoted);
        quote.setItems(quoteItems);
        return quote;
    }

    /** 根据 BIM 套餐创建客户报价快照，兼容既有套餐流程。 */
    @Transactional
    public Quote create(Customer customer, PackageTemplate packageTemplate, BigDecimal area,
                        BigDecimal discount, Employee employee) {
        return create(customer, packageTemplate, propertyContext.ensureForCustomer(customer, employee),
                area, discount, employee);
    }

    @Transactional
    public Quote create(Customer customer, PackageTemplate packageTemplate, Property property, BigDecimal area,
                        BigDecimal discount, Employee employee) {
        if (customer == null || packageTemplate == null || area == null || area.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "客户、套餐和有效面积不能为空"));
        }
        if (packageTemplate.getArea() == null || packageTemplate.getArea().signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "套餐标准面积配置无效"));
        }
        if (customer.getCompany() == null || packageTemplate.getCompany() == null
                || !customer.getCompany().getComId().equals(packageTemplate.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "客户与套餐不属于同一企业"));
        }
        ensurePropertyOpen(property);

        BigDecimal ratio = area.divide(packageTemplate.getArea(), 6, RoundingMode.HALF_UP);
        BigDecimal packCost = giftPackContext.calculateCost(packageTemplate);
        BigDecimal amount = money(packageTemplate.getPrice().multiply(ratio));
        BigDecimal cost = money(packCost.multiply(ratio));

        Quote quote = new Quote();
        quote.setNo("Q" + System.currentTimeMillis());
        quote.setTitle(packageTemplate.getName() + "套餐报价");
        if (property == null || !property.getCustomer().getCustId().equals(customer.getCustId())) {
            throw new DefinedException(new DefaultErrorCode("400", "套餐报价必须绑定客户所属房屋"));
        }
        quote.setCompany(customer.getCompany());
        quote.setStore(property.getStore() == null ? employee.getStore() : property.getStore());
        quote.setTeam(property.getTeam() == null ? employee.getTeam() : property.getTeam());
        quote.setProperty(property);
        quote.setCustomer(customer);
        quote.setPackageTemplate(packageTemplate);
        quote.setCreatedBy(employee);
        quote.setVersion((int) quoteRepository
                .countByCompany_ComIdAndCustomer_CustIdAndPackageTemplate_TemplateId(
                        customer.getCompany().getComId(), customer.getCustId(), packageTemplate.getTemplateId()) + 1);
        quote.setMode(QuoteMode.Package);
        quote.setArea(area.setScale(2, RoundingMode.HALF_UP));
        BigDecimal rate = discount != null
                ? discount
                : (packageTemplate.getDiscount() == null ? BigDecimal.ONE : packageTemplate.getDiscount());
        if (rate.signum() <= 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "折扣率必须在0到1之间"));
        }
        BigDecimal discounted = money(amount.multiply(rate));
        BigDecimal tax = money(discounted.multiply(new BigDecimal("0.05")));
        quote.setSubtotal(amount);
        quote.setDiscount(rate);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setTaxAmount(tax);
        quote.setAmount(money(discounted.add(tax)));
        quote.setCost(cost);
        quote.setCostComplete(amount.signum() == 0 || cost.signum() > 0);
        quote.setStatus(QuoteStatus.Draft);
        quoteRepository.save(quote);

        List<QuoteLine> items = buildItems(quote, packageTemplate, ratio, amount, cost);
        quoteItemRepository.saveAll(items);
        customer.setStatus(CustStatus.Quoted);
        customerRepository.save(customer);
        propertyContext.updateStatus(property, com.th4.edge.decoboon.property.configure.PropertyStatus.Quoted);
        quote.setItems(items);
        return quote;
    }

    @Transactional
    public Quote submit(Long quoteId) {
        Quote quote = requireQuote(quoteId);
        if (!QuoteStatus.Draft.equals(quote.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "只有草稿报价可以提交"));
        }
        boolean pendingDifference = quoteItemRepository.findByQuote(quote).stream()
                .anyMatch(line -> QuoteDifferenceStatus.Pending.equals(line.getDifferenceStatus()));
        if (pendingDifference) {
            throw new DefinedException(new DefaultErrorCode("400", "存在待确认的设计差异，请先确认、忽略或调整后再提交"));
        }
        quote.setStatus(QuoteStatus.Submitted);
        quote.setSubmittedAt(java.time.LocalDateTime.now());
        return quoteRepository.save(quote);
    }

    @DistributedLocked(key = "#root.acceptLockKey(#quoteId)")
    @Transactional
    public Quote accept(Long quoteId) {
        Quote quote = requireQuote(quoteId);
        if (!QuoteStatus.Submitted.equals(quote.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "只有已提交报价可以接受"));
        }
        Property property = propertyRepository.findByPropertyIdAndCompany_ComId(
                        quote.getProperty().getPropertyId(), quote.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "房屋不存在")));
        Customer customer = customerRepository.findByCustIdAndCompany_ComId(
                        quote.getCustomer().getCustId(), quote.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "客户不存在")));
        ensureNoOtherConfirmedQuote(property, quote.getQuoteId());
        supersedeOtherQuotes(property, quote.getQuoteId());
        quote.setProperty(property);
        quote.setCustomer(customer);
        quote.setStatus(QuoteStatus.Accepted);
        quote.setAcceptedAt(java.time.LocalDateTime.now());
        customer.setStatus(CustStatus.Quoted);
        customerRepository.save(customer);
        Quote accepted = quoteRepository.save(quote);
        // 客户接受报价即代表主推方案被选中，推进设计方案状态，避免方案长期停留在待谈单。
        designLifecycleContext.markSelected(accepted.getDesign());
        return accepted;
    }

    @Transactional
    public Quote reject(Long quoteId) {
        Quote quote = requireQuote(quoteId);
        if (!QuoteStatus.Submitted.equals(quote.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "只有已提交报价可以驳回"));
        }
        quote.setStatus(QuoteStatus.Rejected);
        quote.setRejectedAt(java.time.LocalDateTime.now());
        return quoteRepository.save(quote);
    }

    @Transactional
    public Quote convert(Long quoteId) {
        Quote quote = requireQuote(quoteId);
        if (!QuoteStatus.Accepted.equals(quote.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "只有客户接受的报价可以转项目"));
        }
        quote.setStatus(QuoteStatus.Converted);
        quote.setConvertedAt(java.time.LocalDateTime.now());
        return quoteRepository.save(quote);
    }

    private void ensurePropertyOpen(Property property) {
        if (property == null || property.getPropertyId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "报价必须关联客户房屋"));
        }
        if (projectRepository.existsByCompany_ComIdAndProperty_PropertyId(
                property.getCompany().getComId(), property.getPropertyId())
                || quoteRepository.existsByProperty_PropertyIdAndStatusIn(property.getPropertyId(),
                List.of(QuoteStatus.Accepted, QuoteStatus.Converted))) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该房屋已有客户确认报价，不能继续新增报价；后续调整请发起合同变更单"));
        }
    }

    private void supersedeOtherQuotes(Property property, Long winnerQuoteId) {
        List<Quote> history = quoteRepository.findByProperty_PropertyIdOrderByQuoteIdDesc(property.getPropertyId()).stream()
                .filter(item -> !item.getQuoteId().equals(winnerQuoteId))
                .filter(item -> item.getStatus() == QuoteStatus.Draft
                        || item.getStatus() == QuoteStatus.Submitted
                        || item.getStatus() == QuoteStatus.Accepted)
                .peek(item -> item.setStatus(QuoteStatus.Superseded))
                .toList();
        if (!history.isEmpty()) quoteRepository.saveAll(history);
    }

    private void ensureNoOtherConfirmedQuote(Property property, Long quoteId) {
        Long comId = property.getCompany().getComId();
        if (projectRepository.existsByCompany_ComIdAndProperty_PropertyId(comId, property.getPropertyId())) {
            throw new DefinedException(new DefaultErrorCode("400", "该房屋已经生成唯一项目，不能再次确认报价"));
        }
        if (quoteRepository.existsByProperty_PropertyIdAndStatusInAndQuoteIdNot(
                property.getPropertyId(), List.of(QuoteStatus.Accepted, QuoteStatus.Converted), quoteId)) {
            throw new DefinedException(new DefaultErrorCode("400", "该房屋已有其他客户确认报价"));
        }
    }

    public Quote requireQuote(Long quoteId) {
        if (quoteId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "报价索引不能为空"));
        }
        return quoteRepository.findById(quoteId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在")));
    }

    private QuoteLine snapshot(Quote quote, DesignItem source) {
        QuoteLine item = new QuoteLine();
        item.setCompany(quote.getCompany());
        item.setProperty(quote.getProperty());
        item.setStore(quote.getStore());
        item.setTeam(quote.getTeam());
        item.setOwner(quote.getCreatedBy());
        item.setQuote(quote);
        item.setDesignItem(source);
        item.setCatalogItem(source.getCatalogItem());
        item.setSpace(source.getSpace());
        item.setType(source.getType() == null ? ItemType.General : source.getType());
        item.setCategory(source.getCategory());
        item.setName(source.getName());
        item.setBrand(source.getBrand());
        item.setSpec(source.getSpec());
        item.setUnit(source.getUnit());
        item.setQuantity(zero(source.getQuantity()));
        item.setMaterialPrice(zero(source.getMaterialPrice()));
        item.setManualPrice(zero(source.getManualPrice()));
        item.setFixedTotalPrice(source.getFixedTotalPrice());
        item.setPricingMode(source.getPricingMode());
        item.setPrice(quoteCalcContext.unitPrice(source.getPricingMode(), zero(source.getQuantity()),
                zero(source.getMaterialPrice()), zero(source.getManualPrice()), source.getFixedTotalPrice()));
        item.setAmount(effectiveAmount(source));
        item.setCost(zero(source.getCost()));
        item.setComponents(quoteCalcContext.components(source.getPricingMode(), zero(source.getQuantity()),
                zero(source.getMaterialPrice()), zero(source.getManualPrice()), zero(source.getCost()),
                source.getFormula(), source.getFixedTotalPrice()));
        item.setFormula(source.getFormula());
        // 施工工艺按"设计分项快照 → 材料库主数据"冻结，签约后不随材料库改动回溯。
        item.setCraft(CraftText.firstNonBlank(source.getCraft(),
                source.getCatalogItem() == null ? null : source.getCatalogItem().getCraft()));
        item.setRole(QuoteLineRole.Base);
        item.setSort(source.getSort());
        item.setStore(quote.getStore());
        item.setTeam(quote.getTeam());
        item.setOwner(quote.getCreatedBy());
        return item;
    }

    private List<QuoteLine> buildItems(Quote quote, PackageTemplate packageTemplate, BigDecimal ratio,
                                       BigDecimal amount, BigDecimal totalCost) {
        List<QuoteLine> result = new ArrayList<>();
        if (packageTemplate.getItems() == null || packageTemplate.getItems().isEmpty()) {
            return result;
        }
        int index = 0;
        for (PackageLine packageLine : packageTemplate.getItems()) {
            BigDecimal itemCost = giftPackContext.calculateItemCost(packageLine).multiply(ratio);
            BigDecimal itemAmount = totalCost.signum() == 0
                    ? amount.divide(BigDecimal.valueOf(packageTemplate.getItems().size()), 2, RoundingMode.HALF_UP)
                    : amount.multiply(itemCost).divide(totalCost, 2, RoundingMode.HALF_UP);
            BigDecimal itemQuantity = (packageLine.getQuantityFactor() == null ? BigDecimal.ONE : packageLine.getQuantityFactor())
                    .multiply(ratio);
            BigDecimal itemPrice = itemQuantity.signum() == 0
                    ? itemAmount : itemAmount.divide(itemQuantity, 2, RoundingMode.HALF_UP);

            QuoteLine item = new QuoteLine();
            item.setCompany(quote.getCompany());
            item.setProperty(quote.getProperty());
            item.setStore(quote.getStore());
            item.setTeam(quote.getTeam());
            item.setOwner(quote.getCreatedBy());
            item.setQuote(quote);
            item.setPackageLine(packageLine);
            item.setCatalogItem(packageLine.getItem());
            item.setCraft(packageLine.getItem() == null ? null : packageLine.getItem().getCraft());
            item.setType(packageLine.getItem() == null ? ItemType.General : packageLine.getItem().getType());
            item.setCategory(packageLine.getSection() == null ? "综合项目" : packageLine.getSection().getName());
            item.setName(packageLine.getItem() == null ? packageLine.getName() : packageLine.getItem().getName());
            item.setBrand(packageLine.getItem() == null ? packageLine.getBrand() : packageLine.getItem().getBrand());
            item.setSpec(packageLine.getItem() == null ? packageLine.getSpec() : packageLine.getItem().getSpec());
            item.setUnit(packageLine.getItem() == null ? packageLine.getUnit() : packageLine.getItem().getUnit());
            item.setQuantity(itemQuantity);
            item.setPrice(money(itemPrice));
            item.setAmount(money(itemAmount));
            item.setCost(money(itemCost));
            item.setComponents(quoteCalcContext.components(
                    com.th4.edge.decoboon.catalog.configure.PricingMethod.FixedTotal,
                    BigDecimal.ONE, itemAmount, BigDecimal.ZERO, itemCost, "套餐模板分摊"));
            item.setSort(index++);
            item.setRole(QuoteLineRole.Base);
            result.add(item);
        }
        return result;
    }

    private BigDecimal effectiveAmount(DesignItem item) {
        return item.getAmount() == null ? BigDecimal.ZERO : item.getAmount();
    }

    private BigDecimal parseQuantity(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ONE;
        }
        String normalized = value.replaceAll("[^0-9.]", "");
        if (normalized.isBlank()) {
            return BigDecimal.ONE;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ignored) {
            return BigDecimal.ONE;
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 报价接受与同一房屋下的合同/立项共用同一把房屋锁，保证同一房屋的报价确认、签约、立项严格串行。
     */
    public String acceptLockKey(Long quoteId) {
        if (quoteId == null) {
            return "quote-accept:unknown";
        }
        Quote quote = quoteRepository.findById(quoteId).orElse(null);
        Property property = quote == null ? null : quote.getProperty();
        if (property == null || property.getPropertyId() == null) {
            // 报价不存在或缺房屋时仍返回非空键，由业务方法按既有口径返回 400/404。
            return "quote-accept:" + quoteId;
        }
        Long comId = property.getCompany() == null ? null : property.getCompany().getComId();
        return "property:" + comId + ":" + property.getPropertyId();
    }
}
