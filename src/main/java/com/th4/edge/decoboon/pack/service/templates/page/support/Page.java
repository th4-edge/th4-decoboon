package com.th4.edge.decoboon.pack.service.templates.page.support;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

/** 当前企业装修套餐分页实现。 */
@Note("装修套餐分页")
@Properties(allow = {
        @AllowProperties(model = PackageTemplate.class,
                properties = {"templateId", "name", "area", "price", "discount", "cost", "criterions", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("pack/template/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.pack.service.templates.page.Page {

    @User
    private Employee employee;

    private final PackageTemplateRepository giftPackRepository;
    private final TenantContext tenantContext;

    public Page(PackageTemplateRepository giftPackRepository, TenantContext tenantContext) {
        this.giftPackRepository = giftPackRepository;
        this.tenantContext = tenantContext;
    }

    @Override
    public org.springframework.data.domain.Page<PackageTemplate> call(PackageTemplate request) {
        Employee current = tenantContext.requireEmployee(employee);
        return new PageImpl<>(giftPackRepository
                .findByCompany_ComIdOrderByTemplateIdDesc(current.getCompany().getComId()));
    }
}
