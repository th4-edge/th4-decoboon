package com.th4.edge.decoboon.project.service.reports.save.support;
import com.th4.edge.decoboon.project.model.ProjectStage;

import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.carryover.configure.CarryoverBusinessType;
import com.th4.edge.decoboon.carryover.context.CarryoverLockContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 发布工地动态实现。 */
@Note("发布工地动态")
@Properties(allow = {
        @AllowProperties(model = ProjectReport.class,
                properties = {"reportId", "project", "stage", "task", "type", "title", "content",
                        "change", "images", "author", "authorRole", "visibleToCustomer", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class,
                properties = {"projectId", "name", "siteAddr"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = ProjectTask.class, properties = {"taskId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/report/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.reports.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long projectId;

    @AutoInject
    private Long stageId;

    @AutoInject
    private Long taskId;

    @AutoInject
    private ReportType type;

    @AutoInject
    @Length(max = 128)
    private String title;

    @AutoInject
    @NotEmpty
    @Length(max = 1024)
    private String content;

    @AutoInject
    private java.util.List<String> images;

    @AutoInject
    private Boolean visibleToCustomer;

    private final ProjectRepository projectRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectReportRepository reportRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectPermissionContext permissionContext;
    private final CarryoverLockContext carryoverLockContext;

    public Save(ProjectRepository projectRepository,
                ProjectStageRepository stageRepository,
                ProjectTaskRepository taskRepository,
                ProjectReportRepository reportRepository,
                ProjectAccessContext accessContext,
                ProjectPermissionContext permissionContext,
                CarryoverLockContext carryoverLockContext) {
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.reportRepository = reportRepository;
        this.accessContext = accessContext;
        this.permissionContext = permissionContext;
        this.carryoverLockContext = carryoverLockContext;
    }

    @Override
    @Transactional
    public ProjectReport call(ProjectReport request) {
        Employee current = accessContext.requireEmployee(employee);
        var project = accessContext.requireProject(projectId, current);

        ProjectReport report = new ProjectReport();
        report.setCompany(project.getCompany());
        report.setProject(project);
        report.setProperty(project.getProperty());
        permissionContext.apply(report, project);
        if (stageId != null) {
            Specification<ProjectStage> stageById = (root, query, builder) ->
                    builder.equal(root.get("stageId"), stageId);
            report.setStage(stageRepository.findOne(stageById.and((root, query, builder) ->
                            builder.equal(root.get("project").get("projectId"), projectId)))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "阶段不存在或不属于当前项目"))));
        }
        if (taskId != null) {
            Specification<ProjectTask> taskById = (root, query, builder) ->
                    builder.equal(root.get("taskId"), taskId);
            ProjectTask task = taskRepository.findOne(taskById.and((root, query, builder) ->
                            builder.equal(root.get("project").get("projectId"), projectId)))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "任务不存在或不属于当前项目")));
            report.setTask(task);
            if (report.getStage() == null) report.setStage(task.getStage());
        }
        report.setType(type == null ? ReportType.Progress : type);
        report.setTitle(title == null || title.isBlank() ? defaultTitle(report.getType()) : title);
        report.setContent(content);
        report.setImages(images == null ? null : new java.util.ArrayList<>(images));
        report.setAuthor(current);
        report.setAuthorRole(current.getRole());
        report.setVisibleToCustomer(visibleToCustomer == null || visibleToCustomer);
        ProjectReport saved = reportRepository.save(report);
        if (carryoverLockContext != null) carryoverLockContext.lock(project, CarryoverBusinessType.SiteReport, current);
        return saved;
    }

    private String defaultTitle(ReportType type) {
        return "现场" + type.getName();
    }
}
