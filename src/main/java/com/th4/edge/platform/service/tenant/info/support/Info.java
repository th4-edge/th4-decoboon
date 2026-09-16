package com.th4.edge.platform.service.tenant.info.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.platform.context.PlatformAdminContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Setting;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 平台企业详情实现。 */
@Note("平台企业详情")
@Properties(allow = {
        @AllowProperties(model = Company.class, properties = {
                "comId", "title", "name", "logo", "color", "size", "addr", "platform",
                "setting", "createTime", "planName", "subscriptionStatus", "serviceEndTime",
                "remainingDays", "usedSeats", "effectiveMaxSeats", "usedStores", "effectiveMaxStores"
        }),
        @AllowProperties(model = Setting.class, properties = {"enableStore"})
})
@Service("platform/tenant/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.platform.service.tenant.info.Info {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long companyId;

    private final PlatformAdminContext platformContext;

    public Info(PlatformAdminContext platformContext) {
        this.platformContext = platformContext;
    }

    @Override
    @Transactional(readOnly = true)
    public Company call(Company request) {
        platformContext.requireAdmin(employee);
        Company company = platformContext.requireTenant(companyId);
        platformContext.enrich(company);
        return company;
    }
}
