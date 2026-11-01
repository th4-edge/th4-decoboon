package com.th4.edge.decoboon.statistic.service.boss.workbench.support;

import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.statistic.context.BossDashboardContext;
import com.th4.edge.decoboon.statistic.context.BossWorkbenchContext;
import com.th4.edge.decoboon.statistic.context.BusinessAlertContext;
import com.th4.edge.decoboon.statistic.context.BusinessEventContext;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BossDashboardQuery;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.statistic.model.BossWorkbench;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.decoboon.statistic.model.BossWorkbenchQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAlertQuery;
import com.th4.edge.decoboon.statistic.model.BusinessEvent;
import com.th4.edge.decoboon.statistic.model.WorkbenchIndicator;
import com.th4.edge.decoboon.statistic.model.WorkbenchProject;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 企业负责人首页聚合实现。 */
@Note("企业负责人首页")
@Properties(allow = {
        @AllowProperties(model = BossWorkbench.class,
                properties = {"asOf", "month", "indicators", "attentionProjects", "businessEvents", "alertReport"}),
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
        @AllowProperties(model = WorkbenchIndicator.class,
                properties = {"key", "label", "value", "valueType", "status", "statusText", "detail"}),
        @AllowProperties(model = WorkbenchProject.class,
                properties = {"projectId", "no", "name", "status", "progress", "managerName", "riskLevel",
                        "riskCount", "mainRiskType", "riskReason", "nextAction", "amount", "dueDate"}),
        @AllowProperties(model = BusinessEvent.class,
                properties = {"eventId", "category", "type", "level", "title", "content", "occurredAt", "amount",
                        "businessType", "businessId", "businessNo", "projectId", "projectName", "customerId",
                        "customerName", "ownerName"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.Customer.class, properties = {"custId", "name", "telNo", "community", "addr", "area", "status"})
})
@Service("statistic/boss/workbench")
@Scope(ScopeTypes.PROTOTYPE)
public class Workbench implements com.th4.edge.decoboon.statistic.service.boss.workbench.Workbench {

    @User
    private Employee employee;

    private final BossDashboardContext dashboardContext;
    private final BusinessAlertContext alertContext;
    private final BusinessEventContext eventContext;
    private final BossWorkbenchContext workbenchContext;

    public Workbench(BossDashboardContext dashboardContext,
                     BusinessAlertContext alertContext,
                     BusinessEventContext eventContext,
                     BossWorkbenchContext workbenchContext) {
        this.dashboardContext = dashboardContext;
        this.alertContext = alertContext;
        this.eventContext = eventContext;
        this.workbenchContext = workbenchContext;
    }

    @Override
    @Transactional(readOnly = true)
    public BossWorkbench call(BossWorkbenchQuery request) {
        BossWorkbenchQuery query = request == null ? new BossWorkbenchQuery() : request;
        LocalDate month = query.getMonth() == null ? LocalDate.now().withDayOfMonth(1) : query.getMonth().withDayOfMonth(1);
        LocalDate asOf = LocalDate.now();

        BossDashboardQuery dashboardQuery = new BossDashboardQuery();
        dashboardQuery.setMonth(month);
        BossDashboard dashboard = dashboardContext.dashboard(employee, dashboardQuery);

        BusinessAlertQuery alertQuery = new BusinessAlertQuery();
        alertQuery.setAsOf(asOf);
        BusinessAlertReport alerts = alertContext.report(employee, alertQuery);

        List<BusinessEvent> events = eventContext.events(employee, asOf, query.getEventLimit());
        return workbenchContext.build(dashboard, alerts, events, month, asOf);
    }
}
