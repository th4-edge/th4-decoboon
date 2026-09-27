package com.th4.edge.platform.service.onboarding.reveal.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.integration.tenant.TenantDataAdapter;
import com.th4.edge.platform.model.PlatformAction;
import com.th4.edge.platform.model.PlatformOperationLog;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformTenantOwnerContact;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.repository.PlatformOperationLogRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 企业负责人手机号揭示实现。 */
@Note("企业负责人手机号揭示")
@Properties(allow = {
        @AllowProperties(model = PlatformTenantOwnerContact.class, properties = {
                "companyId", "empId", "ownerName", "phone"
        })
})
@Service("platform/tenant/onboarding/reveal")
@Scope(ScopeTypes.PROTOTYPE)
public class Reveal implements com.th4.edge.platform.service.onboarding.reveal.Reveal {
    @AutoInject @NotNull private Long companyId;
    @AutoInject @NotNull private Long empId;
    @AutoInject @NotEmpty @Length(max = 200) private String reason;
    private final PlatformAccessContext accessContext;
    private final TenantDataAdapter tenantDataAdapter;
    private final PlatformOperationLogRepository operationLogRepository;

    public Reveal(PlatformAccessContext accessContext,
                  TenantDataAdapter tenantDataAdapter,
                  PlatformOperationLogRepository operationLogRepository) {
        this.accessContext = accessContext;
        this.tenantDataAdapter = tenantDataAdapter;
        this.operationLogRepository = operationLogRepository;
    }

    @Override
    @Transactional
    public PlatformTenantOwnerContact call(@User PlatformUser user) {
        PlatformUser operator = accessContext.requirePermission(user, PlatformPermission.TenantOwnerPhoneReveal);
        PlatformTenantOwnerContact contact = tenantDataAdapter.ownerContact(companyId, empId);
        PlatformOperationLog log = new PlatformOperationLog();
        log.setOrg(operator.getOrg());
        log.setOperator(operator);
        log.setAction(PlatformAction.OwnerPhoneReveal);
        log.setTargetType("Employee");
        log.setTargetKey(String.valueOf(empId));
        log.setTargetComId(companyId);
        log.setTargetCompanyTitle(tenantDataAdapter.tenant(companyId).getTitle());
        log.setSummary("查看企业负责人手机号");
        log.setBeforeValue("手机号=已掩码");
        log.setAfterValue("手机号=已揭示");
        log.setReason(reason.trim());
        operationLogRepository.save(log);
        return contact;
    }
}
