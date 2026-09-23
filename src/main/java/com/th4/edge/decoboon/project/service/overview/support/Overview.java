package com.th4.edge.decoboon.project.service.overview.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectOverviewContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectMetricCard;
import com.th4.edge.decoboon.project.model.ProjectOverviewReport;
import com.th4.edge.decoboon.project.model.ProjectStatusCount;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 企业负责人项目经营聚合实现。 */
@Note("项目经营聚合")
@Properties(allow = {
        @AllowProperties(model = ProjectOverviewReport.class,
                properties = {"asOf", "month", "metrics", "statusCounts", "contractCounts", "projects",
                        "current", "size", "total", "hasMore"}),
        @AllowProperties(model = ProjectMetricCard.class,
                properties = {"key", "label", "value", "valueType", "status", "subText", "trendRate"}),
        @AllowProperties(model = ProjectStatusCount.class,
                properties = {"key", "label", "count"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "customer", "property", "quote", "designer", "manager", "support",
                        "siteAddr", "area", "contractAmount", "cost", "status", "progress", "startDate",
                        "planEndDate", "actualEndDate", "currentStageName", "createTime",
                        "riskLevel", "riskCount", "mainRiskType", "riskReason", "nextAction",
                        "receivedAmount", "receivableAmount", "actualCost", "grossProfit", "grossMargin", "costComplete",
                        "overdueDays", "overdueReceivableCount", "contractStage"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status", "owner"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "amount", "cost", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role"})
})
@Service("project/project/overview")
@Scope(ScopeTypes.PROTOTYPE)
public class Overview implements com.th4.edge.decoboon.project.service.overview.Overview {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private String status;

    @AutoInject
    private String contractStage;

    /** 风险筛选：risk/abnormal=仅有风险；Normal/Attention/High=按等级；空或 all=不过滤。 */
    @AutoInject
    private String riskLevel;

    @AutoInject
    private String keyword;

    @AutoInject
    private String sort;

    @AutoInject
    private Long storeId;

    private final ProjectOverviewContext context;

    public Overview(ProjectOverviewContext context) {
        this.context = context;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectOverviewReport call(Project request) {
        return context.report(employee, current, size, status, contractStage, riskLevel, keyword, sort, storeId);
    }
}
