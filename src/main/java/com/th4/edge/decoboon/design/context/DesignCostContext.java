package com.th4.edge.decoboon.design.context;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** 设计分项成本与毛利字段的角色裁剪。 */
@Note("设计成本可见性")
@Component
public class DesignCostContext {

    private static final Set<EmployeeRole> COST_VISIBLE = EnumSet.of(
            EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Finance);

    public boolean canViewCost(Employee employee) {
        return employee != null && employee.getRole() != null && COST_VISIBLE.contains(employee.getRole());
    }

    public void mask(List<DesignItem> items, Employee employee) {
        if (items == null || canViewCost(employee)) return;
        for (DesignItem item : items) {
            if (item == null) continue;
            item.setCost(null);
            item.setCostPrice(null);
            CatalogItem catalogItem = item.getCatalogItem();
            if (catalogItem != null) catalogItem.setCostPrice(null);
        }
    }
}
