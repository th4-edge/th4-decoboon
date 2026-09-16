package com.th4.edge.platform.service.subscription.update.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.platform.context.PlatformAdminContext;
import com.th4.edge.platform.model.PlatformAction;
import com.th4.edge.tenant.billing.configure.SubStatus;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Plan;
import com.th4.edge.tenant.billing.model.PlanVersion;
import com.th4.edge.tenant.billing.model.Subscription;
import com.th4.edge.tenant.billing.repository.PlanRepository;
import com.th4.edge.tenant.billing.repository.PlanVersionRepository;
import com.th4.edge.tenant.billing.repository.SubscriptionRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.framework.provider.validation.annotation.Gt;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** 平台人工调整订阅实现。 */
@Note("平台人工调整订阅")
@Properties(allow = {
        @AllowProperties(model = Subscription.class, properties = {
                "subId", "company", "plan", "planVersion", "status", "serviceEndTime",
                "trialActivated", "lastRenewTime", "extraSeats", "extraStores", "remainingDays",
                "usedSeats", "usedStores", "effectiveMaxSeats", "effectiveMaxStores", "writable",
                "expired", "createTime"
        }),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name"}),
        @AllowProperties(model = Plan.class, properties = {"planId", "code", "name"}),
        @AllowProperties(model = PlanVersion.class, properties = {
                "versionId", "versionNo", "planCode", "planName", "days", "maxStores", "maxSeats", "modules", "price"
        })
})
@Service("platform/subscription/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.platform.service.subscription.update.Update {

    @AutoInject
    @NotNull
    private Long subId;
    @AutoInject
    @Gt("0")
    private Integer days;
    @AutoInject
    @Length(max = 32)
    private String planCode;
    @AutoInject
    private SubStatus status;
    @AutoInject
    @Gt("-1")
    private Integer extraSeats;
    @AutoInject
    @Gt("-1")
    private Integer extraStores;
    @AutoInject
    @NotEmpty
    @Length(max = 500)
    private String reason;

    private final PlatformAdminContext platformContext;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PlanVersionRepository planVersionRepository;
    private final SubscriptionContext subscriptionContext;

    public Update(PlatformAdminContext platformContext,
                  SubscriptionRepository subscriptionRepository,
                  PlanRepository planRepository,
                  PlanVersionRepository planVersionRepository,
                  SubscriptionContext subscriptionContext) {
        this.platformContext = platformContext;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.planVersionRepository = planVersionRepository;
        this.subscriptionContext = subscriptionContext;
    }

    @Override
    @Transactional
    public Subscription call(@User Employee employee) {
        Employee operator = platformContext.requireAdmin(employee);
        subscriptionContext.initializePlans();
        Subscription subscription = subscriptionRepository.findBySubIdForUpdate(subId);
        if (subscription == null || subscription.getCompany() == null
                || Boolean.TRUE.equals(subscription.getCompany().getPlatform())) {
            throw new DefinedException(new DefaultErrorCode("404", "订阅不存在"));
        }
        if (days == null && (planCode == null || planCode.isBlank()) && status == null
                && extraSeats == null && extraStores == null) {
            throw new DefinedException(new DefaultErrorCode("400", "至少提交一项订阅调整"));
        }

        Map<String, Object> before = platformContext.subscriptionSnapshot(subscription);
        PlatformAction action = resolveAction();
        if (days != null) {
            LocalDateTime base = subscription.getServiceEndTime() != null
                    && subscription.getServiceEndTime().toLocalDateTime() != null
                    && subscription.getServiceEndTime().toLocalDateTime().isAfter(LocalDateTime.now())
                    ? subscription.getServiceEndTime().toLocalDateTime() : LocalDateTime.now();
            subscription.setServiceEndTime(new DateTime(base.plusDays(days)));
            subscription.setLastRenewTime(LocalDateTime.now());
            if (subscription.getStatus() == null || subscription.getStatus() == SubStatus.Expired) {
                subscription.setStatus(SubStatus.Active);
            }
        }
        if (planCode != null && !planCode.isBlank()) {
            Plan plan = planRepository.findByCode(planCode.trim());
            if (plan == null || !Boolean.TRUE.equals(plan.getEnabled())) {
                throw new DefinedException(new DefaultErrorCode("400", "套餐不存在或已下架"));
            }
            PlanVersion version = planVersionRepository.findTopByPlan_PlanIdOrderByVersionNoDesc(plan.getPlanId());
            if (version == null) {
                throw new DefinedException(new DefaultErrorCode("400", "套餐版本不存在"));
            }
            subscription.setPlan(plan);
            subscription.setPlanVersion(version);
        }
        if (extraSeats != null) subscription.setExtraSeats(extraSeats);
        if (extraStores != null) subscription.setExtraStores(extraStores);
        if (status != null) {
            if (status == SubStatus.Active && !hasFutureService(subscription)) {
                throw new DefinedException(new DefaultErrorCode("400", "恢复为使用中前必须确保服务期未到期"));
            }
            subscription.setStatus(status);
            if (status == SubStatus.Frozen) action = PlatformAction.Freeze;
            if (status == SubStatus.Active) action = PlatformAction.Unfreeze;
        }

        Subscription saved = subscriptionRepository.save(subscription);
        platformContext.enrich(saved);
        platformContext.audit(operator, saved.getCompany(), action, "Subscription",
                String.valueOf(saved.getSubId()), "人工调整企业订阅", before,
                platformContext.subscriptionSnapshot(saved), reason.trim());
        return saved;
    }

    private PlatformAction resolveAction() {
        if (days != null) return PlatformAction.Renew;
        if (planCode != null && !planCode.isBlank()) return PlatformAction.PlanChange;
        if (extraSeats != null) return PlatformAction.SeatAdjust;
        if (extraStores != null) return PlatformAction.StoreAdjust;
        return PlatformAction.StatusChange;
    }

    private boolean hasFutureService(Subscription subscription) {
        return subscription.getServiceEndTime() != null
                && subscription.getServiceEndTime().toLocalDateTime() != null
                && subscription.getServiceEndTime().toLocalDateTime().isAfter(LocalDateTime.now());
    }
}
