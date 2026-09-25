package com.th4.edge.decoboon.carryover.service.confirmation.save.support;

import com.th4.edge.decoboon.carryover.configure.CarryoverConfirmationRole;
import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.context.CarryoverProjectContext;
import com.th4.edge.decoboon.carryover.model.CarryoverConfirmation;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("保存承接确认")
@Properties(allow = {
        @AllowProperties(model = CarryoverConfirmation.class,
                properties = {"confirmationId", "requiredRole", "confirmedBy", "proxy", "confirmedVersion",
                        "confirmedAt", "summary", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.carryover.model.CarryoverProject.class,
                properties = {"itemId", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/confirmation/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.carryover.service.confirmation.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long itemId;

    @AutoInject
    @NotNull
    private CarryoverConfirmationRole requiredRole;

    @AutoInject
    @Length(max = 500)
    private String summary;

    private final CarryoverAccessContext accessContext;
    private final CarryoverProjectContext projectContext;

    public Save(CarryoverAccessContext accessContext, CarryoverProjectContext projectContext) {
        this.accessContext = accessContext;
        this.projectContext = projectContext;
    }

    @Override
    @Transactional
    public CarryoverConfirmation call(CarryoverConfirmation request) {
        Employee current = accessContext.requireEditor(employee);
        return projectContext.confirm(current, itemId, requiredRole, summary);
    }
}
