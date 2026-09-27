package com.th4.edge.platform.service.onboarding.page.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.integration.tenant.TenantDataAdapter;
import com.th4.edge.platform.model.PlatformLoginFreshness;
import com.th4.edge.platform.model.PlatformOnboardingServiceStatus;
import com.th4.edge.platform.model.PlatformOnboardingServiceView;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformTenantOnboarding;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.service.onboarding.PlatformOnboardingServiceContext;
import com.th4.edge.tenant.billing.configure.SubStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 企业入驻台账分页实现。 */
@Note("企业入驻台账分页")
@Properties(allow = {
        @AllowProperties(model = PlatformTenantOnboarding.class, properties = {
                "comId", "companyTitle", "companyName", "companySize", "companyAddress",
                "companyCreateTime", "planName", "subscriptionStatus", "remainingDays",
                "ownerCount", "ownerEmpId", "ownerName", "ownerPhoneMasked", "ownerStatus", "ownerJoinedAt",
                "accountCreateTime", "lastLoginAt", "lastLoginType", "daysSinceLastLogin",
                "loginStatus", "businessStage", "serviceStatus", "lastServiceAt", "nextFollowAt", "nextAction"
        })
})
@Service("platform/tenant/onboarding/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.platform.service.onboarding.page.Page {
    @AutoInject private String keyword;
    @AutoInject private SubStatus status;
    @AutoInject private PlatformLoginFreshness loginFreshness;
    @AutoInject private PlatformOnboardingServiceStatus serviceStatus;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final PlatformAccessContext accessContext;
    private final TenantDataAdapter tenantDataAdapter;
    private final PlatformOnboardingServiceContext serviceContext;

    public Page(PlatformAccessContext accessContext,
                TenantDataAdapter tenantDataAdapter,
                PlatformOnboardingServiceContext serviceContext) {
        this.accessContext = accessContext;
        this.tenantDataAdapter = tenantDataAdapter;
        this.serviceContext = serviceContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PlatformTenantOnboarding> call(@User PlatformUser user) {
        PlatformUser operator = accessContext.requirePermission(user, PlatformPermission.TenantOnboardingRead);
        List<PlatformTenantOnboarding> all = tenantDataAdapter.onboarding(keyword, status);
        Map<Long, PlatformOnboardingServiceView> latest = serviceContext.latest(
                operator.getOrg().getOrgId(), all.stream().map(PlatformTenantOnboarding::getComId).toList());
        List<PlatformTenantOnboarding> filtered = all.stream()
                .map(item -> applyService(item, latest.get(item.getComId())))
                .filter(item -> serviceStatus == null || item.getServiceStatus() == serviceStatus)
                .filter(item -> matchesLogin(item, loginFreshness))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = Math.min(page * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        return new PageImpl<>(filtered.subList(from, to), PageRequest.of(page, pageSize), filtered.size());
    }

    private PlatformTenantOnboarding applyService(PlatformTenantOnboarding item, PlatformOnboardingServiceView service) {
        if (service == null) {
            item.setServiceStatus(PlatformOnboardingServiceStatus.Pending);
            return item;
        }
        item.setServiceStatus(service.getStatus());
        item.setLastServiceAt(service.getCreateTime());
        item.setNextFollowAt(service.getNextFollowAt());
        if (service.getNextAction() != null && !service.getNextAction().isBlank()) {
            item.setNextAction(service.getNextAction());
        }
        return item;
    }

    private boolean matchesLogin(PlatformTenantOnboarding item, PlatformLoginFreshness filter) {
        if (filter == null || filter == PlatformLoginFreshness.All) return true;
        LocalDateTime lastLogin = item.getLastLoginAt();
        if (filter == PlatformLoginFreshness.Never) return lastLogin == null;
        if (lastLogin == null) return false;
        LocalDate today = LocalDate.now();
        long days = Math.max(0L, java.time.temporal.ChronoUnit.DAYS.between(lastLogin.toLocalDate(), today));
        return switch (filter) {
            case Today -> days == 0;
            case Within7Days -> days < 7;
            case Inactive7Days -> days >= 7;
            case Inactive30Days -> days >= 30;
            default -> true;
        };
    }
}
