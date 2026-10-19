package com.th4.edge.decoboon.statistic.service.store.profit.report.support;

import com.th4.edge.decoboon.statistic.context.StoreProfitContext;
import com.th4.edge.decoboon.statistic.model.StoreProfitReport;
import com.th4.edge.decoboon.statistic.model.StoreProfitRequest;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 门店经营利润（参考）报告：BOSS 指定门店；店长/门店财务锁定本店。 */
@Note("门店经营利润报告")
@Properties(allow = @AllowProperties(model = StoreProfitReport.class,
        properties = {"storeId", "storeName", "periodLabel", "projectGrossProfit", "otherIncome",
                "directCompanyExpense", "storeProfit", "costComplete", "profitWarning", "unallocatedNote"}))
@Service("statistic/store/profit/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.store.profit.report.Report {

    @User
    private Employee employee;

    @AutoInject
    private Long storeId;

    private final TenantContext tenantContext;
    private final StoreProfitContext profitContext;

    public Report(TenantContext tenantContext, StoreProfitContext profitContext) {
        this.tenantContext = tenantContext;
        this.profitContext = profitContext;
    }

    @Override
    @Transactional(readOnly = true)
    public StoreProfitReport call(StoreProfitRequest request) {
        Employee current = tenantContext.requireRole(employee,
                EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Finance);
        return profitContext.calculate(current, storeId);
    }
}
