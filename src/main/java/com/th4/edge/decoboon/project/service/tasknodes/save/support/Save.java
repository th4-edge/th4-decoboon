package com.th4.edge.decoboon.project.service.tasknodes.save.support;

import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.configure.TaskNodeKey;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.model.TaskNode;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
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
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 按施工节点留证并推进任务状态。 */
@Note("任务节点确认")
@Properties(allow = {
        @AllowProperties(model = TaskNode.class,
                properties = {"nodeId", "task", "nodeKey", "note", "medias", "confirmedBy", "createTime"}),
        @AllowProperties(model = ProjectTask.class,
                properties = {"taskId", "name", "status", "progress", "evidenceUrls", "actualStart", "actualEnd"})
})
@Service("project/task-node/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.tasknodes.save.Save {

    @AutoInject
    @NotNull
    private Long taskId;

    @AutoInject
    @NotNull
    private String key;

    @AutoInject
    private String note;

    @AutoInject
    private List<Map<String, Object>> medias;

    @User
    private Employee employee;

    private final ProjectTaskRepository taskRepository;
    private final TaskNodeRepository nodeRepository;
    private final ProjectReportRepository reportRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectContext projectContext;
    private final ProjectPermissionContext permissionContext;

    public Save(ProjectTaskRepository taskRepository,
                TaskNodeRepository nodeRepository,
                ProjectReportRepository reportRepository,
                ProjectAccessContext accessContext,
                ProjectContext projectContext,
                ProjectPermissionContext permissionContext) {
        this.taskRepository = taskRepository;
        this.nodeRepository = nodeRepository;
        this.reportRepository = reportRepository;
        this.accessContext = accessContext;
        this.projectContext = projectContext;
        this.permissionContext = permissionContext;
    }

    @Override
    @Transactional
    public TaskNode call(TaskNode request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<ProjectTask> byId = (root, query, builder) -> builder.equal(root.get("taskId"), taskId);
        ProjectTask task = taskRepository.findOne(
                        byId.and(accessContext.visibleChild(employee, ProjectTask.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "施工任务不存在或无权访问")));
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Pm) {
            throw new DefinedException(new DefaultErrorCode("403", "只有项目经理或企业负责人可以确认施工节点"));
        }
        TaskNodeKey nodeKey = parseKey(key);
        if (nodeRepository.findByTaskAndNodeKey(task, nodeKey).isPresent()) {
            throw new DefinedException(new DefaultErrorCode("400", "该施工节点已经确认"));
        }
        List<String> urls = mediaUrls();
        if (urls.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "施工节点必须上传照片或视频"));
        }

        if (nodeKey == TaskNodeKey.Start) {
            if (task.getStatus() != TaskStatus.Todo) {
                throw new DefinedException(new DefaultErrorCode("400", "只有未开始任务可以确认开工"));
            }
            task.setStatus(TaskStatus.Doing);
            task.setProgress(Math.max(task.getProgress(), 10));
            task.setActualStart(LocalDate.now());
        } else if (nodeKey == TaskNodeKey.Conceal) {
            if (task.getStatus() != TaskStatus.Doing || !Boolean.TRUE.equals(task.getConceal())) {
                throw new DefinedException(new DefaultErrorCode("400", "当前任务没有可确认的隐蔽节点或任务未开工"));
            }
            task.setProgress(Math.max(task.getProgress(), 55));
        } else {
            if (task.getStatus() != TaskStatus.Doing && task.getStatus() != TaskStatus.Accepting) {
                throw new DefinedException(new DefaultErrorCode("400", "任务尚未开工，不能确认完工"));
            }
            if (Boolean.TRUE.equals(task.getConceal())
                    && nodeRepository.findByTaskAndNodeKey(task, TaskNodeKey.Conceal).isEmpty()) {
                throw new DefinedException(new DefaultErrorCode("400", "隐蔽工程确认后才能完成施工"));
            }
            task.setStatus(TaskStatus.Accepting);
            task.setProgress(90);
        }
        List<String> evidence = task.getEvidenceUrls() == null
                ? new ArrayList<>() : new ArrayList<>(task.getEvidenceUrls());
        evidence.addAll(urls);
        task.setEvidenceUrls(evidence);
        taskRepository.save(task);
        projectContext.refreshProject(task.getProject());

        TaskNode node = new TaskNode();
        node.setCompany(task.getCompany());
        node.setTask(task);
        permissionContext.apply(node, task.getProject());
        node.setNodeKey(nodeKey);
        node.setNote(note);
        node.setMedias(urls);
        node.setConfirmedBy(current);
        TaskNode saved = nodeRepository.save(node);

        ProjectReport report = new ProjectReport();
        report.setCompany(task.getCompany());
        report.setProject(task.getProject());
        report.setProperty(task.getProject().getProperty());
        permissionContext.apply(report, task.getProject());
        report.setStage(task.getStage());
        report.setTask(task);
        report.setType(ReportType.Progress);
        report.setTitle(nodeKey.getName() + " · " + task.getName());
        report.setContent(note == null || note.isBlank() ? nodeKey.getName() + "已影像确认" : note);
        report.setImages(urls);
        report.setAuthor(current);
        report.setAuthorRole(current.getRole());
        report.setVisibleToCustomer(true);
        reportRepository.save(report);
        return saved;
    }

    private TaskNodeKey parseKey(String value) {
        if (value == null) throw new DefinedException(new DefaultErrorCode("400", "节点类型不能为空"));
        for (TaskNodeKey item : TaskNodeKey.values()) {
            if (item.getValue().equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value)) return item;
        }
        throw new DefinedException(new DefaultErrorCode("400", "不支持的施工节点"));
    }

    private List<String> mediaUrls() {
        if (medias == null) return List.of();
        List<String> urls = new ArrayList<>();
        for (Map<String, Object> media : medias) {
            Object url = media.get("url");
            if (url != null && !String.valueOf(url).isBlank()) urls.add(String.valueOf(url));
        }
        return urls;
    }
}
