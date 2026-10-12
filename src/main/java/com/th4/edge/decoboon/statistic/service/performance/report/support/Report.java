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
import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;

/** 人员绩效自动考核报表实现。 */
@Note("人员绩效报表")
@Properties(allow = {
        @AllowProperties(model = PerformanceReport.class,
                properties = {"month", "storeScope", "designerCount", "marketerCount", "pmCount", "averageDesignerConversionRate",
                        "averageMarketerSignRate", "averagePmOnTimeDeliveryRate", "averagePmAcceptancePassRate",
                        "performances"}),
        @AllowProperties(model = EmployeePerformance.class,
                properties = {"employee", "role", "quoteCount", "acceptedQuoteCount", "designerConversionRate",
                        "pendingDecisionQuoteCount", "decidedOpportunityCount", "firstQuoteAcceptanceRate",
                        "customerCount", "signedCustomerCount", "marketerSignRate", "deliveredProjectCount",
                        "onTimeProjectCount", "pmOnTimeDeliveryRate", "acceptanceCount", "acceptedAcceptanceCount",
                        "pmAcceptancePassRate"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.StoreScopeInfo.class,
                properties = {"scopeType", "storeId", "storeName", "unassignedCount"}),
        // 绩效列表只需要员工身份摘要；store/team 会带出 company/leader 循环归属图。
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("statistic/performance/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.performance.report.Report {

    @User
    private Employee employee;

    /**
     * 门店维度（DEC-067 / `GAP-048`）。框架只对 provider 实例的 {@code @AutoInject} 字段做请求绑定，
     * 请求模型不承载 HTTP 参数；因此门店维度落在字段上，解析校验后写回请求模型供上下文取数。
     */
    @AutoInject
    private Long storeId;


    private final PerformanceReportContext reportContext;
    private final StoreFilterContext storeFilterContext;

    public Report(PerformanceReportContext reportContext, StoreFilterContext storeFilterContext) {
        this.reportContext = reportContext;
        this.storeFilterContext = storeFilterContext;
    }

    @Override
    @Transactional(readOnly = true)
    public PerformanceReport call(PerformanceQuery request) {
        if (request == null) {
            request = new PerformanceQuery();
        }
        // 门店维度：经 @AutoInject 绑定（GAP-048），解析 + 越权校验后写回请求模型
        request.setStoreId(storeFilterContext.resolve(employee, storeId));
        return reportContext.report(employee, request);
    }
}
