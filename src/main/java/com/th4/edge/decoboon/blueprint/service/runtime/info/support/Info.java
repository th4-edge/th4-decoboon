package com.th4.edge.decoboon.blueprint.service.runtime.info.support;

import com.th4.edge.decoboon.blueprint.context.BlueprintRuntimeContext;
import com.th4.edge.decoboon.blueprint.model.BlueprintRuntime;
import com.th4.edge.decoboon.blueprint.model.BlueprintRuntimeQuery;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 业务蓝图运行态实现。 */
@Note("业务蓝图运行态")
@Properties(allow = @AllowProperties(model = BlueprintRuntime.class,
        properties = {"id", "title", "version", "status", "lastReviewedCommit", "sourceRefCount",
                "viewerRole", "costVisible", "blueprintJson"}))
@Service("blueprint/runtime/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.blueprint.service.runtime.info.Info {

    @User
    private Employee employee;

    @AutoInject
    private String blueprintId;

    private final BlueprintRuntimeContext runtimeContext;

    public Info(BlueprintRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    @Override
    public BlueprintRuntime call(BlueprintRuntimeQuery request) {
        return runtimeContext.load(employee, blueprintId);
    }
}
