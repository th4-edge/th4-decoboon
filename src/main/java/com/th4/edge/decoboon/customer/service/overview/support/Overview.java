package com.th4.edge.decoboon.customer.service.overview.support;

import com.th4.edge.decoboon.customer.context.CustomerOverviewContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerMetricCard;
import com.th4.edge.decoboon.customer.model.CustomerMetricPeriod;
import com.th4.edge.decoboon.customer.model.CustomerOwnerGroup;
import com.th4.edge.decoboon.customer.model.CustomerOverviewReport;
import com.th4.edge.decoboon.customer.model.CustomerStageCount;
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

/** 企业负责人客户经营聚合实现。 */
@Note("客户经营聚合")
@Properties(allow = {
        @AllowProperties(model = CustomerOverviewReport.class,
                properties = {"asOf", "month", "metrics", "stageCounts", "ownerGroups", "customers",
                        "current", "size", "total", "hasMore"}),
        @AllowProperties(model = CustomerMetricCard.class,
                properties = {"key", "label", "value", "valueType", "status", "subText", "trendRate", "periods"}),
        @AllowProperties(model = CustomerMetricPeriod.class,
                properties = {"key", "label", "value", "valueType"}),
        @AllowProperties(model = CustomerStageCount.class,
                properties = {"key", "label", "count"}),
        @AllowProperties(model = CustomerOwnerGroup.class,
                properties = {"ownerId", "ownerName", "storeName", "customerCount", "riskCustomerCount",
                        "pendingMeasureCount", "signedCustomerCount", "quoteAmount", "signedAmount"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr", "area", "budget",
                        "status", "statusBeforeDormant", "dormantReason", "dormantReasonText", "dormantAt", "dormantBy", "restoredAt", "restoredBy", "owner", "designer", "remark", "lastFollowAt", "nextFollowAt", "wechatAdded", "wechatNo",
                        "wechatAddedAt", "visitCount", "lastVisitAt", "pendingMeasureAt", "measureAt", "measureRequestedAt",
                        "handedOffAt", "tags", "portrait", "grade", "idleDays", "riskLevel", "riskReason", "nextAction",
                        "quoteAmount", "signedAmount", "projectId", "projectName", "mainlineKey", "mainlineLabel", "mainlineHint", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/customer/overview")
@Scope(ScopeTypes.PROTOTYPE)
public class Overview implements com.th4.edge.decoboon.customer.service.overview.Overview {

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
    @AutoInject
    private Long storeId;
    @AutoInject
    private Long ownerId;

    private final CustomerOverviewContext context;

    public Overview(CustomerOverviewContext context) {
        this.context = context;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOverviewReport call(Customer request) {
        return context.report(employee, current, size, keyword, stage, sort, storeId, ownerId);
    }
}
