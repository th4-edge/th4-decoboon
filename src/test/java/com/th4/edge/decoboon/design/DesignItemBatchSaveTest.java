package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.design.service.items.batchsave.support.BatchSave;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 设计分项批量保存回归：一次保存整张草稿表，任一行不合法整批拒绝。
 */
class DesignItemBatchSaveTest {

    private final DesignContext designContext = mock(DesignContext.class);
    private final DesignItemRepository itemRepository = mock(DesignItemRepository.class);
    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final CatalogItemRepository catalogRepository = mock(CatalogItemRepository.class);
    private final QuoteLineRepository quoteLineRepository = mock(QuoteLineRepository.class);
    private final QuoteCalcContext quoteCalcContext = mock(QuoteCalcContext.class);
    private final DataScopeContext dataScopeContext = mock(DataScopeContext.class);

    private final BatchSave batchSave = new BatchSave(designContext, itemRepository, spaceRepository,
            catalogRepository, quoteLineRepository, quoteCalcContext, dataScopeContext);

    @Test
    void shouldPersistEveryRowOfOneDraftSheet() {
        Company company = company(1L);
        Employee designer = employee(10L, company);
        Design design = design(100L, company);
        Space space = space(200L, design, company);
        CatalogItem first = catalog(301L, company, "拆除--保温墙铲除", "27.40");
        CatalogItem second = catalog(302L, company, "水电--点位安装", "88.00");

        when(designContext.requireDesign(100L, designer)).thenReturn(design);
        when(spaceRepository.findById(200L)).thenReturn(Optional.of(space));
        when(catalogRepository.findById(301L)).thenReturn(Optional.of(first));
        when(catalogRepository.findById(302L)).thenReturn(Optional.of(second));
        when(quoteCalcContext.calculate(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.save(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(100L)).thenReturn(List.of(space));
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(100L))
                .thenReturn(List.of(new DesignItem(), new DesignItem()));

        ReflectionTestUtils.setField(batchSave, "designId", 100L);
        ReflectionTestUtils.setField(batchSave, "employee", designer);
        ReflectionTestUtils.setField(batchSave, "items", List.of(row(space, first, "2"), row(space, second, "3")));
        Design request = new Design();

        Design result = batchSave.call(request);

        verify(itemRepository, times(2)).save(any(DesignItem.class));
        assertThat(result.getItems()).hasSize(2);
    }

    @Test
    void shouldRejectWholeBatchWithoutWritingWhenAnyRowIsInvalid() {
        Company company = company(1L);
        Company other = company(2L);
        Employee designer = employee(10L, company);
        Design design = design(100L, company);
        Space space = space(200L, design, company);
        CatalogItem valid = catalog(301L, company, "拆除--保温墙铲除", "27.40");
        CatalogItem foreign = catalog(302L, other, "外部企业项目", "88.00");

        when(designContext.requireDesign(100L, designer)).thenReturn(design);
        when(spaceRepository.findById(200L)).thenReturn(Optional.of(space));
        when(catalogRepository.findById(301L)).thenReturn(Optional.of(valid));
        when(catalogRepository.findById(302L)).thenReturn(Optional.of(foreign));

        ReflectionTestUtils.setField(batchSave, "designId", 100L);
        ReflectionTestUtils.setField(batchSave, "employee", designer);
        Design request = new Design();
        ReflectionTestUtils.setField(batchSave, "items", List.of(row(space, valid, "2"), row(space, foreign, "3")));

        assertThatThrownBy(() -> batchSave.call(request))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("材料库项目不属于当前企业");

        verify(itemRepository, never()).save(any(DesignItem.class));
        verify(itemRepository, never()).delete(any(DesignItem.class));
    }

    @Test
    void shouldPersistZeroQuantityGiftRow() {
        Company company = company(1L);
        Employee designer = employee(10L, company);
        Design design = design(100L, company);
        Space space = space(200L, design, company);
        CatalogItem gift = catalog(301L, company, "赠送项目", "100.00");

        when(designContext.requireDesign(100L, designer)).thenReturn(design);
        when(spaceRepository.findById(200L)).thenReturn(Optional.of(space));
        when(catalogRepository.findById(301L)).thenReturn(Optional.of(gift));
        when(quoteCalcContext.calculate(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.save(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(100L)).thenReturn(List.of(space));
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(100L)).thenReturn(List.of(new DesignItem()));

        ReflectionTestUtils.setField(batchSave, "designId", 100L);
        ReflectionTestUtils.setField(batchSave, "employee", designer);
        ReflectionTestUtils.setField(batchSave, "items", List.of(row(space, gift, "0")));

        Design result = batchSave.call(new Design());

        verify(itemRepository, times(1)).save(any(DesignItem.class));
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    void shouldRejectNegativeQuantityWholeBatch() {
        Company company = company(1L);
        Employee designer = employee(10L, company);
        Design design = design(100L, company);
        Space space = space(200L, design, company);
        CatalogItem valid = catalog(301L, company, "拆除--保温墙铲除", "27.40");

        when(designContext.requireDesign(100L, designer)).thenReturn(design);
        when(spaceRepository.findById(200L)).thenReturn(Optional.of(space));
        when(catalogRepository.findById(301L)).thenReturn(Optional.of(valid));

        ReflectionTestUtils.setField(batchSave, "designId", 100L);
        ReflectionTestUtils.setField(batchSave, "employee", designer);
        ReflectionTestUtils.setField(batchSave, "items", List.of(row(space, valid, "-1")));

        assertThatThrownBy(() -> batchSave.call(new Design()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不能为负数");

        verify(itemRepository, never()).save(any(DesignItem.class));
    }

    @Test
    void shouldRefuseDeletingItemReferencedByQuoteSnapshot() {
        Company company = company(1L);
        Employee designer = employee(10L, company);
        Design design = design(100L, company);
        DesignItem referenced = new DesignItem();
        referenced.setItemId(555L);
        referenced.setDesign(design);
        referenced.setCompany(company);

        when(designContext.requireDesign(100L, designer)).thenReturn(design);
        when(dataScopeContext.requireScope(designer))
                .thenReturn(new DataScope(DataScopeType.Self, 1L, null, null, 10L));
        when(itemRepository.findOne(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Optional.of(referenced));
        when(quoteLineRepository.existsByDesignItem_ItemId(555L)).thenReturn(true);

        ReflectionTestUtils.setField(batchSave, "designId", 100L);
        ReflectionTestUtils.setField(batchSave, "employee", designer);
        ReflectionTestUtils.setField(batchSave, "deletedItemIds", List.of(555L));

        assertThatThrownBy(() -> batchSave.call(new Design()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("分项已生成报价快照");

        verify(itemRepository, never()).delete(any(DesignItem.class));
        verify(itemRepository, never()).save(any(DesignItem.class));
    }

    private DesignItem row(Space space, CatalogItem catalog, String quantity) {
        DesignItem item = new DesignItem();
        item.setSpace(space);
        item.setCatalogItem(catalog);
        item.setQuantity(new BigDecimal(quantity));
        return item;
    }

    private Company company(Long id) {
        Company company = new Company();
        company.setComId(id);
        return company;
    }

    private Employee employee(Long id, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setRole(EmployeeRole.Designer);
        employee.setCompany(company);
        return employee;
    }

    private Design design(Long id, Company company) {
        Design design = new Design();
        design.setDesignId(id);
        design.setCompany(company);
        return design;
    }

    private Space space(Long id, Design design, Company company) {
        Space space = new Space();
        space.setSpaceId(id);
        space.setDesign(design);
        space.setCompany(company);
        space.setType(SpaceType.LivingRoom);
        space.setName("全屋");
        return space;
    }

    private CatalogItem catalog(Long id, Company company, String name, String manualPrice) {
        CatalogItem item = new CatalogItem();
        item.setItemId(id);
        item.setCompany(company);
        item.setType(ItemType.Construction);
        item.setCategory("拆除");
        item.setName(name);
        item.setUnit(Unit.Area);
        item.setPricingMode(PricingMethod.ManualOnly);
        item.setMaterialPrice(BigDecimal.ZERO);
        item.setManualPrice(new BigDecimal(manualPrice));
        item.setCostPrice(new BigDecimal(manualPrice));
        item.setActive(Boolean.TRUE);
        return item;
    }
}
