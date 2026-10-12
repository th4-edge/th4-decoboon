package com.th4.edge.decoboon.statistic.service.organization.report.support;

import com.th4.edge.decoboon.statistic.context.OrganizationContext;
import com.th4.edge.decoboon.statistic.model.EmployeePerformance;
import com.th4.edge.decoboon.statistic.model.OrganizationQuery;
import com.th4.edge.decoboon.statistic.model.OrganizationReport;
import com.th4.edge.decoboon.statistic.model.RoleCapacity;
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

/** 企业组织效能报表实现。 */
@Note("企业组织效能报表")
@Properties(allow = {
        @AllowProperties(model = OrganizationReport.class,
                properties = {"month", "storeScope", "headcount", "newHires", "signedAmount", "newProjectCount",
                        "perCapitaOutput", "perCapitaOrders", "roleCapacities",
                        "designerRanking", "marketerRanking", "pmRanking", "sampleSufficient"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.StoreScopeInfo.class,
                properties = {"scopeType", "storeId", "storeName", "unassignedCount"}),
        @AllowProperties(model = RoleCapacity.class,
                properties = {"role", "label", "count", "metricKey", "metricLabel", "metricValue"}),
        @AllowProperties(model = EmployeePerformance.class,
                properties = {"employee", "role", "quoteCount", "acceptedQuoteCount", "designerConversionRate",
                        "pendingDecisionQuoteCount", "decidedOpportunityCount", "firstQuoteAcceptanceRate",
                        "customerCount", "signedCustomerCount", "marketerSignRate", "deliveredProjectCount",
                        "onTimeProjectCount", "pmOnTimeDeliveryRate", "acceptanceCount", "acceptedAcceptanceCount",
                        "pmAcceptancePassRate"}),
        // 排行只需要员工身份摘要，不带 store/team，避免 company/leader 循环图。
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("statistic/organization/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.organization.report.Report {

    @User
    private Employee employee;

    /**
     * 门店维度（DEC-067 / `GAP-048`）。框架只对 provider 实例的 {@code @AutoInject} 字段做请求绑定，
     * 请求模型不承载 HTTP 参数；因此门店维度落在字段上，解析校验后写回请求模型供上下文取数。
     */
    @AutoInject
    private Long storeId;


    private final OrganizationContext organizationContext;
    private final StoreFilterContext storeFilterContext;

    public Report(OrganizationContext organizationContext, StoreFilterContext storeFilterContext) {
        this.organizationContext = organizationContext;
        this.storeFilterContext = storeFilterContext;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationReport call(OrganizationQuery request) {
        if (request == null) {
            request = new OrganizationQuery();
        }
        // 门店维度：经 @AutoInject 绑定（GAP-048），解析 + 越权校验后写回请求模型
        request.setStoreId(storeFilterContext.resolve(employee, storeId));
        return organizationContext.report(employee, request);
    }
}
