package com.th4.edge.decoboon.pack.service.templates.disable.support;

import com.th4.edge.decoboon.pack.context.PackageLifecycleContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 套餐模板停用实现。 */
@Note("套餐模板停用")
@Properties(allow = @AllowProperties(model = PackageTemplate.class,
        properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price",
                "discount", "cost", "publishedAt", "snapshotHash", "criterions", "createTime", "updateTime"}))
@Service("pack/template/disable")
@Scope(ScopeTypes.PROTOTYPE)
public class Disable implements com.th4.edge.decoboon.pack.service.templates.disable.Disable {

    @AutoInject
    @NotNull
    private Long templateId;

    @User
    private Employee employee;

    private final PackageLifecycleContext lifecycleContext;

    public Disable(PackageLifecycleContext lifecycleContext) {
        this.lifecycleContext = lifecycleContext;
    }

    @Override
    public PackageTemplate call(PackageTemplate request) {
        return lifecycleContext.disable(templateId, employee);
    }
}
