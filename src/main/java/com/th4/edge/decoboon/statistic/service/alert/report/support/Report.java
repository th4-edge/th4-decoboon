package com.th4.edge.decoboon.statistic.service.alert.report.support;

import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.statistic.context.BusinessAlertContext;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.decoboon.statistic.model.BusinessAlertQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 企业经营预警报表实现。 */
@Note("经营预警报表")
@Properties(allow = {
        @AllowProperties(model = BusinessAlertReport.class,
                properties = {"asOf", "totalCount", "highCount", "mediumCount", "lowCount", "overdueReceivables",
                        "suspensionRisks", "materialOverIssues", "changeAnomalies"}),
        @AllowProperties(model = BusinessAlert.class,
                properties = {"type", "level", "title", "message", "businessType", "businessId", "businessNo",
                        "project", "owner", "amount", "rate", "dueDate"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager",
                        "siteAddr", "status", "progress", "planEndDate", "actualEndDate"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.Customer.class, properties = {"custId", "name", "telNo", "community", "addr", "area", "status"})
})
@Service("statistic/alert/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.alert.report.Report {

    @User
    private Employee employee;

    private final BusinessAlertContext alertContext;

    public Report(BusinessAlertContext alertContext) {
        this.alertContext = alertContext;
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessAlertReport call(BusinessAlertQuery request) {
        return alertContext.report(employee, request);
    }
}
