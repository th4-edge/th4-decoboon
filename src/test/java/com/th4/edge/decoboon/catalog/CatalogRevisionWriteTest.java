package com.th4.edge.decoboon.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.CatalogRevisionType;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.catalog.context.CatalogDetailContext;
import com.th4.edge.decoboon.catalog.context.CatalogMutationContext;
import com.th4.edge.decoboon.catalog.context.CatalogRevisionContext;
import com.th4.edge.decoboon.catalog.context.PinyinContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.model.CatalogItemRevision;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRevisionRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogRevisionWriteTest {

    private final CatalogItemRepository itemRepository = mock(CatalogItemRepository.class);
    private final CatalogItemRevisionRepository revisionRepository = mock(CatalogItemRevisionRepository.class);
    private final CatalogCostContext costContext = mock(CatalogCostContext.class);
    private final CatalogContext catalogContext = mock(CatalogContext.class);
    private final CatalogDetailContext detailContext = mock(CatalogDetailContext.class);
    private final CatalogMutationContext mutationContext = mock(CatalogMutationContext.class);

    private Company company;
    private Employee finance;
    private CatalogItem item;
    private CatalogRevisionContext revisionContext;
    private com.th4.edge.decoboon.catalog.service.items.update.support.Update update;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setComId(1L);
        finance = new Employee();
        finance.setEmpId(9L);
        finance.setRole(EmployeeRole.Finance);
        finance.setCompany(company);

        item = new CatalogItem();
        item.setItemId(20L);
        item.setCompany(company);
        item.setType(ItemType.Material);
        item.setCode("ZC-1");
        item.setCategory("瓷砖");
        item.setName("墙砖");
        item.setUnit(Unit.Area);
        item.setPricingMode(PricingMethod.MaterialOnly);
        item.setMaterialPrice(new BigDecimal("100"));
        item.setManualPrice(BigDecimal.ZERO);
        item.setCostPrice(new BigDecimal("70"));
        item.setFormula("材料单价 × 工程量");
        item.setImages(List.of());
        item.setActive(true);
        item.setSort(0);
        item.setRevision(3);

        revisionContext = new CatalogRevisionContext(revisionRepository, new ObjectMapper());
        when(revisionRepository.save(any(CatalogItemRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findByItemIdAndCompany_ComId(20L, 1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(CatalogItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(catalogContext.requirePriceMaintainer(finance)).thenReturn(finance);
        when(catalogContext.formula(PricingMethod.MaterialOnly)).thenReturn("材料单价 × 工程量");
        when(costContext.mask(any(CatalogItem.class), eq(finance))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mutationContext.execute(eq(1L), eq(20L), org.mockito.ArgumentMatchers.<Supplier<CatalogItem>>any()))
                .thenAnswer(invocation -> invocation.<Supplier<CatalogItem>>getArgument(2).get());

        update = new com.th4.edge.decoboon.catalog.service.items.update.support.Update(
                itemRepository, costContext, new PinyinContext(), catalogContext, detailContext,
                revisionContext, mutationContext);
        bindRequest(3, new BigDecimal("100"));
        ReflectionTestUtils.setField(update, "employee", finance);
    }

    @Test
    void unchangedSaveDoesNotIncrementOrWriteAudit() {
        CatalogItem saved = update.call(new CatalogItem());

        assertThat(saved.getRevision()).isEqualTo(3);
        verify(revisionRepository, never()).save(any(CatalogItemRevision.class));
    }

    @Test
    void effectivePriceChangeIncrementsRevisionAndWritesAudit() {
        bindRequest(3, new BigDecimal("120"));

        CatalogItem saved = update.call(new CatalogItem());

        assertThat(saved.getRevision()).isEqualTo(4);
        verify(revisionRepository).save(org.mockito.ArgumentMatchers.argThat(audit ->
                audit.getRevision() == 4
                        && audit.getChangeType() == CatalogRevisionType.Update
                        && audit.getSnapshotJson().contains("\"materialPrice\":120")));
    }

    @Test
    void staleExpectedRevisionReturnsConflictWithoutAudit() {
        bindRequest(2, new BigDecimal("120"));

        assertThatThrownBy(() -> update.call(new CatalogItem()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("已被其他人更新");
        verify(revisionRepository, never()).save(any(CatalogItemRevision.class));
    }

    private void bindRequest(int expectedRevision, BigDecimal materialPrice) {
        ReflectionTestUtils.setField(update, "itemId", 20L);
        ReflectionTestUtils.setField(update, "type", ItemType.Material);
        ReflectionTestUtils.setField(update, "code", "ZC-1");
        ReflectionTestUtils.setField(update, "category", "瓷砖");
        ReflectionTestUtils.setField(update, "brand", null);
        ReflectionTestUtils.setField(update, "series", null);
        ReflectionTestUtils.setField(update, "spec", null);
        ReflectionTestUtils.setField(update, "name", "墙砖");
        ReflectionTestUtils.setField(update, "unit", Unit.Area);
        ReflectionTestUtils.setField(update, "pricingMode", PricingMethod.MaterialOnly);
        ReflectionTestUtils.setField(update, "materialPrice", materialPrice);
        ReflectionTestUtils.setField(update, "manualPrice", BigDecimal.ZERO);
        ReflectionTestUtils.setField(update, "fixedTotalPrice", null);
        ReflectionTestUtils.setField(update, "costPrice", new BigDecimal("70"));
        ReflectionTestUtils.setField(update, "craft", null);
        ReflectionTestUtils.setField(update, "criterion", null);
        ReflectionTestUtils.setField(update, "description", null);
        ReflectionTestUtils.setField(update, "images", List.of());
        ReflectionTestUtils.setField(update, "active", true);
        ReflectionTestUtils.setField(update, "sort", 0);
        ReflectionTestUtils.setField(update, "revision", expectedRevision);
    }
}
