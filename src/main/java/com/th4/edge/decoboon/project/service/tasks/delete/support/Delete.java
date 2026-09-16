package com.th4.edge.decoboon.project.service.tasks.delete.support;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

/** 删除尚在待办状态的施工任务。 */
@Note("施工任务删除")
@Properties(allow = {
        @AllowProperties(model = ProjectTask.class,
                properties = {"taskId", "project", "stage", "catalogItem", "owner", "creator", "name",
                        "zone", "craft", "spec", "quantity", "unit", "salePrice", "cost", "status", "progress",
                        "priority", "conceal", "remark", "plannedStart", "plannedEnd", "actualStart", "actualEnd",
                        "evidenceUrls", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr", "property" }),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = CatalogItem.class, properties = {"itemId", "type", "name", "unit"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/task/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.project.service.tasks.delete.Delete {

    @AutoInject
    @NotNull
    private Long taskId;

    @User
    private Employee employee;

    private final ProjectTaskRepository taskRepository;
    private final ProjectAccessContext accessContext;

    public Delete(ProjectTaskRepository taskRepository, ProjectAccessContext accessContext) {
        this.taskRepository = taskRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public ProjectTask call(ProjectTask request) {
        ProjectTask task = taskRepository.findOne(((Specification<ProjectTask>) (root, query, builder) -> builder.equal(root.get("taskId"), taskId)).and(accessContext.visibleChild(employee, ProjectTask.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "任务不存在")));
        accessContext.requirePlanEditor(task.getProject(), employee);
        if (task.getProgress() != null && task.getProgress() > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "已开工任务不能删除"));
        }
        taskRepository.delete(task);
        return task;
    }
}
