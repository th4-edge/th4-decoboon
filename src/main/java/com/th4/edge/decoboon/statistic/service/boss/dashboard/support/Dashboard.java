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

/** 企业负责人经营驾驶舱实现。 */
@Note("企业负责人经营驾驶舱")
@Properties(allow = {
        @AllowProperties(model = BossDashboard.class,
                properties = {"month", "signedAmount", "receivedAmount", "grossProfit", "activeProjectCount"}),
        @AllowProperties(model = DashboardMetric.class,
                properties = {"value", "target", "achievementRate", "previousMonthValue", "previousYearValue",
                        "monthOverMonthRate", "yearOverYearRate", "targetSet", "monthOverMonthComparable", "yearOverYearComparable", "complete", "warning"})
})
@Service("statistic/boss/dashboard")
@Scope(ScopeTypes.PROTOTYPE)
public class Dashboard implements com.th4.edge.decoboon.statistic.service.boss.dashboard.Dashboard {

    @User
    private Employee employee;

    private final BossDashboardContext dashboardContext;

    public Dashboard(BossDashboardContext dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @Override
    @Transactional(readOnly = true)
    public BossDashboard call(BossDashboardQuery request) {
        return dashboardContext.dashboard(employee, request);
    }
}
