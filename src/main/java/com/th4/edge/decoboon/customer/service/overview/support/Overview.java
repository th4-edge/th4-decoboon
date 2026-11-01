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
import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.statistic.model.StoreScopeInfo;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.repository.StoreRepository;

/** 企业负责人客户经营聚合实现。 */
@Note("客户经营聚合")
@Properties(allow = {
        @AllowProperties(model = CustomerOverviewReport.class,
                properties = {"asOf", "month", "metrics", "stageCounts", "ownerGroups", "customers",
                        "current", "size", "total", "hasMore", "storeScope"}),
        @AllowProperties(model = StoreScopeInfo.class,
                properties = {"scopeType", "storeId", "storeName", "unassignedCount"}),
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
                        "quoteAmount", "signedAmount", "projectId", "projectName", "mainlineKey", "mainlineLabel", "mainlineHint", "createTime",
                        "lossId", "lossScope", "lossStage", "lossReason", "lossReasonText", "competitorName", "lostAt", "lostBy",
                        "lossPropertyId", "lossPropertyName", "lossFromStatus", "activeLossCount", "partialLoss"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.CustomerGrade.class, properties = {"level", "score", "computedAt", "breakdown"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.CustomerPortrait.class, properties = {"portraitId", "decisionRole", "purpose", "houseCondition", "startWindow", "style", "eco", "household", "payment", "concerns", "competitor"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.CustomerTag.class, properties = {"tagId", "name", "category", "scope", "color", "status", "sort", "key"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.CustomerGrade.Item.class, properties = {"dimension", "label", "weight", "score", "reason"})
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
    private final StoreFilterContext storeFilterContext;
    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;
    private final TenantContext tenantContext;

    public Overview(CustomerOverviewContext context,
                    StoreFilterContext storeFilterContext,
                    StoreRepository storeRepository,
                    CustomerRepository customerRepository,
                    TenantContext tenantContext) {
        this.context = context;
        this.storeFilterContext = storeFilterContext;
        this.storeRepository = storeRepository;
        this.customerRepository = customerRepository;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOverviewReport call(Customer request) {
        // 门店维度（DEC-067）：经 @AutoInject 绑定（GAP-048），解析 + 越权校验
        Long storeFilter = storeFilterContext.resolve(employee, storeId);
        CustomerOverviewReport report =
                context.report(employee, current, size, keyword, stage, sort, storeFilter, ownerId);
        report.setStoreScope(storeScopeInfo(employee, storeFilter));
        return report;
    }

    /**
     * 门店维度范围标注（`DEC-067` BOSS-STORE-03，与 `P0B-04` 同构）：
     * 「未归属门店」条数由 SQL `COUNT` 给出**全量口径**（不是当前页 20 行的局部计数）。
     */
    private StoreScopeInfo storeScopeInfo(Employee employee, Long storeFilter) {
        StoreScopeInfo info = new StoreScopeInfo();
        boolean allStores = StoreScope.isAllStores(storeFilter);
        info.setScopeType(allStores ? "AllStores" : "Store");
        if (!allStores) {
            info.setStoreId(storeFilter);
            com.th4.edge.tenant.user.model.Store store = storeRepository.findByStoreId(storeFilter);
            if (store != null) {
                info.setStoreName(store.getName());
            }
            return info;
        }
        Long comId = tenantContext.requireEmployee(employee).getCompany().getComId();
        info.setUnassignedCount((int) customerRepository.countByCompany_ComIdAndStoreIsNull(comId));
        return info;
    }
}
