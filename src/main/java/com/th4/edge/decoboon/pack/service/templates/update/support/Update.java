package com.th4.edge.decoboon.pack.service.templates.update.support;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.pack.context.PackageContext;
import com.th4.edge.decoboon.pack.context.PackageLifecycleContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.security.annotation.User;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.transaction.annotation.Transactional;

/**
 * 装修套餐更新 Provider。
 *
 * <p>更新套餐或增删分项后重新计算成本，确保套餐报价和成本口径一致。</p>
 */
@Note("套餐更新")
@Description("更新 BIM 套餐并重新计算成本")
@Properties(allow = {
        @AllowProperties(model = PackageTemplate.class,
                properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price",
                        "discount", "cost", "criterions", "createTime"})
})
@Service("pack/template/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.pack.service.templates.update.Update {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long templateId;

    @AutoInject
    @NotEmpty
    private String name;

    @AutoInject
    @NotNull
    private java.math.BigDecimal area;

    @AutoInject
    @NotNull
    private java.math.BigDecimal price;

    @AutoInject
    private java.math.BigDecimal discount;

    private final PackageTemplateRepository giftPackRepository;
    private final PackageContext giftPackContext;
    private final TenantContext tenantContext;
    private final PackageLifecycleContext lifecycleContext;

    public Update(PackageTemplateRepository giftPackRepository, PackageContext giftPackContext,
                  TenantContext tenantContext, PackageLifecycleContext lifecycleContext) {
        this.giftPackRepository = giftPackRepository;
        this.giftPackContext = giftPackContext;
        this.tenantContext = tenantContext;
        this.lifecycleContext = lifecycleContext;
    }

    @Override
    @Transactional
    public PackageTemplate call(@ModelSaveInject PackageTemplate request) {
        PackageTemplate pack = lifecycleContext.requireDraft(templateId, employee);
        pack.setName(name);
        pack.setArea(area);
        pack.setPrice(price);
        pack.setDiscount(discount == null ? pack.getDiscount() : discount);
        if (request.getItems() != null) pack.setItems(request.getItems());
        if (request.getMeasures() != null) pack.setMeasures(request.getMeasures());
        if (request.getCriterions() != null) pack.setCriterions(request.getCriterions());
        pack.setCost(giftPackContext.calculateCost(pack));
        return giftPackRepository.save(pack);
    }
}
