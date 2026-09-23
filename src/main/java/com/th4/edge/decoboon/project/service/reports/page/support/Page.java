package com.th4.edge.decoboon.project.service.reports.page.support;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** 工地动态分页实现。 */
@Note("工地动态分页")
@Properties(allow = {
        @AllowProperties(model = ProjectReport.class,
                properties = {"reportId", "project", "stage", "task", "type", "title", "content",
                        "change", "images", "author", "authorRole", "visibleToCustomer", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class,
                properties = {"projectId", "name", "siteAddr"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.ProjectStage.class,
                properties = {"stageId", "name"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.ProjectTask.class,
                properties = {"taskId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/report/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.reports.page.Page {

    @AutoInject
    private Long projectId;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private ReportType type;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final ProjectReportRepository reportRepository;
    private final ProjectAccessContext accessContext;

    public Page(ProjectRepository projectRepository,
                ProjectReportRepository reportRepository,
                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<ProjectReport> call(ProjectReport request) {
        Employee currentEmployee = accessContext.requireEmployee(employee);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 100 : Math.min(size, 200);
        if (projectId == null) {
            Long comId = currentEmployee.getCompany().getComId();
            Long empId = currentEmployee.getEmpId();
            var visibleProjects = switch (currentEmployee.getRole()) {
                case Boss, Finance -> projectRepository.findByCompany_ComIdOrderByProjectIdDesc(comId);
                case StoreManager -> currentEmployee.getStore() == null || currentEmployee.getStore().getStoreId() == null
                        ? java.util.List.<com.th4.edge.decoboon.project.model.Project>of()
                        : projectRepository.findByCompany_ComIdAndCustomer_Store_StoreIdOrderByProjectIdDesc(
                                comId, currentEmployee.getStore().getStoreId());
                case TeamLeader -> currentEmployee.getTeam() == null || currentEmployee.getTeam().getTeamId() == null
                        ? java.util.List.<com.th4.edge.decoboon.project.model.Project>of()
                        : projectRepository.findByCompany_ComIdAndCustomer_Team_TeamIdOrderByProjectIdDesc(
                                comId, currentEmployee.getTeam().getTeamId());
                case Designer -> projectRepository.findByCompany_ComIdAndDesigner_EmpIdOrderByProjectIdDesc(comId, empId);
                case Pm -> projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(comId, empId);
                case Marketer -> projectRepository.findByCompany_ComIdAndQuote_CreatedBy_EmpIdOrderByProjectIdDesc(comId, empId);
                default -> java.util.List.<com.th4.edge.decoboon.project.model.Project>of();
            };
            var visibleIds = visibleProjects.stream()
                    .map(com.th4.edge.decoboon.project.model.Project::getProjectId).toList();
            var all = reportRepository.findByCompany_ComIdOrderByCreateTime_DateTimeDesc(
                    comId, PageRequest.of(0, 500)).getContent().stream()
                    .filter(report -> visibleIds.contains(report.getProject().getProjectId()))
                    .filter(report -> type == null || report.getType() == type)
                    .toList();
            int from = Math.min(page * pageSize, all.size());
            int to = Math.min(from + pageSize, all.size());
            return new org.springframework.data.domain.PageImpl<>(all.subList(from, to),
                    PageRequest.of(page, pageSize), all.size());
        }
        accessContext.requireProject(projectId, currentEmployee);
        if (type == null) {
            return reportRepository.findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(
                    currentEmployee.getCompany().getComId(), projectId, PageRequest.of(page, pageSize));
        }
        var all = reportRepository.findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(
                currentEmployee.getCompany().getComId(), projectId, PageRequest.of(0, 500)).getContent().stream()
                .filter(report -> report.getType() == type)
                .toList();
        int from = Math.min(page * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return new org.springframework.data.domain.PageImpl<>(all.subList(from, to),
                PageRequest.of(page, pageSize), all.size());
    }
}
