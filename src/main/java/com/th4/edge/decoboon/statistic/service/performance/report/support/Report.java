package com.th4.edge.decoboon.statistic.service.performance.report.support;

import com.th4.edge.decoboon.statistic.context.PerformanceReportContext;
import com.th4.edge.decoboon.statistic.model.EmployeePerformance;
import com.th4.edge.decoboon.statistic.model.PerformanceQuery;
import com.th4.edge.decoboon.statistic.model.PerformanceReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 人员绩效自动考核报表实现。 */
@Note("人员绩效报表")
@Properties(allow = {
        @AllowProperties(model = PerformanceReport.class,
                properties = {"month", "designerCount", "marketerCount", "pmCount", "averageDesignerConversionRate",
                        "averageMarketerSignRate", "averagePmOnTimeDeliveryRate", "averagePmAcceptancePassRate",
                        "performances"}),
        @AllowProperties(model = EmployeePerformance.class,
                properties = {"employee", "role", "quoteCount", "acceptedQuoteCount", "designerConversionRate",
                        "customerCount", "signedCustomerCount", "marketerSignRate", "deliveredProjectCount",
                        "onTimeProjectCount", "pmOnTimeDeliveryRate", "acceptanceCount", "acceptedAcceptanceCount",
                        "pmAcceptancePassRate"}),
        // 绩效列表只需要员工身份摘要；store/team 会带出 company/leader 循环归属图。
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("statistic/performance/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.performance.report.Report {

    @User
    private Employee employee;

    private final PerformanceReportContext reportContext;

    public Report(PerformanceReportContext reportContext) {
        this.reportContext = reportContext;
    }

    @Override
    @Transactional(readOnly = true)
    public PerformanceReport call(PerformanceQuery request) {
        return reportContext.report(employee, request);
    }
}
