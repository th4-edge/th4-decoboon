package com.th4.edge.decoboon.carryover.service.project.update.support;

import com.th4.edge.decoboon.carryover.configure.CarryoverProjectStatus;
import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.context.CarryoverProjectContext;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("更新承接项目")
@Properties(allow = @AllowProperties(model = CarryoverProject.class,
        properties = {"itemId", "status", "cancelledAt", "cancelReason"}))
@Service("carryover/project/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.carryover.service.project.update.Update {

    @User
    private Employee employee;

    @AutoInject @NotNull private Long itemId;
    @AutoInject @NotNull private CarryoverProjectStatus status;
    @AutoInject @Length(max = 500) private String reason;

    private final CarryoverAccessContext accessContext;
    private final CarryoverProjectContext projectContext;

    public Update(CarryoverAccessContext accessContext, CarryoverProjectContext projectContext) {
        this.accessContext = accessContext;
        this.projectContext = projectContext;
    }

    @Override
    @Transactional
    public CarryoverProject call(CarryoverProject request) {
        Employee current = accessContext.requireBoss(employee);
        if (status != CarryoverProjectStatus.Cancelled) {
            throw new DefinedException(new DefaultErrorCode("400", "当前仅支持撤回未提交的承接项目"));
        }
        return projectContext.withdraw(current, itemId, reason);
    }
}
