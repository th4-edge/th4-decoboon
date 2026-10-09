package com.th4.edge.decoboon.statistic.service.workbench.designer.report.support;

import com.th4.edge.decoboon.statistic.context.DesignerWorkbenchContext;
import com.th4.edge.decoboon.statistic.model.DesignerWorkbenchReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设计师业务工作台聚合实现。 */
@Note("设计师业务工作台")
@Properties(allow = {
        @AllowProperties(model = DesignerWorkbenchReport.class, properties = {
                "role", "employeeId", "employeeName", "companyId", "storeId", "dataScope",
                "generatedAt", "month", "trendStart", "trendEnd", "status", "summary", "cards", "pipeline", "charts", "warnings", "performanceRanking"
        }),
        @AllowProperties(model = DesignerWorkbenchReport.SummaryMetric.class,
                properties = {"key", "value", "unit", "previousValue", "changeRate", "comparisonState", "comparisonLabel"}),
        @AllowProperties(model = DesignerWorkbenchReport.CardMetric.class,
                properties = {"key", "count", "riskCount", "status", "breakdown"}),
        @AllowProperties(model = DesignerWorkbenchReport.Breakdown.class, properties = {"key", "count"}),
        @AllowProperties(model = DesignerWorkbenchReport.PipelineStage.class,
                properties = {"key", "label", "count", "rate", "previousCount", "state", "description", "route"}),
        @AllowProperties(model = DesignerWorkbenchReport.Chart.class, properties = {"key", "type", "series"}),
        @AllowProperties(model = DesignerWorkbenchReport.ChartSeries.class, properties = {"key", "value", "points"}),
        @AllowProperties(model = DesignerWorkbenchReport.TrendPoint.class, properties = {"date", "value"}),
        @AllowProperties(model = DesignerWorkbenchReport.Warning.class, properties = {"key", "scopeKey", "message"}),
        @AllowProperties(model = DesignerWorkbenchReport.PerformanceRanking.class, properties = {
                "metricKey", "metricLabel", "periodLabel", "rank", "designerCount", "sampleSufficient",
                "myValue", "topValue", "peerAverage", "gapToTop", "gapToAverage", "topDesigners"
        }),
        @AllowProperties(model = DesignerWorkbenchReport.RankingItem.class, properties = {
                "employeeId", "employeeName", "rank", "value", "signedProjectCount", "currentEmployee"
        })
})
@Service("statistic/workbench/designer/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.workbench.designer.report.Report {

    private final TenantContext tenantContext;
    private final DesignerWorkbenchContext workbenchContext;

    public Report(TenantContext tenantContext, DesignerWorkbenchContext workbenchContext) {
        this.tenantContext = tenantContext;
        this.workbenchContext = workbenchContext;
    }

    @Override
    @Transactional(readOnly = true)
    public DesignerWorkbenchReport call(@User Employee employee) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Designer);
        if (current.getCompany() == null || current.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        return workbenchContext.build(current);
    }
}
