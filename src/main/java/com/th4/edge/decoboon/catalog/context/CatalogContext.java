package com.th4.edge.decoboon.catalog.context;

import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

/** 家装报价项目库上下文。 */
@Note("报价项目库上下文")
@Component
public class CatalogContext {

    private final DataScopeContext dataScopeContext;

    public CatalogContext(DataScopeContext dataScopeContext) {
        this.dataScopeContext = dataScopeContext;
    }

    public Employee requireEmployee(Employee principal) {
        return dataScopeContext.requireEmployee(principal);
    }

    /** 报价项目库价格维护：企业负责人、店长、财务；销售价与标准成本分开保存。 */
    public Employee requirePriceMaintainer(Employee principal) {
        Employee current = requireEmployee(principal);
        if (current.getRole() != EmployeeRole.Boss
                && current.getRole() != EmployeeRole.StoreManager
                && current.getRole() != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号无权维护报价项目库价格"));
        }
        return current;
    }

    /** 删除、批量导入等高风险维护仍按既有边界控制，财务不因价格维护权限自动获得。 */
    public Employee requireMaintainer(Employee principal) {
        Employee current = requireEmployee(principal);
        if (current.getRole() != EmployeeRole.Boss
                && current.getRole() != EmployeeRole.StoreManager) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号无权维护报价项目库"));
        }
        return current;
    }

    public String formula(PricingMethod method) {
        if (method == PricingMethod.FixedTotal) {
            return "固定总价，不乘工程量";
        }
        String unit = switch (method == null ? PricingMethod.MaterialAndManual : method) {
            case MaterialOnly -> "材料单价";
            case ManualOnly -> "人工单价";
            case MaterialAndManual -> "材料单价 + 人工单价";
            case FixedTotal -> "";
        };
        return "(" + unit + ") × 工程量";
    }
}
