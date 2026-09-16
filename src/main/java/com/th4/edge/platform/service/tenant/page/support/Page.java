package com.th4.edge.platform.service.tenant.page.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.platform.context.PlatformAdminContext;
import com.th4.edge.tenant.billing.configure.SubStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Setting;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 平台企业分页实现。 */
@Note("平台企业分页")
@Properties(allow = {
        @AllowProperties(model = Company.class, properties = {
                "comId", "title", "name", "logo", "color", "size", "addr", "platform",
                "setting", "createTime", "planName", "subscriptionStatus", "serviceEndTime",
                "remainingDays", "usedSeats", "effectiveMaxSeats", "usedStores", "effectiveMaxStores"
        }),
        @AllowProperties(model = Setting.class, properties = {"enableStore"})
})
@Service("platform/tenant/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.platform.service.tenant.page.Page {

    @User
    private Employee employee;
    @AutoInject
    @Length(max = 64)
    private String keyword;
    @AutoInject
    private SubStatus status;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;

    private final PlatformAdminContext platformContext;
    private final CompanyRepository companyRepository;

    public Page(PlatformAdminContext platformContext, CompanyRepository companyRepository) {
        this.platformContext = platformContext;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Company> call(Company request) {
        platformContext.requireAdmin(employee);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        org.springframework.data.domain.Page<Company> result = companyRepository.pagePlatformTenants(
                normalizedKeyword, status, PageRequest.of(page, pageSize));
        result.forEach(platformContext::enrich);
        return result;
    }
}
