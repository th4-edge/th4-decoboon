package com.th4.edge.decoboon.statistic.service.business.report.support;

import com.th4.edge.decoboon.statistic.context.BusinessAnalysisContext;
import com.th4.edge.decoboon.statistic.model.BusinessAnalysisQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAnalysisReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业务分析报表实现。 */
@Note("业务分析报表")
@Properties(allow = {
        @AllowProperties(model = BusinessAnalysisReport.class,
                properties = {"month", "funnel", "overallConversionRate", "quoteConversion", "channels",
                        "regions", "orderSizes", "medianOrderAmount", "maxOrderAmount"}),
        @AllowProperties(model = BusinessAnalysisReport.FunnelStage.class,
                properties = {"key", "label", "count", "unit"}),
        @AllowProperties(model = BusinessAnalysisReport.QuoteConversion.class,
                properties = {"finalQuoteAcceptanceRate", "firstQuoteAcceptanceRate", "decidedOpportunityCount",
                        "pendingDecisionQuoteCount", "submittedQuoteCount"}),
        @AllowProperties(model = BusinessAnalysisReport.ChannelMetric.class,
                properties = {"source", "label", "leadCount", "visitCount", "signedCount", "signRate", "avgOrderAmount"}),
        @AllowProperties(model = BusinessAnalysisReport.RegionMetric.class,
                properties = {"community", "signedCount", "signedAmount"}),
        @AllowProperties(model = BusinessAnalysisReport.OrderSizeBucket.class,
                properties = {"key", "label", "count"})
})
@Service("statistic/boss/business/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.business.report.Report {

    @User
    private Employee employee;

    private final BusinessAnalysisContext businessContext;

    public Report(BusinessAnalysisContext businessContext) {
        this.businessContext = businessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessAnalysisReport call(BusinessAnalysisQuery request) {
        return businessContext.report(employee, request);
    }
}
