package com.th4.edge.decoboon.trade.quote;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.pack.context.PackageContext;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceStatus;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceType;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.context.HybridQuoteBuilder;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.decoboon.common.access.StoreAttributionTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HybridQuoteBuilderTest {

    private final QuoteRepository quoteRepository = mock(QuoteRepository.class);
    private final QuoteLineRepository lineRepository = mock(QuoteLineRepository.class);
    private final PackageLineRepository packageLineRepository = mock(PackageLineRepository.class);
    private final PackageContext packageContext = mock(PackageContext.class);
    private final QuoteCalcContext calcContext = new QuoteCalcContext();
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final DesignItemRepository designItemRepository = mock(DesignItemRepository.class);
    private final HybridQuoteBuilder builder = StoreAttributionTestSupport.inject(new HybridQuoteBuilder(
            quoteRepository, lineRepository, packageLineRepository, packageContext,
            calcContext, projectRepository, designItemRepository));

    @Test
    void shouldBuildHybridQuoteAndRecordDesignDifferences() {
        Company company = new Company();
        company.setComId(1L);
        Employee owner = new Employee();
        owner.setEmpId(9L);
        Customer customer = new Customer();
        customer.setCustId(2L);
        Property property = new Property();
        property.setPropertyId(3L);
        Design design = design(company, customer, property, owner);
        PackageTemplate template = new PackageTemplate();
        template.setTemplateId(4L);
        template.setCompany(company);
        template.setName("测试套餐");
        template.setArea(new BigDecimal("118"));
        template.setPrice(new BigDecimal("100000"));
        template.setDiscount(BigDecimal.ONE);

        CatalogItem catalog = catalog(11L, "瓷砖", "瓷砖A");
        PackageSection section = new PackageSection();
        section.setGroupId(12L);
        section.setName("客餐厅");
        PackageLine matchedPackageLine = packageLine(21L, template, section, catalog, "瓷砖A", "10");
        PackageLine packageOnlyLine = packageLine(22L, template, section, catalog(13L, "木门", "木门A"), "木门A", "2");
        DesignItem matched = designItem(31L, catalog, "瓷砖A", "10", "500");
        DesignItem designOnly = designItem(32L, catalog(14L, "软装", "窗帘A"), "窗帘A", "1", "300");

        when(packageLineRepository.findByTemplate_TemplateIdOrderBySortAscItemIdAsc(4L))
                .thenReturn(List.of(matchedPackageLine, packageOnlyLine));
        when(packageContext.calculateItemCost(matchedPackageLine)).thenReturn(new BigDecimal("3000"));
        when(packageContext.calculateItemCost(packageOnlyLine)).thenReturn(new BigDecimal("2000"));
        when(designItemRepository.findByDesign_DesignIdOrderBySortAsc(8L)).thenReturn(List.of(matched, designOnly));
        when(quoteRepository.countByDesign_DesignId(8L)).thenReturn(0L);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> {
            Quote quote = invocation.getArgument(0);
            quote.setQuoteId(99L);
            return quote;
        });
        when(lineRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Quote quote = builder.build(design, template, BigDecimal.ONE);

        assertThat(quote.getMode()).isEqualTo(QuoteMode.Hybrid);
        assertThat(quote.getDesign()).isSameAs(design);
        assertThat(quote.getPackageTemplate()).isSameAs(template);
        assertThat(quote.getProperty()).isSameAs(property);
        ArgumentCaptor<List<QuoteLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(lineRepository).saveAll(captor.capture());
        List<QuoteLine> lines = captor.getValue();
        assertThat(lines).hasSize(3);
        assertThat(lines).extracting(QuoteLine::getDifferenceType)
                .contains(QuoteDifferenceType.Matched, QuoteDifferenceType.PackageOnly, QuoteDifferenceType.DesignOnly);
        assertThat(lines).filteredOn(line -> line.getDifferenceType() == QuoteDifferenceType.PackageOnly)
                .allMatch(line -> line.getDifferenceStatus() == QuoteDifferenceStatus.Pending);
    }

    @Test
    void shouldRejectPackageFromDifferentCompany() {
        Company designCompany = new Company();
        designCompany.setComId(1L);
        Company packageCompany = new Company();
        packageCompany.setComId(2L);
        Employee owner = new Employee();
        owner.setEmpId(9L);
        Customer customer = new Customer();
        customer.setCustId(2L);
        Property property = new Property();
        property.setPropertyId(3L);
        Design design = design(designCompany, customer, property, owner);
        PackageTemplate template = new PackageTemplate();
        template.setTemplateId(4L);
        template.setCompany(packageCompany);
        template.setArea(new BigDecimal("118"));
        template.setPrice(new BigDecimal("100000"));

        assertThatThrownBy(() -> builder.build(design, template, BigDecimal.ONE))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("不属于同一企业");
    }

    @Test
    void shouldMarkCostIncompleteWhenHybridCostIsZero() {
        Company company = new Company();
        company.setComId(1L);
        Employee owner = new Employee();
        owner.setEmpId(9L);
        Customer customer = new Customer();
        customer.setCustId(2L);
        Property property = new Property();
        property.setPropertyId(3L);
        Design design = design(company, customer, property, owner);
        PackageTemplate template = new PackageTemplate();
        template.setTemplateId(4L);
        template.setCompany(company);
        template.setArea(new BigDecimal("118"));
        template.setPrice(new BigDecimal("100000"));
        CatalogItem catalog = catalog(11L, "瓷砖", "瓷砖A");
        PackageLine packageLine = packageLine(21L, template, new PackageSection(), catalog, "瓷砖A", "1");
        DesignItem designItem = designItem(31L, catalog, "瓷砖A", "1", "100");
        designItem.setCost(BigDecimal.ZERO);
        when(packageLineRepository.findByTemplate_TemplateIdOrderBySortAscItemIdAsc(4L)).thenReturn(List.of(packageLine));
        when(packageContext.calculateItemCost(packageLine)).thenReturn(BigDecimal.ZERO);
        when(designItemRepository.findByDesign_DesignIdOrderBySortAsc(8L)).thenReturn(List.of(designItem));
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lineRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Quote quote = builder.build(design, template, BigDecimal.ONE);

        assertThat(quote.getCost()).isEqualByComparingTo("0");
        assertThat(quote.getCostComplete()).isFalse();
    }

    private Design design(Company company, Customer customer, Property property, Employee owner) {
        Design design = new Design();
        design.setDesignId(8L);
        design.setName("现代简约方案");
        design.setStatus(DesignStatus.Ready);
        design.setArea(new BigDecimal("118"));
        design.setCompany(company);
        design.setCustomer(customer);
        design.setProperty(property);
        design.setOwner(owner);
        return design;
    }

    private CatalogItem catalog(Long id, String category, String name) {
        CatalogItem item = new CatalogItem();
        item.setItemId(id);
        item.setType(ItemType.Material);
        item.setCategory(category);
        item.setName(name);
        item.setUnit(com.th4.edge.decoboon.bim.configure.Unit.Area);
        item.setPricingMode(com.th4.edge.decoboon.catalog.configure.PricingMethod.MaterialOnly);
        item.setMaterialPrice(new BigDecimal("100"));
        item.setManualPrice(BigDecimal.ZERO);
        item.setCostPrice(new BigDecimal("60"));
        return item;
    }

    private PackageLine packageLine(Long id, PackageTemplate template, PackageSection section,
                                    CatalogItem catalog, String name, String factor) {
        PackageLine line = new PackageLine();
        line.setItemId(id);
        line.setTemplate(template);
        line.setSection(section);
        line.setItem(catalog);
        line.setName(name);
        line.setUnit(catalog.getUnit());
        line.setQuantityFactor(new BigDecimal(factor));
        line.setPricingMethod(catalog.getPricingMode());
        return line;
    }

    private DesignItem designItem(Long id, CatalogItem catalog, String name, String quantity, String amount) {
        DesignItem item = new DesignItem();
        item.setItemId(id);
        item.setCatalogItem(catalog);
        item.setName(name);
        item.setType(catalog.getType());
        item.setCategory(catalog.getCategory());
        item.setUnit(catalog.getUnit());
        item.setPricingMode(catalog.getPricingMode());
        item.setQuantity(new BigDecimal(quantity));
        item.setMaterialPrice(catalog.getMaterialPrice());
        item.setManualPrice(catalog.getManualPrice());
        item.setCostPrice(catalog.getCostPrice());
        item.setAmount(new BigDecimal(amount));
        item.setCost(new BigDecimal("60"));
        item.setSort(0);
        return item;
    }
}
