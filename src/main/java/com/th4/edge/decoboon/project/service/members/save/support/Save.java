package com.th4.edge.decoboon.project.service.members.save.support;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectMember;
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

    public Save(ProjectRepository projectRepository,
                EmployeeRepository employeeRepository,
                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public ProjectMember call(ProjectMember request) {
        Employee current = accessContext.requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人可以调整项目成员"));
        }
        Project project = accessContext.requireProject(projectId, current);
        if (empId == null || role == null) {
            throw new DefinedException(new DefaultErrorCode("400", "成员和项目角色不能为空"));
        }
        Employee target = employeeRepository.findByEmpIdAndCompany_ComId(empId, current.getCompany().getComId());
        if (target == null) throw new DefinedException(new DefaultErrorCode("404", "员工不存在"));
        if (role == EmployeeRole.Designer) {
            project.setDesigner(target);
        } else if (role == EmployeeRole.Pm) {
            project.setManager(target);
        } else {
            throw new DefinedException(new DefaultErrorCode("400", "项目成员角色仅支持设计师或项目经理"));
        }
        projectRepository.save(project);
        ProjectMember member = new ProjectMember();
        member.setId("emp-" + projectId + "-" + empId);
        member.setProject(project);
        member.setEmp(target);
        member.setKind("employee");
        member.setRole(role);
        member.setLabel(role.getName());
        return member;
    }
}
