package com.th4.edge.decoboon.trade.quote;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.context.QuoteAdjustmentContext;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.model.Company;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuoteAdjustmentContextTest {

    private final QuoteRepository quoteRepository = mock(QuoteRepository.class);
    private final QuoteLineRepository lineRepository = mock(QuoteLineRepository.class);
    private final CatalogItemRepository catalogRepository = mock(CatalogItemRepository.class);
    private final QuoteCalcContext calcContext = new QuoteCalcContext();
    private final QuoteAdjustmentContext context = new QuoteAdjustmentContext(
            quoteRepository, lineRepository, catalogRepository, calcContext);

    @Test
    void shouldDeleteBaseLineFromCustomDesignQuote() {
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setStatus(QuoteStatus.Draft);
        quote.setMode(QuoteMode.Custom);
        QuoteLine base = new QuoteLine();
        base.setItemId(2L);
        base.setRole(QuoteLineRole.Base);
        base.setIncluded(true);
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(base));

        context.delete(quote, 2L);

        verify(lineRepository).delete(base);
        verify(quoteRepository).save(quote);
    }

    @Test
    void shouldProtectBaseLineFromPackageQuote() {
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setStatus(QuoteStatus.Draft);
        quote.setMode(QuoteMode.Package);
        QuoteLine base = new QuoteLine();
        base.setItemId(2L);
        base.setRole(QuoteLineRole.Base);
        base.setIncluded(true);
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(base));

        assertThatThrownBy(() -> context.delete(quote, 2L))
                .hasMessageContaining("基础套餐项目不能直接删除");
    }

    @Test
    void shouldDeductBasePackageLineAndRecalculateQuote() {
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setStatus(QuoteStatus.Draft);
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxRate(new BigDecimal("0.05"));
        PackageLine packageLine = new PackageLine();
        packageLine.setDeductible(true);
        QuoteLine base = new QuoteLine();
        base.setItemId(2L);
        base.setPackageLine(packageLine);
        base.setIncluded(true);
        base.setAmount(new BigDecimal("1000.00"));
        base.setCost(new BigDecimal("600.00"));
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(base));
        when(lineRepository.save(base)).thenReturn(base);

        QuoteLine result = context.save(quote, new QuoteLine(), true, 2L, null, null);

        assertThat(result.getIncluded()).isFalse();
        assertThat(result.getAmount()).isEqualByComparingTo("0");
        assertThat(result.getCost()).isEqualByComparingTo("0");
        assertThat(quote.getSubtotal()).isEqualByComparingTo("0");
        assertThat(quote.getAmount()).isEqualByComparingTo("0");
        assertThat(quote.getCost()).isEqualByComparingTo("0");
        verify(quoteRepository).save(quote);
    }

    /**
     * ECD-013 回归：请求未携带 space 时，必须保留既有行的空间归属。
     *
     * 背景：App 与 Web 单行入口的入参都不含 space，原实现无条件覆盖为 null，
     * 会把报价行从原区域挪走，破坏区域分组与区域小计。
     */
    @Test
    void shouldCopyIncludedLineAsNewAddAndRecalculateQuote() {
        Quote quote = draftQuoteWithCompany();
        QuoteLine source = existingLineWithSpace(5L, 77L);
        source.setRole(QuoteLineRole.Base);
        source.setType(com.th4.edge.decoboon.catalog.configure.ItemType.Construction);
        source.setCategory("客厅");
        source.setPackageSectionNameSnapshot("客厅区域");
        source.setName("墙地砖铺贴");
        source.setBrand("测试品牌");
        source.setSpec("800x800");
        source.setUnit(Unit.Area);
        source.setQuantity(new BigDecimal("2"));
        source.setMaterialPrice(new BigDecimal("10.00"));
        source.setManualPrice(new BigDecimal("5.00"));
        source.setPricingMode(com.th4.edge.decoboon.catalog.configure.PricingMethod.MaterialAndManual);
        source.setPrice(new BigDecimal("30.00"));
        source.setAmount(new BigDecimal("60.00"));
        source.setCost(new BigDecimal("24.00"));
        source.setSort(5);
        QuoteComponent component = new QuoteComponent();
        component.setName("材料");
        component.setSaleAmount(new BigDecimal("20.00"));
        component.setCostAmount(new BigDecimal("8.00"));
        source.setComponents(List.of(component));
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(source));
        when(lineRepository.save(any(QuoteLine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuoteLine copied = context.copy(quote, 5L);

        assertThat(copied.getItemId()).isNull();
        assertThat(copied.getRole()).isEqualTo(QuoteLineRole.Add);
        assertThat(copied.getDifferenceType()).isEqualTo(com.th4.edge.decoboon.quote.configure.QuoteDifferenceType.DesignOnly);
        assertThat(copied.getDifferenceStatus()).isEqualTo(com.th4.edge.decoboon.quote.configure.QuoteDifferenceStatus.Confirmed);
        assertThat(copied.getIncluded()).isTrue();
        assertThat(copied.getName()).isEqualTo(source.getName());
        assertThat(copied.getPackageSectionNameSnapshot()).isEqualTo("客厅区域");
        assertThat(copied.getQuantity()).isEqualByComparingTo("2");
        assertThat(copied.getAmount()).isEqualByComparingTo("60.00");
        assertThat(copied.getCost()).isEqualByComparingTo("24.00");
        assertThat(copied.getSort()).isEqualTo(6);
        assertThat(copied.getComponents()).hasSize(1);
        assertThat(copied.getComponents().get(0)).isNotSameAs(component);
        verify(quoteRepository).save(quote);
    }

    @Test
    void shouldPreserveExistingSpaceWhenRequestOmitsSpace() {
        Quote quote = draftQuoteWithCompany();
        QuoteLine existing = existingLineWithSpace(5L, 77L);
        CatalogItem catalog = materialAndManualCatalog();

        when(lineRepository.findByQuote(quote)).thenReturn(List.of(existing));
        when(catalogRepository.findByItemIdAndCompany_ComId(100L, 9L)).thenReturn(Optional.of(catalog));
        when(lineRepository.save(existing)).thenReturn(existing);

        QuoteLine request = new QuoteLine();
        request.setItemId(5L);
        // 关键：request 不带 space（模拟 App / Web 单行入口的既有入参）

        context.save(quote, request, false, null, 100L, new BigDecimal("2"));

        assertThat(existing.getSpace()).isNotNull();
        assertThat(existing.getSpace().getSpaceId()).isEqualTo(77L);
    }

    /** 显式提供 space 时，仍应正常更新（缺省即保留，不等于永远不改）。 */
    @Test
    void shouldApplySpaceWhenRequestProvidesIt() {
        Quote quote = draftQuoteWithCompany();
        QuoteLine existing = existingLineWithSpace(5L, 77L);
        CatalogItem catalog = materialAndManualCatalog();

        when(lineRepository.findByQuote(quote)).thenReturn(List.of(existing));
        when(catalogRepository.findByItemIdAndCompany_ComId(100L, 9L)).thenReturn(Optional.of(catalog));
        when(lineRepository.save(existing)).thenReturn(existing);

        Space target = new Space();
        target.setSpaceId(88L);
        QuoteLine request = new QuoteLine();
        request.setItemId(5L);
        request.setSpace(target);

        context.save(quote, request, false, null, 100L, new BigDecimal("2"));

        assertThat(existing.getSpace()).isNotNull();
        assertThat(existing.getSpace().getSpaceId()).isEqualTo(88L);
    }

    private Quote draftQuoteWithCompany() {
        Company company = new Company();
        company.setComId(9L);
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setStatus(QuoteStatus.Draft);
        quote.setCompany(company);
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxRate(new BigDecimal("0.05"));
        return quote;
    }

    private QuoteLine existingLineWithSpace(Long itemId, Long spaceId) {
        Space space = new Space();
        space.setSpaceId(spaceId);
        QuoteLine line = new QuoteLine();
        line.setItemId(itemId);
        line.setSpace(space);
        line.setIncluded(true);
        line.setAmount(new BigDecimal("100.00"));
        line.setCost(new BigDecimal("50.00"));
        return line;
    }

    private CatalogItem materialAndManualCatalog() {
        CatalogItem catalog = mock(CatalogItem.class);
        when(catalog.getActive()).thenReturn(Boolean.TRUE);
        when(catalog.getPricingMode()).thenReturn(PricingMethod.MaterialAndManual);
        when(catalog.getName()).thenReturn("测试项目");
        when(catalog.getMaterialPrice()).thenReturn(new BigDecimal("10.00"));
        when(catalog.getManualPrice()).thenReturn(new BigDecimal("5.00"));
        when(catalog.getCostPrice()).thenReturn(new BigDecimal("4.00"));
        return catalog;
    }
}
