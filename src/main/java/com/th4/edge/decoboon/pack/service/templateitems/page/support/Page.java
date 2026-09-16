package com.th4.edge.decoboon.pack.service.templateitems.page.support;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.pack.model.PackageTemplateItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.service.annotation.ServiceName;
import com.th4.edge.framework.service.page.context.AbstractPage;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Scope;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Service;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;

/**
 * 套餐差项分页实现。
 *
 * <p>使用框架 Criteria 能力支持前端统一查询语法，并保留 BIM 主数据的分页读取能力。</p>
 */
@Note("套餐差项分页")
@Properties(allow = {
        @AllowProperties(model = PackageTemplateItem.class,
                properties = {"itemId", "name", "unit", "spaceTypes", "priority", "createTime"})
})
@Service("pack/template-item/page")
@ServiceName("page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page extends AbstractPage<PackageTemplateItem> implements com.th4.edge.decoboon.pack.service.templateitems.page.Page {

    @User
    private Employee employee;
    private final TenantContext tenantContext;

    @Override
    public org.springframework.data.domain.Page<PackageTemplateItem> call(PackageTemplateItem request) {
        request.setCompany(tenantContext.requireCompany(employee));
        return super.call(request);
    }

    public Page(EntityManager entityManager, TenantContext tenantContext) {
        super(entityManager);
        this.tenantContext = tenantContext;
    }
}
