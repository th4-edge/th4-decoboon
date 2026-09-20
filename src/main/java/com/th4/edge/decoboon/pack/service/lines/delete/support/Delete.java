package com.th4.edge.decoboon.pack.service.lines.delete.support;

import com.th4.edge.decoboon.pack.context.PackageConfigContext;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 套餐项目行删除实现。 */
@Note("套餐项目行删除")
@Service("pack/line/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.pack.service.lines.delete.Delete {

    @AutoInject
    @NotNull
    private Long itemId;

    @User
    private Employee employee;

    private final PackageConfigContext configContext;

    public Delete(PackageConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    public Boolean call(PackageLine request) {
        configContext.deleteLine(itemId, employee);
        return true;
    }
}
