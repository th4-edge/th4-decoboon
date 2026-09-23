package com.th4.edge.decoboon.project.service.members.save.support;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectMember;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 企业负责人为项目指派设计师或项目经理。 */
@Note("项目成员保存")
@Properties(allow = {
        @AllowProperties(model = ProjectMember.class,
                properties = {"id", "project", "emp", "customer", "kind", "role", "label", "joinedAt"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "property" }),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"})
})
@Service("project/member/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.members.save.Save {

    @AutoInject
    @NotNull
    private Long projectId;

    @AutoInject
    private Long empId;

    @AutoInject
    private EmployeeRole role;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectReportRepository reportRepository;
    private final ProjectPermissionContext permissionContext;

    public Save(ProjectRepository projectRepository,
                EmployeeRepository employeeRepository,
                ProjectAccessContext accessContext,
                ProjectReportRepository reportRepository,
                ProjectPermissionContext permissionContext) {
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
        this.accessContext = accessContext;
        this.reportRepository = reportRepository;
        this.permissionContext = permissionContext;
    }

    @Override
    @Transactional
    public ProjectMember call(ProjectMember request) {
        Employee current = accessContext.requireEmployee(employee);
        Project project = accessContext.requireProject(projectId, current);
        // 成员指派/切换：企业负责人或本门店店长（用户 2026-09-23 裁决）。
        accessContext.requireMemberEditor(project, current);
        if (empId == null || role == null) {
            throw new DefinedException(new DefaultErrorCode("400", "成员和项目角色不能为空"));
        }
        Employee target = employeeRepository.findByEmpIdAndCompany_ComId(empId, current.getCompany().getComId());
        if (target == null) throw new DefinedException(new DefaultErrorCode("404", "员工不存在"));
        String label;
        Employee before;
        if (role == EmployeeRole.Designer) {
            label = "项目设计师";
            before = project.getDesigner();
            project.setDesigner(target);
        } else if (role == EmployeeRole.Pm) {
            label = "项目经理";
            before = project.getManager();
            project.setManager(target);
        } else {
            throw new DefinedException(new DefaultErrorCode("400", "项目成员角色仅支持设计师或项目经理"));
        }
        projectRepository.save(project);
        recordMemberChange(project, current, label, before, target);
        ProjectMember member = new ProjectMember();
        member.setId("emp-" + projectId + "-" + empId);
        member.setProject(project);
        member.setEmp(target);
        member.setKind("employee");
        member.setRole(role);
        member.setLabel(role.getName());
        return member;
    }

    /** 成员切换写一条对内项目动态（原→新 + 操作人 + 时间），立即生效、无接手确认。 */
    private void recordMemberChange(Project project, Employee operator, String label,
                                    Employee before, Employee after) {
        if (after == null || (before != null && before.getEmpId() != null
                && before.getEmpId().equals(after.getEmpId()))) {
            return;
        }
        ProjectReport report = new ProjectReport();
        report.setCompany(project.getCompany());
        report.setProject(project);
        report.setProperty(project.getProperty());
        permissionContext.apply(report, project);
        report.setType(ReportType.Notice);
        report.setTitle("项目成员调整");
        report.setContent(label + "：" + (before == null ? "未指定" : before.getName()) + " → " + after.getName()
                + "（操作人：" + operator.getName() + "，时间：" + java.time.LocalDateTime.now() + "，立即生效）");
        report.setAuthor(operator);
        report.setAuthorRole(operator.getRole());
        report.setVisibleToCustomer(false);
        reportRepository.save(report);
    }
}
