package com.th4.edge.decoboon.statistic.service.workbench.site.report.support;

import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.decoboon.statistic.context.SiteWorkbenchContext;
import com.th4.edge.decoboon.statistic.model.SiteWorkbench;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 项目工地工作台实现。
 *
 * <p>面向项目经理、财务（含履约协同）、店长、团队负责人与企业负责人：
 * 在服务端一次性完成数据范围过滤与派生状态计算，返回三栏工作项，
 * 前端只负责渲染，不得二次推导业务状态。</p>
 */
@Note("项目工地工作台")
@Description("按当前员工数据范围聚合计划、任务、材料、验收与回款风险，输出需要处理/推进中/风险三栏工作项")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.SiteWorkbench.class,
                properties = {"asOf", "metrics", "bucketCounts", "items", "ownerGroups", "planningStart", "planningEnd", "planningDays"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.SiteWorkbench.Metric.class,
                properties = {"key", "label", "count", "hint", "tone"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.SiteWorkbench.BucketCount.class,
                properties = {"bucket", "label", "count"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.SiteWorkItem.class,
                properties = {"itemKey", "domain", "stage", "stageName", "subjectType", "subjectId", "projectId", "title", "subtitle", "ownerName", "derivedState", "riskLevel", "stayDays", "reason", "nextActionLabel", "nextActionType", "nextActionTarget", "dueAt", "amount", "bucket", "metrics"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.SiteWorkbench.OwnerGroup.class,
                properties = {"ownerId", "ownerName", "count", "needActionCount", "riskCount"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.SiteWorkbench.PlanningDay.class,
                properties = {"date", "weekday", "today", "count", "dense", "denseHint", "items"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.SiteWorkbench.PlanningItem.class,
                properties = {"subjectType", "subjectId", "projectId", "projectName", "kind", "title", "ownerName", "plannedDate", "overdue", "reschedulable", "nextActionLabel"})
})
@Service("statistic/workbench/site/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.workbench.site.report.Report {

    private final TenantContext tenantContext;
    private final SiteWorkbenchContext workbenchContext;

    public Report(TenantContext tenantContext, SiteWorkbenchContext workbenchContext) {
        this.tenantContext = tenantContext;
        this.workbenchContext = workbenchContext;
    }

    @Override
    @Transactional(readOnly = true)
    public SiteWorkbench call(@User Employee employee) {
        Employee current = tenantContext.requireRole(employee,
                EmployeeRole.Boss,
                EmployeeRole.StoreManager,
                EmployeeRole.TeamLeader,
                EmployeeRole.Pm,
                EmployeeRole.Finance);
        if (current.getCompany() == null || current.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        return workbenchContext.build(current);
    }
}
