package com.th4.edge.decoboon.pack.service.templates.build.support;

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

/** 套餐下一版草稿创建实现。 */
@Note("套餐下一版创建")
@Properties(allow = @AllowProperties(model = PackageTemplate.class,
        properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price",
                "discount", "cost", "criterions", "createTime", "updateTime"}))
@Service("pack/template/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.pack.service.templates.build.Build {

    @AutoInject
    @NotNull
    private Long templateId;

    @User
    private Employee employee;

    private final PackageLifecycleContext lifecycleContext;

    public Build(PackageLifecycleContext lifecycleContext) {
        this.lifecycleContext = lifecycleContext;
    }

    @Override
    public PackageTemplate call(PackageTemplate request) {
        return lifecycleContext.buildNextVersion(templateId, employee);
    }
}
