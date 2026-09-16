package com.th4.edge.decoboon.project.service.tasks.page.support;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 施工任务分页实现。 */
@Note("施工任务分页")
@Properties(allow = {
        @AllowProperties(model = ProjectTask.class,
                properties = {"taskId", "project", "stage", "catalogItem", "owner", "creator", "name",
                        "zone", "craft", "spec", "quantity", "unit", "status", "progress", "priority", "conceal",
                        "remark", "plannedStart", "plannedEnd", "actualStart", "actualEnd", "evidenceUrls", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class,
                properties = {"projectId", "name", "siteAddr"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.ProjectStage.class,
                properties = {"stageId", "name"}),
        @AllowProperties(model = com.th4.edge.decoboon.catalog.model.CatalogItem.class,
                properties = {"itemId", "code", "type", "name", "unit", "craft", "spec"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/task/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.tasks.page.Page {

    @AutoInject
    private Long projectId;

    @AutoInject
    private Long stageId;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectAccessContext accessContext;

    public Page(ProjectRepository projectRepository,
                ProjectTaskRepository taskRepository,
                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<ProjectTask> call(ProjectTask request) {
        Employee current = accessContext.requireEmployee(employee);
        var project = accessContext.requireProject(projectId, current);
        List<ProjectTask> tasks = taskRepository.findByProject(project).stream()
                .filter(task -> stageId == null || task.getStage().getStageId().equals(stageId))
                .sorted(Comparator.comparing(ProjectTask::getTaskId))
                .map(this::safeCopy)
                .toList();
        return new PageImpl<>(tasks);
    }

    /** 页面返回安全副本，关联人员仅保留最小字段，避免实体图递归序列化。 */
    private ProjectTask safeCopy(ProjectTask source) {
        ProjectTask copy = new ProjectTask();
        BeanUtils.copyProperties(source, copy, "owner", "creator");
        copy.setOwner(minimalEmployee(source.getOwner()));
        copy.setCreator(minimalEmployee(source.getCreator()));
        return copy;
    }

    private Employee minimalEmployee(Employee source) {
        if (source == null) return null;
        Employee employee = new Employee();
        employee.setEmpId(source.getEmpId());
        employee.setName(source.getName());
        employee.setRole(source.getRole());
        return employee;
    }
}
