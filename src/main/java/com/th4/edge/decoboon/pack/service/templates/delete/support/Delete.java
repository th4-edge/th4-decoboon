package com.th4.edge.decoboon.pack.service.templates.delete.support;

import com.th4.edge.decoboon.pack.context.PackageLifecycleContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 仅删除未被业务引用的最新套餐草稿。 */
@Note("套餐模板草稿删除")
@Description("删除未被业务引用的最新套餐草稿；已发布和历史版本永久保留")
@Service("pack/template/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.pack.service.templates.delete.Delete {

    @AutoInject
    @NotNull
    private Long templateId;

    @User
    private Employee employee;

    private final PackageLifecycleContext lifecycleContext;

    public Delete(PackageLifecycleContext lifecycleContext) {
        this.lifecycleContext = lifecycleContext;
    }

    @Override
    public Boolean call(PackageTemplate request) {
        lifecycleContext.deleteDraft(templateId, employee);
        return true;
    }
}
