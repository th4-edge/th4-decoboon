package com.th4.edge.decoboon.project.service.tasks.update.support;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.carryover.configure.CarryoverBusinessType;
import com.th4.edge.decoboon.carryover.context.CarryoverLockContext;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 施工任务推进 Provider。 */
@Note("施工任务推进")
@Description("项目经理推进施工任务状态并保留现场证据")
@Properties(allow = {
        @AllowProperties(model = ProjectTask.class,
                properties = {"taskId", "project", "stage", "catalogItem", "owner", "creator", "name",
                        "zone", "craft", "spec", "quantity", "unit", "salePrice", "cost", "status", "progress",
                        "priority", "conceal", "remark", "plannedStart", "plannedEnd", "actualStart", "actualEnd",
                        "evidenceUrls", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr", "property" }),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = CatalogItem.class, properties = {"itemId", "type", "name", "unit"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/task/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.project.service.tasks.update.Update {

    @AutoInject
    @NotNull
    private Long taskId;

    @AutoInject
    private TaskStatus status;

    @AutoInject
    private String action;

    @AutoInject
    private List<String> evidenceUrls;

    @User
    private Employee employee;

    private final ProjectTaskRepository taskRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectContext projectContext;
    private final CarryoverLockContext carryoverLockContext;

    public Update(ProjectTaskRepository taskRepository,
                  ProjectAccessContext accessContext,
                  ProjectContext projectContext,
                  CarryoverLockContext carryoverLockContext) {
        this.taskRepository = taskRepository;
        this.accessContext = accessContext;
        this.projectContext = projectContext;
        this.carryoverLockContext = carryoverLockContext;
    }

    @Override
    @Transactional
    public ProjectTask call(ProjectTask request) {
        Employee current = accessContext.requireEmployee(employee);
        ProjectTask task = taskRepository.findOne(((Specification<ProjectTask>) (root, query, builder) -> builder.equal(root.get("taskId"), taskId)).and(accessContext.visibleChild(employee, ProjectTask.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "施工任务不存在")));
        accessContext.requireVisible(task.getProject(), current);
        // 施工任务推进：企业负责人 / 店长（限本门店数据范围）/ 项目经理
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Pm
                && current.getRole() != EmployeeRole.StoreManager) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人、店长或项目经理可以推进施工任务"));
        }
        TaskStatus target = status != null ? status : statusOf(action);
        if (target == null) {
            throw new DefinedException(new DefaultErrorCode("400", "任务状态或动作不能为空"));
        }
        boolean reopen = "reopen".equalsIgnoreCase(action);
        task.setStatus(target);
        if (target == TaskStatus.Doing) {
            task.setProgress(nextProgress(target, task.getProgress(), reopen));
            if (task.getActualStart() == null) task.setActualStart(LocalDate.now());
        } else if (target == TaskStatus.Accepting) {
            task.setProgress(nextProgress(target, task.getProgress(), false));
            if (Boolean.TRUE.equals(task.getConceal())) {
                throw new DefinedException(new DefaultErrorCode("400", "隐蔽任务请先通过节点留证确认"));
            }
        } else if (target == TaskStatus.Done) {
            if (evidenceUrls == null || evidenceUrls.isEmpty()) {
                throw new DefinedException(new DefaultErrorCode("400", "任务验收前必须上传现场照片或视频"));
            }
            task.setProgress(nextProgress(target, task.getProgress(), false));
            task.setActualEnd(LocalDate.now());
        } else if (target == TaskStatus.Todo) {
            task.setProgress(nextProgress(target, task.getProgress(), false));
        }
        if (evidenceUrls != null) {
            task.setEvidenceUrls(new ArrayList<>(evidenceUrls));
        }
        ProjectTask saved = taskRepository.save(task);
        projectContext.refreshProject(task.getProject());
        if (carryoverLockContext != null) carryoverLockContext.lock(task.getProject(), CarryoverBusinessType.Task, current);
        return safeCopy(saved);
    }

    /** 返回安全副本，避免负责人/创建人完整实体图递归序列化。 */
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

    static TaskStatus statusOf(String value) {
        if (value == null) return null;
        return switch (value.toLowerCase()) {
            case "start", "doing", "reopen" -> TaskStatus.Doing;
            case "finish", "accepting" -> TaskStatus.Accepting;
            case "done" -> TaskStatus.Done;
            case "todo" -> TaskStatus.Todo;
            default -> null;
        };
    }

    static int nextProgress(TaskStatus target, int current, boolean reopen) {
        if (target == TaskStatus.Doing) return reopen ? 10 : Math.max(current, 10);
        if (target == TaskStatus.Accepting) return 90;
        if (target == TaskStatus.Done) return 100;
        if (target == TaskStatus.Todo) return 0;
        return current;
    }
}
