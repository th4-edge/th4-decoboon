package com.th4.edge.decoboon.pack.service.templates.save.support;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.pack.context.PackageContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.transaction.annotation.Transactional;

/**
 * 装修套餐创建 Provider。
 *
 * <p>套餐保存前必须重新计算成本，避免客户端直接提交估算成本。</p>
 */
@Note("套餐创建")
@Description("创建 BIM 装修套餐并计算成本")
@Properties(allow = {
        @AllowProperties(model = PackageTemplate.class,
                properties = {"templateId", "name", "area", "price", "discount", "cost", "criterions", "createTime"})
})
@Service("pack/template/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.pack.service.templates.save.Save {

    @User
    private Employee employee;

    private final PackageTemplateRepository giftPackRepository;
    private final PackageContext giftPackContext;
    private final TenantContext tenantContext;

    public Save(PackageTemplateRepository giftPackRepository,
                PackageContext giftPackContext,
                TenantContext tenantContext) {
        this.giftPackRepository = giftPackRepository;
        this.giftPackContext = giftPackContext;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional
    public PackageTemplate call(PackageTemplate request) {
        Employee current = tenantContext.requireBoss(employee);
        request.setCompany(current.getCompany());
        request.setCost(giftPackContext.calculateCost(request));
        return giftPackRepository.save(request);
    }
}
