package com.th4.edge.decoboon.project.service.tasks.info.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.model.TaskNode;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.project.repository.TaskNodeRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** 施工任务详情实现，附任务节点。 */
@Note("施工任务详情")
@Properties(allow = {
        @AllowProperties(model = ProjectTask.class,
                properties = {"taskId", "project", "stage", "catalogItem", "owner", "creator", "name",
                        "zone", "craft", "spec", "quantity", "unit", "status", "progress", "priority", "conceal",
                        "remark", "plannedStart", "plannedEnd", "actualStart", "actualEnd", "evidenceUrls", "nodes", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "name", "siteAddr" }),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = CatalogItem.class, properties = {"itemId", "code", "type", "name", "unit", "craft", "criterion"}),
        @AllowProperties(model = TaskNode.class,
                properties = {"nodeId", "nodeKey", "note", "medias", "confirmedBy", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/task/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.project.service.tasks.info.Info {

    @AutoInject
    @NotNull
    private Long taskId;

    @User
    private Employee employee;

    private final ProjectTaskRepository taskRepository;
    private final TaskNodeRepository nodeRepository;
    private final ProjectAccessContext accessContext;

    public Info(ProjectTaskRepository taskRepository,
                TaskNodeRepository nodeRepository,
                ProjectAccessContext accessContext) {
        this.taskRepository = taskRepository;
        this.nodeRepository = nodeRepository;
        this.accessContext = accessContext;
    }

    @Override
    public ProjectTask call(ProjectTask request) {
        ProjectTask task = taskRepository.findOne(((Specification<ProjectTask>) (root, query, builder) -> builder.equal(root.get("taskId"), taskId)).and(accessContext.visibleChild(employee, ProjectTask.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "施工任务不存在")));
        accessContext.requireVisible(task.getProject(), employee);
        task.setNodes(nodeRepository.findByTaskOrderByCreateTime_DateTimeAsc(task));
        return task;
    }
}
