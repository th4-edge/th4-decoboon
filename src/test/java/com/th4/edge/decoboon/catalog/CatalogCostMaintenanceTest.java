package com.th4.edge.decoboon.catalog;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.catalog.context.CatalogDetailContext;
import com.th4.edge.decoboon.catalog.context.PinyinContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
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
import static org.mockito.Mockito.when;

class CatalogCostMaintenanceTest {

    @Test
    void catalogCostIsVisibleOnlyToBossAndStoreManager() {
        CatalogCostContext context = new CatalogCostContext();

        assertThat(context.canViewCost(employee(EmployeeRole.Boss))).isTrue();
        assertThat(context.canViewCost(employee(EmployeeRole.StoreManager))).isTrue();
        assertThat(context.canViewCost(employee(EmployeeRole.Finance))).isFalse();
        assertThat(context.canViewCost(employee(EmployeeRole.Designer))).isFalse();

        CatalogItem financeView = new CatalogItem();
        financeView.setCostPrice(new BigDecimal("88.00"));
        context.mask(financeView, employee(EmployeeRole.Finance));
        assertThat(financeView.getCostPrice()).isNull();

        CatalogItem managerView = new CatalogItem();
        managerView.setCostPrice(new BigDecimal("88.00"));
        context.mask(managerView, employee(EmployeeRole.StoreManager));
        assertThat(managerView.getCostPrice()).isEqualByComparingTo("88.00");
    }

    @Test
    void catalogWriteRejectsFinanceAndAllowsStoreManager() {
        DataScopeContext scope = mock(DataScopeContext.class);
        CatalogContext context = new CatalogContext(scope);
        Employee finance = employee(EmployeeRole.Finance);
        Employee storeManager = employee(EmployeeRole.StoreManager);
        when(scope.requireEmployee(finance)).thenReturn(finance);
        when(scope.requireEmployee(storeManager)).thenReturn(storeManager);

        assertThat(context.requireMaintainer(storeManager)).isSameAs(storeManager);
        assertThatThrownBy(() -> context.requireMaintainer(finance))
                .hasMessageContaining("无权维护报价项目库");
    }

    @Test
    void storeManagerCanUpdateCatalogCostDirectly() {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(10L);
        employee.setRole(EmployeeRole.StoreManager);
        employee.setCompany(company);

        CatalogItem item = new CatalogItem();
        item.setItemId(20L);
        item.setCompany(company);
        item.setCostPrice(new BigDecimal("80.00"));

        CatalogItemRepository repository = mock(CatalogItemRepository.class);
        CatalogCostContext costContext = mock(CatalogCostContext.class);
        PinyinContext pinyinContext = new PinyinContext();
        CatalogContext catalogContext = mock(CatalogContext.class);
        CatalogDetailContext detailContext = mock(CatalogDetailContext.class);

        when(catalogContext.requireMaintainer(employee)).thenReturn(employee);
        when(catalogContext.formula(PricingMethod.MaterialOnly)).thenReturn("材料单价 × 工程量");
        when(repository.findByItemIdAndCompany_ComId(20L, 1L)).thenReturn(Optional.of(item));
        when(repository.save(any(CatalogItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(costContext.mask(any(CatalogItem.class), eq(employee))).thenAnswer(invocation -> invocation.getArgument(0));

        com.th4.edge.decoboon.catalog.service.items.update.support.Update update =
                new com.th4.edge.decoboon.catalog.service.items.update.support.Update(
                        repository, costContext, pinyinContext, catalogContext, detailContext);

        ReflectionTestUtils.setField(update, "itemId", 20L);
        ReflectionTestUtils.setField(update, "type", ItemType.Material);
        ReflectionTestUtils.setField(update, "code", "ZC-0001");
        ReflectionTestUtils.setField(update, "category", "瓷砖");
        ReflectionTestUtils.setField(update, "brand", "马可波罗");
        ReflectionTestUtils.setField(update, "name", "马可波罗地砖");
        ReflectionTestUtils.setField(update, "unit", Unit.Area);
        ReflectionTestUtils.setField(update, "pricingMode", PricingMethod.MaterialOnly);
        ReflectionTestUtils.setField(update, "materialPrice", new BigDecimal("120.00"));
        ReflectionTestUtils.setField(update, "manualPrice", BigDecimal.ZERO);
        ReflectionTestUtils.setField(update, "costPrice", new BigDecimal("88.00"));
        ReflectionTestUtils.setField(update, "active", true);
        ReflectionTestUtils.setField(update, "sort", 0);
        ReflectionTestUtils.setField(update, "employee", employee);

        CatalogItem saved = update.call(new CatalogItem());

        assertThat(saved.getCostPrice()).isEqualByComparingTo("88.00");
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId((long) role.ordinal() + 1);
        employee.setRole(role);
        return employee;
    }
}
