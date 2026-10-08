package com.th4.edge.decoboon.project.service.projects.update.support;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 项目状态更新 Provider，先按当前登录人的项目参与关系校验，再更新白名单字段。 */
@Note("项目更新")
@Properties(allow = {
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "property", "customer", "quote", "designer", "manager", "support",
                        "siteAddr", "area", "contractAmount", "cost", "progress", "startDate",
                        "planEndDate", "actualEndDate", "currentStageName", "createTime" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status", "owner"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "createdBy", "amount", "cost", "discount", "status", "property"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role"})
})
@Service("project/project/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.project.service.projects.update.Update {

    @AutoInject
    @NotNull
    private Long projectId;

    @AutoInject
    private String name;

    @AutoInject
    private String siteAddr;

    @AutoInject
    private LocalDate startDate;

    @AutoInject
    private LocalDate planEndDate;

    @AutoInject
    private Long designerId;

    @AutoInject
    private Long managerId;

    @AutoInject
    private Long supportId;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final ProjectAccessContext accessContext;
    private final EmployeeRepository employeeRepository;
    private final ProjectContractRepository contractRepository;
    private final ProjectContext projectContext;
    private final ProjectReportRepository reportRepository;
    private final ProjectPermissionContext permissionContext;

    public Update(ProjectRepository projectRepository,
                  ProjectAccessContext accessContext,
                  EmployeeRepository employeeRepository,
                  ProjectContractRepository contractRepository,
                  ProjectContext projectContext,
                  ProjectReportRepository reportRepository,
                  ProjectPermissionContext permissionContext) {
        this.projectRepository = projectRepository;
        this.accessContext = accessContext;
        this.employeeRepository = employeeRepository;
        this.contractRepository = contractRepository;
        this.projectContext = projectContext;
        this.reportRepository = reportRepository;
        this.permissionContext = permissionContext;
    }

    @Override
    @Transactional
    public Project call(Project request) {
        Employee current = accessContext.requireEmployee(employee);
        Project project = accessContext.requireProject(projectId, current);
        boolean memberChange = designerId != null || managerId != null || supportId != null;
        boolean dateChange = startDate != null || planEndDate != null;
        if (memberChange || dateChange) {
            // 成员切换仅限企业负责人或本门店店长（用户 2026-09-23 裁决）；
            // 合同基准开工/完工日期变更同样限 Boss/店长，项目经理不得改合同基准（DEC-048 T-06）。
            accessContext.requireMemberEditor(project, current);
        } else if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Pm) {
            throw new DefinedException(new DefaultErrorCode("403", "只有项目经理或企业负责人可以调整项目状态"));
        }
        List<String> memberChanges = new ArrayList<>();
        if (name != null && !name.isBlank()) project.setName(name.trim());
        if (siteAddr != null && !siteAddr.isBlank()) project.setSiteAddr(siteAddr.trim());
        if (startDate != null) project.setStartDate(startDate);
        if (planEndDate != null) project.setPlanEndDate(planEndDate);
        if (project.getStartDate() != null && project.getPlanEndDate() != null
                && project.getPlanEndDate().isBefore(project.getStartDate())) {
            throw new DefinedException(new DefaultErrorCode("400", "计划完工日期不能早于开工日期"));
        }
        Long comId = current.getCompany().getComId();
        if (designerId != null) {
            Employee designer = employeeRepository.findByEmpIdAndCompany_ComId(designerId, comId);
            if (designer == null || designer.getStatus() != EmployeeStatus.Active) {
                throw new DefinedException(new DefaultErrorCode("400", "项目设计师必须为本企业在职员工"));
            }
            memberChanges.add(memberChangeText("项目设计师", project.getDesigner(), designer));
            project.setDesigner(designer);
        }
        if (managerId != null) {
            Employee manager = employeeRepository.findByEmpIdAndCompany_ComId(managerId, comId);
            if (manager == null || (manager.getRole() != EmployeeRole.Pm && manager.getRole() != EmployeeRole.Boss)) {
                throw new DefinedException(new DefaultErrorCode("400", "项目经理必须为项目经理或企业负责人"));
            }
            memberChanges.add(memberChangeText("项目经理", project.getManager(), manager));
            project.setManager(manager);
        }
        if (supportId != null) {
            Employee support = employeeRepository.findByEmpIdAndCompany_ComId(supportId, comId);
            if (support == null || (support.getRole() != EmployeeRole.Finance && support.getRole() != EmployeeRole.Boss)) {
                throw new DefinedException(new DefaultErrorCode("400", "履约负责人必须为财务或企业负责人"));
            }
            memberChanges.add(memberChangeText("履约负责人", project.getSupport(), support));
            project.setSupport(support);
        }
        Project saved = projectRepository.save(project);
        recordMemberChange(saved, current, memberChanges.stream().filter(java.util.Objects::nonNull).toList());
        projectContext.scheduleStages(saved);
        saved = projectContext.refreshProject(saved);
        contractRepository.findByProject_ProjectIdAndCompany_ComId(saved.getProjectId(), comId)
                .ifPresent(contract -> {
                    if (startDate != null) contract.setStartDate(startDate);
                    if (planEndDate != null) contract.setPlanEndDate(planEndDate);
                    contractRepository.save(contract);
                });
        return saved;
    }

    /** 生成「原 → 新」文案；未发生变化时返回 null（不写动态）。 */
    private String memberChangeText(String label, Employee before, Employee after) {
        if (after == null) {
            return null;
        }
        String beforeName = before == null ? "未指定" : before.getName();
        if (before != null && before.getEmpId() != null && before.getEmpId().equals(after.getEmpId())) {
            return null;
        }
        return label + "：" + beforeName + " → " + after.getName();
    }

    /** 成员切换写一条对内项目动态（含原→新、操作人与时间），不做接手确认、立即生效。 */
    private void recordMemberChange(Project project, Employee operator, List<String> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        ProjectReport report = new ProjectReport();
        report.setCompany(project.getCompany());
        report.setProject(project);
        report.setProperty(project.getProperty());
        permissionContext.apply(report, project);
        report.setType(ReportType.Notice);
        report.setTitle("项目成员调整");
        report.setContent(String.join("；", changes)
                + "（操作人：" + operator.getName() + "，时间：" + LocalDateTime.now() + "，立即生效）");
        report.setAuthor(operator);
        report.setAuthorRole(operator.getRole());
        report.setVisibleToCustomer(false);
        reportRepository.save(report);
    }
}
