package com.th4.edge.decoboon.project.service.stages.update.support;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
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

/** 项目经理开启阶段，自动同步项目状态并发布对业主可见动态。 */
@Note("项目阶段推进")
@Properties(allow = {
        @AllowProperties(model = ProjectStage.class,
                properties = {"stageId", "project", "name", "sort", "weight", "status", "progress",
                        "plannedStart", "plannedEnd", "actualStart", "actualEnd", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr", "property" })
})
@Service("project/stage/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.project.service.stages.update.Update {

    @AutoInject
    @NotNull
    private Long stageId;

    @AutoInject
    private StageStatus status;

    @AutoInject
    private String content;

    @User
    private Employee employee;

    private final ProjectStageRepository stageRepository;
    private final ProjectRepository projectRepository;
    private final ProjectReportRepository reportRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectPermissionContext permissionContext;
    private final MeasureRepository measureRepository;

    public Update(ProjectStageRepository stageRepository,
                  ProjectRepository projectRepository,
                  ProjectReportRepository reportRepository,
                  ProjectAccessContext accessContext,
                  ProjectPermissionContext permissionContext,
                  MeasureRepository measureRepository) {
        this.stageRepository = stageRepository;
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.accessContext = accessContext;
        this.permissionContext = permissionContext;
        this.measureRepository = measureRepository;
    }

    @Override
    @Transactional
    public ProjectStage call(ProjectStage request) {
        Employee current = accessContext.requireEmployee(employee);
        ProjectStage stage = stageRepository.findOne(((Specification<ProjectStage>) (root, query, builder) -> builder.equal(root.get("stageId"), stageId)).and(accessContext.visibleChild(employee, ProjectStage.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目阶段不存在")));
        accessContext.requireVisible(stage.getProject(), current);
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Pm) {
            throw new DefinedException(new DefaultErrorCode("403", "只有项目经理或企业负责人可以开启施工阶段"));
        }
        StageStatus target = status == null ? StageStatus.Doing : status;
        if (target != StageStatus.Doing || stage.getStatus() != StageStatus.Todo) {
            throw new DefinedException(new DefaultErrorCode("400", "只有待开始阶段可以开工"));
        }
        if (stage.getProject().getManager() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请先指定项目经理再开工"));
        }
        if (stage.getProject().getStartDate() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请先确认项目开工日期"));
        }
        if (stage.getProject().getPlanEndDate() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请先确认项目计划完工日期"));
        }
        if (stage.getProject().getPlanEndDate().isBefore(stage.getProject().getStartDate())) {
            throw new DefinedException(new DefaultErrorCode("400", "计划完工日期不能早于开工日期"));
        }
        var property = stage.getProject().getProperty();
        boolean measured = property != null && property.getLatestMeasureAt() != null;
        if (!measured && property != null) {
            measured = measureRepository.findByPropertyOrderByMeasureTimeDesc(property).stream()
                    .anyMatch(item -> item.getStatus() == MeasureStatus.Completed);
        }
        if (!measured) {
            throw new DefinedException(new DefaultErrorCode("400", "请先完成量房并录入量房结论再开工"));
        }
        stage.setStatus(StageStatus.Doing);
        stage.setActualStart(LocalDate.now());
        stage.getProject().setStatus(ProjectStatus.Ongoing);
        projectRepository.save(stage.getProject());
        ProjectStage saved = stageRepository.save(stage);

        ProjectReport report = new ProjectReport();
        report.setCompany(saved.getCompany());
        report.setProject(saved.getProject());
        report.setProperty(saved.getProperty());
        permissionContext.apply(report, saved.getProject());
        report.setStage(saved);
        report.setType(ReportType.StageOpen);
        report.setTitle("阶段开工 · " + saved.getName());
        report.setContent(content == null || content.isBlank() ? saved.getName() + "已正式开工" : content);
        report.setAuthor(current);
        report.setAuthorRole(current.getRole());
        report.setVisibleToCustomer(true);
        reportRepository.save(report);
        return saved;
    }
}
