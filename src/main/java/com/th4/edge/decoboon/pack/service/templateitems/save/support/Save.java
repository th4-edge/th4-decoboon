package com.th4.edge.decoboon.pack.service.templateitems.save.support;

import com.th4.edge.decoboon.pack.context.PackageConfigContext;
import com.th4.edge.decoboon.pack.model.PackageTemplateItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 套餐差价项保存实现。 */
@Note("套餐差价项保存")
@Properties(allow = {
        @AllowProperties(model = PackageTemplateItem.class,
                properties = {"itemId", "name", "unit", "spaceTypes", "priority"})
})
@Service("pack/template-item/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.pack.service.templateitems.save.Save {

    @User
    private Employee employee;

    private final PackageConfigContext configContext;

    public Save(PackageConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    public PackageTemplateItem call(@ModelSaveInject PackageTemplateItem request) {
        return configContext.saveTemplateItem(request, employee);
    }
}
