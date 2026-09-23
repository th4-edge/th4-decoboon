package com.th4.edge.decoboon.material.service.selections.item.info.support;

import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.model.MaterialSelectionItem;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 单个主材分项详情实现。 */
@Note("主材分项详情")
@Service("material/selection/item/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.material.service.selections.item.info.Info {

    @AutoInject @NotNull private Long projectId;
    @AutoInject @NotNull private Long contractLineId;
    @User private Employee employee;
    private final MaterialSelectionContext selectionContext;
    private final ProjectAccessContext projectAccessContext;

    public Info(MaterialSelectionContext selectionContext, ProjectAccessContext projectAccessContext) {
        this.selectionContext = selectionContext;
        this.projectAccessContext = projectAccessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialSelectionItem call(MaterialSelectionItem request) {
        Employee current = selectionContext.requireEmployee(employee);
        Project project = projectAccessContext.requireProject(projectId, current);
        return selectionContext.requireProjectItem(project, contractLineId, current);
    }
}
