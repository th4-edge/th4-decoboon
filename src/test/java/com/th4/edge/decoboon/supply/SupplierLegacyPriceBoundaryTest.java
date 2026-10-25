package com.th4.edge.decoboon.supply;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.project.context.MaterialPlanContext;
import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.model.MaterialSupplier;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.decoboon.supply.repository.MaterialSupplierRepository;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** EOP-D7a：供应商旧采购价只读兼容，不能覆盖成本价或供应关系保存。 */
class SupplierLegacyPriceBoundaryTest {

    @Test
    void partnerPriceSaveOnlyMaintainsLaborPriceAndPreservesLegacyPurchasePrice() {
        TenantContext tenants = mock(TenantContext.class);
        PartnerRepository partners = mock(PartnerRepository.class);
        CatalogItemRepository items = mock(CatalogItemRepository.class);
        PartnerPriceRepository prices = mock(PartnerPriceRepository.class);
        ModelIdentityContext identities = mock(ModelIdentityContext.class);
        var save = new com.th4.edge.decoboon.supply.service.partnerprice.save.support.Save(
                tenants, partners, items, prices, identities);

        Employee employee = employee(EmployeeRole.Finance);
        Company company = employee.getCompany();
        Partner partner = partner(company);
        CatalogItem item = item(company);
        PartnerPrice legacy = legacyPrice(company, partner, item);

        ReflectionTestUtils.setField(save, "employee", employee);
        ReflectionTestUtils.setField(save, "partnerPriceId", 30L);
        ReflectionTestUtils.setField(save, "partnerId", 10L);
        ReflectionTestUtils.setField(save, "itemId", 20L);
        ReflectionTestUtils.setField(save, "laborPrice", new BigDecimal("12.00"));

        when(tenants.requireRole(eq(employee), eq(EmployeeRole.Boss),
                eq(EmployeeRole.StoreManager), eq(EmployeeRole.Finance))).thenReturn(employee);
        when(partners.findById(10L)).thenReturn(Optional.of(partner));
        when(items.findById(20L)).thenReturn(Optional.of(item));
        when(prices.findById(30L)).thenReturn(Optional.of(legacy));
        when(identities.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(prices.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PartnerPrice saved = save.call(new PartnerPrice());

        assertThat(saved.getPurchasePrice()).isEqualByComparingTo("68.00");
        assertThat(saved.getLaborPrice()).isEqualByComparingTo("12.00");
        verify(items, never()).save(any(CatalogItem.class));
    }

    @Test
    void partnerPriceSaveRejectsPartnerFromAnotherCompany() {
        TenantContext tenants = mock(TenantContext.class);
        PartnerRepository partners = mock(PartnerRepository.class);
        CatalogItemRepository items = mock(CatalogItemRepository.class);
        PartnerPriceRepository prices = mock(PartnerPriceRepository.class);
        ModelIdentityContext identities = mock(ModelIdentityContext.class);
        var save = new com.th4.edge.decoboon.supply.service.partnerprice.save.support.Save(
                tenants, partners, items, prices, identities);

        Employee employee = employee(EmployeeRole.Finance);
        Partner foreignPartner = partner(company2());

        ReflectionTestUtils.setField(save, "employee", employee);
        ReflectionTestUtils.setField(save, "partnerId", 10L);
        ReflectionTestUtils.setField(save, "itemId", 20L);
        ReflectionTestUtils.setField(save, "laborPrice", new BigDecimal("12.00"));
        when(tenants.requireRole(eq(employee), eq(EmployeeRole.Boss),
                eq(EmployeeRole.StoreManager), eq(EmployeeRole.Finance))).thenReturn(employee);
        when(partners.findById(10L)).thenReturn(Optional.of(foreignPartner));

        assertThatThrownBy(() -> save.call(new PartnerPrice()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("合作方不存在");
        verify(prices, never()).save(any());
    }

    @Test
    void materialSupplierSavePreservesLegacyPurchasePrice() {
        TenantContext tenants = mock(TenantContext.class);
        PartnerRepository partners = mock(PartnerRepository.class);
        CatalogItemRepository items = mock(CatalogItemRepository.class);
        MaterialSupplierRepository relations = mock(MaterialSupplierRepository.class);
        MaterialPlanContext materialPlans = mock(MaterialPlanContext.class);
        ModelIdentityContext identities = mock(ModelIdentityContext.class);
        var save = new com.th4.edge.decoboon.supply.service.materialsupplier.save.support.Save(
                tenants, partners, items, relations, materialPlans, identities);

        Employee employee = employee(EmployeeRole.Finance);
        Company company = employee.getCompany();
        Partner partner = partner(company);
        CatalogItem item = item(company);
        MaterialSupplier relation = new MaterialSupplier();
        relation.setCompany(company);
        relation.setPartner(partner);
        relation.setCatalogItem(item);
        relation.setMaterialKey("ci:20|brand:测试|unit:Item");
        relation.setPurchasePrice(new BigDecimal("68.00"));

        ReflectionTestUtils.setField(save, "employee", employee);
        ReflectionTestUtils.setField(save, "partnerId", 10L);
        ReflectionTestUtils.setField(save, "catalogItemId", 20L);
        ReflectionTestUtils.setField(save, "brand", "测试");
        ReflectionTestUtils.setField(save, "unit", Unit.Item);
        ReflectionTestUtils.setField(save, "minimumOrder", new BigDecimal("5.000"));

        when(tenants.requireRole(eq(employee), eq(EmployeeRole.Boss),
                eq(EmployeeRole.StoreManager), eq(EmployeeRole.Finance))).thenReturn(employee);
        when(partners.findById(10L)).thenReturn(Optional.of(partner));
        when(items.findById(20L)).thenReturn(Optional.of(item));
        when(materialPlans.materialKey(20L, "测试", null, null, Unit.Item))
                .thenReturn("ci:20|brand:测试|unit:Item");
        when(relations.findFirstByCompany_ComIdAndMaterialKeyAndPartner_PartnerId(
                1L, "ci:20|brand:测试|unit:Item", 10L)).thenReturn(Optional.of(relation));
        when(identities.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(relations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MaterialSupplier saved = save.call(new MaterialSupplier());

        assertThat(saved.getPurchasePrice()).isEqualByComparingTo("68.00");
        assertThat(saved.getMinimumOrder()).isEqualByComparingTo("5.000");
    }

    @Test
    void materialSupplierSaveRejectsPartnerFromAnotherCompany() {
        TenantContext tenants = mock(TenantContext.class);
        PartnerRepository partners = mock(PartnerRepository.class);
        CatalogItemRepository items = mock(CatalogItemRepository.class);
        MaterialSupplierRepository relations = mock(MaterialSupplierRepository.class);
        MaterialPlanContext materialPlans = mock(MaterialPlanContext.class);
        ModelIdentityContext identities = mock(ModelIdentityContext.class);
        var save = new com.th4.edge.decoboon.supply.service.materialsupplier.save.support.Save(
                tenants, partners, items, relations, materialPlans, identities);

        Employee employee = employee(EmployeeRole.Finance);
        Partner foreignPartner = partner(company2());

        ReflectionTestUtils.setField(save, "employee", employee);
        ReflectionTestUtils.setField(save, "partnerId", 10L);
        ReflectionTestUtils.setField(save, "catalogItemId", 20L);
        ReflectionTestUtils.setField(save, "brand", "测试");
        ReflectionTestUtils.setField(save, "unit", Unit.Item);
        when(tenants.requireRole(eq(employee), eq(EmployeeRole.Boss),
                eq(EmployeeRole.StoreManager), eq(EmployeeRole.Finance))).thenReturn(employee);
        when(partners.findById(10L)).thenReturn(Optional.of(foreignPartner));

        assertThatThrownBy(() -> save.call(new MaterialSupplier()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("供应商不存在");
        verify(relations, never()).save(any());
    }

    private Employee employee(EmployeeRole role) {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setRole(role);
        employee.setCompany(company);
        return employee;
    }

    private Company company2() {
        Company company = new Company();
        company.setComId(2L);
        return company;
    }

    private Partner partner(Company company) {
        Partner partner = new Partner();
        partner.setPartnerId(10L);
        partner.setCompany(company);
        partner.setStatus(PartnerStatus.Active);
        return partner;
    }

    private CatalogItem item(Company company) {
        CatalogItem item = new CatalogItem();
        item.setItemId(20L);
        item.setCompany(company);
        return item;
    }

    private PartnerPrice legacyPrice(Company company, Partner partner, CatalogItem item) {
        PartnerPrice price = new PartnerPrice();
        price.setPartnerPriceId(30L);
        price.setCompany(company);
        price.setPartner(partner);
        price.setItem(item);
        price.setPurchasePrice(new BigDecimal("68.00"));
        price.setLaborPrice(new BigDecimal("10.00"));
        return price;
    }
}
