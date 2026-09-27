package com.th4.edge.platform.service.onboarding.info.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.integration.tenant.TenantDataAdapter;
import com.th4.edge.platform.model.PlatformOnboardingServiceStatus;
import com.th4.edge.platform.model.PlatformOnboardingServiceView;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformTenantOnboarding;
import com.th4.edge.platform.model.PlatformTenantOwner;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.service.onboarding.PlatformOnboardingServiceContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 企业入驻台账详情实现。 */
@Note("企业入驻台账详情")
@Properties(allow = {
        @AllowProperties(model = PlatformTenantOnboarding.class, properties = {
                "comId", "companyTitle", "companyName", "companySize", "companyAddress",
                "companyCreateTime", "planName", "subscriptionStatus", "remainingDays",
                "ownerCount", "ownerEmpId", "ownerName", "ownerPhoneMasked", "ownerStatus", "ownerJoinedAt",
                "accountCreateTime", "lastLoginAt", "lastLoginType", "daysSinceLastLogin",
                "loginStatus", "businessStage", "serviceStatus", "lastServiceAt", "nextFollowAt",
                "nextAction", "owners", "serviceRecords"
        }),
        @AllowProperties(model = PlatformTenantOwner.class, properties = {
                "empId", "name", "phoneMasked", "status", "joinedAt", "accountCreateTime",
                "lastLoginAt", "lastLoginType"
        }),
        @AllowProperties(model = PlatformOnboardingServiceView.class, properties = {
                "serviceId", "status", "contactResult", "nextAction", "nextFollowAt",
                "remark", "operatorName", "createTime"
        })
})
@Service("platform/tenant/onboarding/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.platform.service.onboarding.info.Info {
    @AutoInject @NotNull private Long companyId;
    private final PlatformAccessContext accessContext;
    private final TenantDataAdapter tenantDataAdapter;
    private final PlatformOnboardingServiceContext serviceContext;

    public Info(PlatformAccessContext accessContext,
                TenantDataAdapter tenantDataAdapter,
                PlatformOnboardingServiceContext serviceContext) {
        this.accessContext = accessContext;
        this.tenantDataAdapter = tenantDataAdapter;
        this.serviceContext = serviceContext;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformTenantOnboarding call(@User PlatformUser user) {
        PlatformUser operator = accessContext.requirePermission(user, PlatformPermission.TenantOnboardingRead);
        PlatformTenantOnboarding result = tenantDataAdapter.onboarding(companyId);
        List<PlatformOnboardingServiceView> records = serviceContext.records(operator.getOrg().getOrgId(), companyId);
        result.setServiceRecords(records);
        if (!records.isEmpty()) {
            PlatformOnboardingServiceView latest = records.get(0);
            result.setServiceStatus(latest.getStatus());
            result.setLastServiceAt(latest.getCreateTime());
            result.setNextFollowAt(latest.getNextFollowAt());
            if (latest.getNextAction() != null && !latest.getNextAction().isBlank()) {
                result.setNextAction(latest.getNextAction());
            }
        } else {
            result.setServiceStatus(PlatformOnboardingServiceStatus.Pending);
        }
        return result;
    }
}
