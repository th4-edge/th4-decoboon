package com.th4.edge.decoboon.project.service.reports.list.support;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** 工地日报列表实现。 */
@Note("工地日报列表")
@Properties(allow = {
        @AllowProperties(model = ProjectReport.class,
                properties = {"reportId", "project", "stage", "task", "type", "title", "content", "images",
                        "author", "authorRole", "visibleToCustomer", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr", "property" }),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name"}),
        @AllowProperties(model = ProjectTask.class, properties = {"taskId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/report/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.project.service.reports.list.List {

    @AutoInject
    private Long projectId;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final ProjectReportRepository reportRepository;
    private final ProjectAccessContext accessContext;

    public List(ProjectRepository projectRepository,
                ProjectReportRepository reportRepository,
                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.accessContext = accessContext;
    }

    @Override
    public java.util.List<ProjectReport> call(ProjectReport request) {
        Employee current = accessContext.requireEmployee(employee);
        var project = accessContext.requireProject(projectId, current);
        return reportRepository.findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(
                current.getCompany().getComId(), projectId, PageRequest.of(0, 500)).getContent();
    }
}
