package com.th4.edge.tenant.billing.service.subscription.info.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Plan;
import com.th4.edge.tenant.billing.model.PlanVersion;
import com.th4.edge.tenant.billing.model.Subscription;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 当前企业订阅与额度信息 Provider。
 */
@Note("订阅信息")
@Description("返回当前企业的套餐、到期时间和席位、门店使用情况")
@Properties(allow = {
        @AllowProperties(model = Subscription.class,
                properties = {"subId", "plan", "planVersion", "status", "serviceEndTime", "trialActivated",
                        "lastRenewTime", "extraSeats", "extraStores", "createTime", "remainingDays",
                        "usedSeats", "usedStores", "effectiveMaxSeats", "effectiveMaxStores", "writable", "expired"}),
        @AllowProperties(model = Plan.class,
                properties = {"planId", "code", "name", "days", "maxStores", "maxSeats",
                        "modules", "price", "originalPrice", "description", "tagline", "highlights", "badge", "recommended"}),
        @AllowProperties(model = PlanVersion.class,
                properties = {"versionId", "versionNo", "planCode", "planName", "days", "maxStores",
                        "maxSeats", "modules", "price", "originalPrice", "description", "createTime"})
})
@Service("subscription/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.tenant.billing.service.subscription.info.Info {

    private final TenantContext tenantContext;
    private final SubscriptionContext subscriptionContext;
    private final EmployeeRepository employeeRepository;
    private final StoreRepository storeRepository;

    public Info(TenantContext tenantContext,
                SubscriptionContext subscriptionContext,
                EmployeeRepository employeeRepository,
                StoreRepository storeRepository) {
        this.tenantContext = tenantContext;
        this.subscriptionContext = subscriptionContext;
        this.employeeRepository = employeeRepository;
        this.storeRepository = storeRepository;
    }

    @Override
    public Subscription call(@User Employee employee) {
        Employee current = tenantContext.requireEmployee(employee);
        Subscription subscription = subscriptionContext.current(current.getCompany());
        if (subscription == null) {
            return null;
        }
        Long comId = current.getCompany().getComId();
        if (subscriptionContext.isExpired(current.getCompany())) {
            subscription.setStatus(com.th4.edge.tenant.billing.configure.SubStatus.Expired);
        }
        subscription.setRemainingDays(subscriptionContext.remainingDays(subscription));
        subscription.setUsedSeats(employeeRepository.countByCompany_ComId(comId));
        subscription.setUsedStores(storeRepository.countByCompany_ComId(comId));
        subscription.setEffectiveMaxSeats(subscriptionContext.effectiveMaxSeats(subscription));
        subscription.setEffectiveMaxStores(subscriptionContext.effectiveMaxStores(subscription));
        subscription.setWritable(subscriptionContext.isWritable(current.getCompany()));
        subscription.setExpired(subscriptionContext.isExpired(current.getCompany()));
        return subscription;
    }
}
