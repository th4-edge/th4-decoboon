package com.th4.edge.decoboon.statistic.service.boss.dashboard.support;

import com.th4.edge.decoboon.statistic.context.BossDashboardContext;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BossDashboardQuery;
import com.th4.edge.decoboon.statistic.model.DashboardMetric;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;

/** 企业负责人经营驾驶舱实现。 */
@Note("企业负责人经营驾驶舱")
@Properties(allow = {
        @AllowProperties(model = BossDashboard.class,
                properties = {"month", "storeScope", "signedAmount", "receivedAmount", "grossProfit", "activeProjectCount"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.StoreScopeInfo.class,
                properties = {"scopeType", "storeId", "storeName", "unassignedCount"}),
        @AllowProperties(model = DashboardMetric.class,
                properties = {"value", "target", "achievementRate", "previousMonthValue", "previousYearValue",
                        "monthOverMonthRate", "yearOverYearRate", "targetSet", "monthOverMonthComparable", "yearOverYearComparable", "complete", "warning"})
})
@Service("statistic/boss/dashboard")
@Scope(ScopeTypes.PROTOTYPE)
public class Dashboard implements com.th4.edge.decoboon.statistic.service.boss.dashboard.Dashboard {

    @User
    private Employee employee;

    /**
     * 门店维度（DEC-067 / `GAP-048`）。框架只对 provider 实例的 {@code @AutoInject} 字段做请求绑定，
     * 请求模型不承载 HTTP 参数；因此门店维度落在字段上，解析校验后写回请求模型供上下文取数。
     */
    @AutoInject
    private Long storeId;


    private final BossDashboardContext dashboardContext;
    private final StoreFilterContext storeFilterContext;

    public Dashboard(BossDashboardContext dashboardContext, StoreFilterContext storeFilterContext) {
        this.dashboardContext = dashboardContext;
        this.storeFilterContext = storeFilterContext;
    }

    @Override
    @Transactional(readOnly = true)
    public BossDashboard call(BossDashboardQuery request) {
        if (request == null) {
            request = new BossDashboardQuery();
        }
        // 门店维度：经 @AutoInject 绑定（GAP-048），解析 + 越权校验后写回请求模型
        request.setStoreId(storeFilterContext.resolve(employee, storeId));
        return dashboardContext.dashboard(employee, request);
    }
}
