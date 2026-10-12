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
import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;

/** 业务分析报表实现。 */
@Note("业务分析报表")
@Properties(allow = {
        @AllowProperties(model = BusinessAnalysisReport.class,
                properties = {"month", "storeScope", "funnel", "overallConversionRate", "quoteConversion", "channels",
                        "regions", "orderSizes", "medianOrderAmount", "maxOrderAmount"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.StoreScopeInfo.class,
                properties = {"scopeType", "storeId", "storeName", "unassignedCount"}),
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

    /**
     * 门店维度（DEC-067 / `GAP-048`）。框架只对 provider 实例的 {@code @AutoInject} 字段做请求绑定，
     * 请求模型不承载 HTTP 参数；因此门店维度落在字段上，解析校验后写回请求模型供上下文取数。
     */
    @AutoInject
    private Long storeId;


    private final BusinessAnalysisContext businessContext;
    private final StoreFilterContext storeFilterContext;

    public Report(BusinessAnalysisContext businessContext, StoreFilterContext storeFilterContext) {
        this.businessContext = businessContext;
        this.storeFilterContext = storeFilterContext;
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessAnalysisReport call(BusinessAnalysisQuery request) {
        if (request == null) {
            request = new BusinessAnalysisQuery();
        }
        // 门店维度：经 @AutoInject 绑定（GAP-048），解析 + 越权校验后写回请求模型
        request.setStoreId(storeFilterContext.resolve(employee, storeId));
        return businessContext.report(employee, request);
    }
}
