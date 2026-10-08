package com.th4.edge.decoboon.pack;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.pack.context.PackageCostContext;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PackageCostContextTest {

    @Test
    void masksPackageAndNestedCatalogCostForNonCostRoles() {
        PackageCostContext context = new PackageCostContext();
        CatalogItem item = new CatalogItem();
        item.setCostPrice(new BigDecimal("60.00"));
        PackageLine line = new PackageLine();
        line.setItem(item);
        PackageTemplate template = new PackageTemplate();
        template.setCost(new BigDecimal("80.00"));
        template.setItems(List.of(line));

        context.mask(template, employee(EmployeeRole.Designer));

        assertThat(template.getCost()).isNull();
        assertThat(item.getCostPrice()).isNull();
    }

    @Test
    void keepsPackageCostForFinanceButCatalogCostContextRemainsStricter() {
        PackageCostContext context = new PackageCostContext();
        PackageTemplate template = new PackageTemplate();
        template.setCost(new BigDecimal("80.00"));

        assertThat(context.canViewCost(employee(EmployeeRole.Finance))).isTrue();
        assertThat(context.canViewCost(employee(EmployeeRole.StoreManager))).isTrue();
        assertThat(context.canViewCost(employee(EmployeeRole.Boss))).isTrue();

        context.mask(template, employee(EmployeeRole.Finance));
        assertThat(template.getCost()).isEqualByComparingTo("80.00");
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId((long) role.ordinal() + 1);
        employee.setRole(role);
        return employee;
    }
}
