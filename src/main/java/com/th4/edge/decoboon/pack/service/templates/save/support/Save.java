package com.th4.edge.decoboon.pack.service.templates.save.support;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.pack.context.PackageLifecycleContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
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
                properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price",
                        "discount", "cost", "criterions", "createTime"})
})
@Service("pack/template/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.pack.service.templates.save.Save {

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

    @User
    private Employee employee;

    private final PackageLifecycleContext lifecycleContext;

    public Save(PackageLifecycleContext lifecycleContext) {
        this.lifecycleContext = lifecycleContext;
    }

    @Override
    @Transactional
    public PackageTemplate call(@ModelSaveInject PackageTemplate request) {
        return lifecycleContext.createDraft(request, name, area, price, discount, employee);
    }
}
