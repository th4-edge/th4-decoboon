package com.th4.edge.decoboon.customer.service.mycustomer.overview.support;

import com.th4.edge.decoboon.customer.context.MyCustomerOverviewContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerMetricCard;
import com.th4.edge.decoboon.customer.model.CustomerOverviewReport;
import com.th4.edge.decoboon.customer.model.CustomerStageCount;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设计师「我的客户」聚合实现。
 *
 * <p>数据范围由服务端按登录员工推导（本人负责 ∪ 指派给我设计），不接受前端传入企业/员工。
 * 阶段计数与经营时钟为聚合 SQL；客户列表只对当前页做有界投影组装，企业与项目由
 * {@code CustomerInsightContext} 统一派生，与企业负责人视角同源。</p>
 */
@Note("我的客户聚合")
@Properties(allow = {
        @AllowProperties(model = CustomerOverviewReport.class,
                properties = {"asOf", "month", "metrics", "stageCounts", "followCounts", "customers",
                        "current", "size", "total", "hasMore"}),
        @AllowProperties(model = CustomerMetricCard.class,
                properties = {"key", "label", "value", "valueType", "status", "subText"}),
        @AllowProperties(model = CustomerStageCount.class,
                properties = {"key", "label", "count"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr", "area", "budget",
                        "status", "owner", "designer", "remark", "lastFollowAt", "nextFollowAt", "wechatAdded", "wechatNo",
                        "wechatAddedAt", "visitCount", "lastVisitAt", "pendingMeasureAt", "measureAt", "measureRequestedAt",
                        "handedOffAt", "tags", "portrait", "grade", "idleDays", "riskLevel", "riskReason", "nextAction",
                        "quoteAmount", "signedAmount", "projectId", "projectName", "mainlineKey", "mainlineLabel", "mainlineHint", "createTime",
                        "lossId", "lossScope", "lossStage", "lossReason", "lossReasonText", "competitorName", "lostAt", "lostBy",
                        "lossPropertyId", "lossPropertyName", "lossFromStatus", "activeLossCount", "partialLoss"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.CustomerPortrait.class,
                properties = {"portraitId", "decisionRole", "purpose", "houseCondition", "startWindow",
                        "style", "eco", "household", "payment", "concerns", "competitor"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.CustomerGrade.class, properties = {"level", "score", "computedAt", "breakdown"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.CustomerTag.class, properties = {"tagId", "name", "category", "scope", "color", "status", "sort", "key"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.CustomerGrade.Item.class, properties = {"dimension", "label", "weight", "score", "reason"})
})
@Service("customer/my-customer/overview")
@Scope(ScopeTypes.PROTOTYPE)
public class Overview implements com.th4.edge.decoboon.customer.service.mycustomer.overview.Overview {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private String keyword;
    @AutoInject
    private String stage;
    @AutoInject
    private String sort;
    /** all / overdue / today / soon / unplanned；设置时只返回设计师待跟进队列。 */
    @AutoInject
    private String followFilter;

    private final MyCustomerOverviewContext context;
    private final TenantContext tenantContext;

    public Overview(MyCustomerOverviewContext context, TenantContext tenantContext) {
        this.context = context;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOverviewReport call(Customer request) {
        // @User 注入的是轻量登录主体，企业与门店需由服务端按登录员工回读，禁止信任前端传值。
        Employee principal = tenantContext.requireEmployee(employee);
        return context.report(principal, current, size, keyword, stage, sort, followFilter);
    }
}
