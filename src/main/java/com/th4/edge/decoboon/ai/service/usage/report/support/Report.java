package com.th4.edge.decoboon.ai.service.usage.report.support;

import com.th4.edge.decoboon.ai.context.AiUsageReportContext;
import com.th4.edge.decoboon.ai.model.AiUsageQuery;
import com.th4.edge.decoboon.ai.model.AiUsageReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 企业 AI 资源用量报表实现。P0 仅 BOSS 可读。 */
@Note("AI资源用量报表")
@Properties(allow = {
        @AllowProperties(model = AiUsageReport.class,
                properties = {"asOf", "from", "to", "requestCount", "validCallCount", "successCount",
                        "failureCount", "resourceUnits", "shadowBillableUnits", "refundedUnits",
                        "netBillableUnits", "successRate", "trend", "stores", "capabilities"}),
        @AllowProperties(model = AiUsageReport.AiUsageTrendPoint.class,
                properties = {"date", "requestCount", "successCount", "shadowBillableUnits"}),
        @AllowProperties(model = AiUsageReport.AiUsageStoreSummary.class,
                properties = {"storeId", "storeName", "allocationType", "requestCount", "successCount",
                        "failureCount", "resourceUnits", "shadowBillableUnits"}),
        @AllowProperties(model = AiUsageReport.AiUsageCapabilitySummary.class,
                properties = {"capabilityCode", "requestCount", "successCount", "failureCount",
                        "shadowBillableUnits"})
})
@Service("ai/usage/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.ai.service.usage.report.Report {

    @User
    private Employee employee;

    private final AiUsageReportContext reportContext;

    public Report(AiUsageReportContext reportContext) {
        this.reportContext = reportContext;
    }

    @Override
    @Transactional(readOnly = true)
    public AiUsageReport call(AiUsageQuery request) {
        return reportContext.report(employee, request);
    }
}
