package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.design.context.DesignCostContext;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DesignCostContextTest {

    private final DesignCostContext context = new DesignCostContext();

    @Test
    void onlyBossStoreManagerAndFinanceCanSeeDesignCost() {
        assertThat(context.canViewCost(employee(EmployeeRole.Boss))).isTrue();
        assertThat(context.canViewCost(employee(EmployeeRole.StoreManager))).isTrue();
        assertThat(context.canViewCost(employee(EmployeeRole.Finance))).isTrue();
        assertThat(context.canViewCost(employee(EmployeeRole.Designer))).isFalse();
        assertThat(context.canViewCost(employee(EmployeeRole.Marketer))).isFalse();
        assertThat(context.canViewCost(employee(EmployeeRole.Pm))).isFalse();
    }

    @Test
    void designerResponseMustNotContainCostFields() {
        CatalogItem catalog = new CatalogItem();
        catalog.setCostPrice(new BigDecimal("80.00"));
        DesignItem item = new DesignItem();
        item.setCatalogItem(catalog);
        item.setCostPrice(new BigDecimal("80.00"));
        item.setCost(new BigDecimal("160.00"));

        context.mask(List.of(item), employee(EmployeeRole.Designer));

        assertThat(item.getCostPrice()).isNull();
        assertThat(item.getCost()).isNull();
        assertThat(catalog.getCostPrice()).isNull();
    }

    @Test
    void financeKeepsCostFields() {
        CatalogItem catalog = new CatalogItem();
        catalog.setCostPrice(new BigDecimal("80.00"));
        DesignItem item = new DesignItem();
        item.setCatalogItem(catalog);
        item.setCostPrice(new BigDecimal("80.00"));
        item.setCost(new BigDecimal("160.00"));

        context.mask(List.of(item), employee(EmployeeRole.Finance));

        assertThat(item.getCostPrice()).isEqualByComparingTo("80.00");
        assertThat(item.getCost()).isEqualByComparingTo("160.00");
        assertThat(catalog.getCostPrice()).isEqualByComparingTo("80.00");
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setRole(role);
        return employee;
    }
}
