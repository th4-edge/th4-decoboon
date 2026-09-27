package com.th4.edge.platform.service.onboarding.service.save.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.integration.tenant.TenantDataAdapter;
import com.th4.edge.platform.model.PlatformOnboardingServiceStatus;
import com.th4.edge.platform.model.PlatformOnboardingServiceView;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformTenant;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.service.onboarding.PlatformOnboardingServiceContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 企业入驻服务记录保存实现。 */
@Note("企业入驻服务记录保存")
@Properties(allow = {
        @AllowProperties(model = PlatformOnboardingServiceView.class, properties = {
                "serviceId", "status", "contactResult", "nextAction", "nextFollowAt",
                "remark", "operatorName", "createTime"
        })
})
@Service("platform/tenant/onboarding/service/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.platform.service.onboarding.service.save.Save {
    @AutoInject @NotNull private Long companyId;
    @AutoInject @NotNull private PlatformOnboardingServiceStatus status;
    @AutoInject @Length(max = 500) private String contactResult;
    @AutoInject @NotEmpty @Length(max = 500) private String nextAction;
    @AutoInject @Length(max = 32) private String nextFollowAt;
    @AutoInject @Length(max = 1000) private String remark;
    @AutoInject @NotEmpty @Length(max = 200) private String reason;
    private final PlatformAccessContext accessContext;
    private final TenantDataAdapter tenantDataAdapter;
    private final PlatformOnboardingServiceContext serviceContext;

    public Save(PlatformAccessContext accessContext,
                TenantDataAdapter tenantDataAdapter,
                PlatformOnboardingServiceContext serviceContext) {
        this.accessContext = accessContext;
        this.tenantDataAdapter = tenantDataAdapter;
        this.serviceContext = serviceContext;
    }

    @Override
    @Transactional
    public PlatformOnboardingServiceView call(@User PlatformUser user) {
        PlatformUser operator = accessContext.requirePermission(user, PlatformPermission.TenantOnboardingServiceWrite);
        PlatformTenant company = tenantDataAdapter.tenant(companyId);
        return serviceContext.save(operator, companyId, company.getTitle(), status, trim(contactResult),
                nextAction.trim(), parse(nextFollowAt), trim(remark), reason.trim());
    }

    private LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value.trim());
        } catch (RuntimeException exception) {
            throw new DefinedException(new DefaultErrorCode("400", "下次跟进时间格式不正确"));
        }
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
