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

/** 企业组织效能报表实现。 */
@Note("企业组织效能报表")
@Properties(allow = {
        @AllowProperties(model = OrganizationReport.class,
                properties = {"month", "headcount", "newHires", "signedAmount", "newProjectCount",
                        "perCapitaOutput", "perCapitaOrders", "roleCapacities",
                        "designerRanking", "marketerRanking", "pmRanking", "sampleSufficient"}),
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

    private final OrganizationContext organizationContext;

    public Report(OrganizationContext organizationContext) {
        this.organizationContext = organizationContext;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationReport call(OrganizationQuery request) {
        return organizationContext.report(employee, request);
    }
}
