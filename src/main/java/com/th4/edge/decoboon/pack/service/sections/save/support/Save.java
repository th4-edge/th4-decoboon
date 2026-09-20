package com.th4.edge.decoboon.pack.service.sections.save.support;

import com.th4.edge.decoboon.pack.context.PackageConfigContext;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 套餐分区保存实现。 */
@Note("套餐分区保存")
@Properties(allow = {
        @AllowProperties(model = PackageSection.class,
                properties = {"groupId", "name", "spaceTypes", "maxDeleteNum", "priority"})
})
@Service("pack/section/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.pack.service.sections.save.Save {

    @User
    private Employee employee;

    private final PackageConfigContext configContext;

    public Save(PackageConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    public PackageSection call(@ModelSaveInject PackageSection request) {
        return configContext.saveSection(request, employee);
    }
}
