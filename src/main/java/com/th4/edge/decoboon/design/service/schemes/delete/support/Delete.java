package com.th4.edge.decoboon.design.service.schemes.delete.support;

import com.th4.edge.decoboon.design.context.DesignDeleteContext;
import com.th4.edge.decoboon.design.model.Design;
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

/** 设计方案删除实现。 */
@Note("设计方案删除")
@Properties(allow = {
        @AllowProperties(model = Design.class, properties = {"designId", "no", "name", "status"})
})
@Service("design/scheme/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.design.service.schemes.delete.Delete {

    @AutoInject
    @NotNull
    private Long designId;

    @User
    private Employee employee;

    private final DesignDeleteContext designDeleteContext;

    public Delete(DesignDeleteContext designDeleteContext) {
        this.designDeleteContext = designDeleteContext;
    }

    @Override
    public Design call(Design request) {
        return designDeleteContext.delete(designId, employee);
    }
}
