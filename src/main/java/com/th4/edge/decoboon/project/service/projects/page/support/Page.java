package com.th4.edge.decoboon.project.service.projects.page.support;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 项目分页实现，按登录员工实际参与关系过滤。 */
@Note("项目分页")
@Properties(allow = {
        @AllowProperties(model = Project.class,
                properties = {"projectId", "businessOrigin", "no", "name", "property", "customer", "quote", "designer", "manager", "support",
                        "siteAddr", "area", "contractAmount", "cost", "status", "progress", "startDate",
                        "planEndDate", "actualEndDate", "currentStageName", "createTime" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status", "owner"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "createdBy", "amount", "cost", "discount", "status", "property"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer", "latestMeasureAt"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name", "telNo", "role"})
})
@Service("project/project/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.projects.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private Long customerId;

    @AutoInject
    private String keyword;

    @AutoInject
    private ProjectStatus status;

    /** 状态集合：用于"在跑 / 已完工"这类分组筛选，与单值 status 互不冲突。 */
    @AutoInject
    private java.util.List<ProjectStatus> statuses;

    /** 设计师筛选（empId），用于项目选择器快速定位。 */
    @AutoInject
    private Long designerId;

    /** 项目类型筛选：正常业务 / 接管基线。 */
    @AutoInject
    private BusinessOrigin businessOrigin;

    private final ProjectRepository projectRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectAccessContext accessContext;

    public Page(ProjectRepository projectRepository,
                ProjectStageRepository stageRepository,
                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Project> call(Project request) {
        Employee currentEmployee = accessContext.requireEmployee(employee);
        Long comId = currentEmployee.getCompany().getComId();
        Long empId = currentEmployee.getEmpId();
        List<Project> projects = switch (currentEmployee.getRole()) {
            case Boss, Finance -> projectRepository.findByCompany_ComIdOrderByProjectIdDesc(comId);
            case StoreManager -> {
                if (currentEmployee.getStore() == null || currentEmployee.getStore().getStoreId() == null) {
                    yield List.of();
                }
                yield projectRepository.findByCompany_ComIdAndCustomer_Store_StoreIdOrderByProjectIdDesc(
                        comId, currentEmployee.getStore().getStoreId());
            }
            case TeamLeader -> {
                if (currentEmployee.getTeam() == null || currentEmployee.getTeam().getTeamId() == null) {
                    yield List.of();
                }
                yield projectRepository.findByCompany_ComIdAndCustomer_Team_TeamIdOrderByProjectIdDesc(
                        comId, currentEmployee.getTeam().getTeamId());
            }
            case Designer -> projectRepository.findByCompany_ComIdAndDesigner_EmpIdOrderByProjectIdDesc(comId, empId);
            case Pm -> projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(comId, empId);
            case Marketer -> projectRepository.findByCompany_ComIdAndOwner_EmpIdOrderByProjectIdDesc(comId, empId);
            default -> List.of();
        };
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        projects = projects.stream()
                .filter(project -> customerId == null || project.getCustomer().getCustId().equals(customerId))
                .filter(project -> status == null || project.getStatus() == status)
                .filter(project -> statuses == null || statuses.isEmpty() || statuses.contains(project.getStatus()))
                .filter(project -> designerId == null
                        || (project.getDesigner() != null && designerId.equals(project.getDesigner().getEmpId())))
                .filter(project -> businessOrigin == null || project.getBusinessOrigin() == businessOrigin)
                .filter(project -> kw.isBlank()
                        || project.getName().toLowerCase().contains(kw)
                        || project.getNo().toLowerCase().contains(kw)
                        || project.getSiteAddr().toLowerCase().contains(kw)
                        || project.getCustomer().getName().toLowerCase().contains(kw))
                .sorted(Comparator.comparing(Project::getProjectId).reversed())
                .toList();
        projects.forEach(this::fillCurrentStage);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 100 : Math.min(size, 200);
        int from = Math.min(page * pageSize, projects.size());
        int to = Math.min(from + pageSize, projects.size());
        return new PageImpl<>(projects.subList(from, to), PageRequest.of(page, pageSize), projects.size());
    }

    private void fillCurrentStage(Project project) {
        var stages = stageRepository.findByProjectOrderBySort(project);
        stages.stream()
                .filter(stage -> stage.getStatus() == com.th4.edge.decoboon.project.configure.StageStatus.Doing
                        || stage.getStatus() == com.th4.edge.decoboon.project.configure.StageStatus.Accepting)
                .findFirst()
                .or(() -> stages.stream().findFirst())
                .ifPresent(stage -> project.setCurrentStageName(stage.getName()));
    }
}
