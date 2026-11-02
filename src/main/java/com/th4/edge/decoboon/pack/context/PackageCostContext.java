package com.th4.edge.decoboon.pack.context;

import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 套餐模板成本展示控制。
 *
 * <p>企业负责人、店长和财务可查看套餐成本与参考毛利；设计师、业务员、项目经理等
 * 即使按只读权限进入套餐模板页，响应中也不会返回套餐成本及其项目行成本，
 * 避免只读入口成为敏感成本泄露通道。</p>
 */
@Component
public class PackageCostContext {

    private static final Set<EmployeeRole> COST_VISIBLE = EnumSet.of(
            EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Finance);

    public boolean canViewCost(Employee employee) {
        return employee != null && employee.getRole() != null && COST_VISIBLE.contains(employee.getRole());
    }

    /** 无权限时同时裁剪套餐汇总成本与项目行内嵌报价项目成本。 */
    public PackageTemplate mask(PackageTemplate template, Employee employee) {
        if (template == null || canViewCost(employee)) return template;
        template.setCost(null);
        template.setReferenceCost(null);
        if (template.getItems() != null) {
            for (PackageLine line : template.getItems()) {
                if (line == null) continue;
                line.setCostPriceSnapshot(null);
                if (line.getItem() != null) line.getItem().setCostPrice(null);
            }
        }
        return template;
    }

    public Page<PackageTemplate> mask(Page<PackageTemplate> page, Employee employee) {
        return page == null ? null : page.map(template -> mask(template, employee));
    }

    public PackageLine mask(PackageLine line, Employee employee) {
        if (line == null || canViewCost(employee)) return line;
        line.setCostPriceSnapshot(null);
        if (line.getItem() != null) line.getItem().setCostPrice(null);
        return line;
    }

    public Page<PackageLine> maskLines(Page<PackageLine> page, Employee employee) {
        return page == null ? null : page.map(line -> mask(line, employee));
    }
}
