package com.th4.edge.decoboon.catalog.context;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 成本价可见性控制。
 *
 * <p>成本价与采购价是中小装企的核心敏感配置：仅企业负责人、门店店长与财务可见。
 * 其余角色（设计师、家装顾问、项目经理、履约专员等）的响应中**直接不返回**该字段，
 * 不依赖前端隐藏。</p>
 */
@Component
public class CatalogCostContext {

    private static final Set<EmployeeRole> COST_VISIBLE = EnumSet.of(
            EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Finance);

    /** 当前员工是否可见成本价。 */
    public boolean canViewCost(Employee employee) {
        return employee != null && employee.getRole() != null && COST_VISIBLE.contains(employee.getRole());
    }

    /** 无权限时置空成本价；项目已配置 non_null 序列化策略，空值字段不会出现在响应中。 */
    public CatalogItem mask(CatalogItem item, Employee employee) {
        if (item != null && !canViewCost(employee)) {
            item.setCostPrice(null);
        }
        return item;
    }

    /** 分页整体裁剪。 */
    public Page<CatalogItem> mask(Page<CatalogItem> page, Employee employee) {
        return page == null ? null : page.map(item -> mask(item, employee));
    }
}
