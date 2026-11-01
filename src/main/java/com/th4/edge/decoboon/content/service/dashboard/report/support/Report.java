package com.th4.edge.decoboon.content.service.dashboard.report.support;

import com.th4.edge.decoboon.content.context.ContentDashboardContext;
import com.th4.edge.decoboon.content.model.ContentDashboardReport;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.decoboon.content.model.LeadTouch;
import com.th4.edge.decoboon.content.model.PublicationRecord;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 内容经营看板。 */
@Note("内容经营看板")
@Properties(allow = {
        @AllowProperties(model = ContentDashboardReport.class, properties = {"metrics", "recentTouches", "recentTasks"}),
        @AllowProperties(model = LeadTouch.class, properties = {"touchId", "customer", "employeeContent", "publication", "task", "employee", "attributionType", "confidence", "isPrimary", "touchedAt"}),
        @AllowProperties(model = ContentTask.class, properties = {"taskId", "title", "status", "deadline"}),
        @AllowProperties(model = EmployeeContent.class, properties = {"contentId", "status", "caption"}),
        @AllowProperties(model = PublicationRecord.class, properties = {"publicationId", "platform", "postUrl", "publishedAt", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "status", "measureAt"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.content.model.ContentDashboardReport.Metric.class, properties = {"key", "label", "value", "subText"})
})
@Service("content/dashboard/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.content.service.dashboard.report.Report {
    @User private Employee employee;
    private final ContentDashboardContext dashboardContext;

    public Report(ContentDashboardContext dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @Override public ContentDashboardReport call(ContentDashboardReport request) {
        return dashboardContext.report(employee);
    }
}
