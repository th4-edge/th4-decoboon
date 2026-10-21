package com.th4.edge.decoboon.project.service.members.list.support;

import com.th4.edge.decoboon.common.support.DateTimeText;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectMember;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** 根据项目角色关系实时生成成员列表。 */
@Note("项目成员列表")
@Properties(allow = {
        @AllowProperties(model = ProjectMember.class,
                properties = {"id", "project", "emp", "customer", "kind", "role", "label", "joinedAt"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "name", "property" }),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"})
})
@Service("project/member/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.project.service.members.list.List {

    @AutoInject
    private Long projectId;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final ProjectAccessContext accessContext;

    public List(ProjectRepository projectRepository, ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<ProjectMember> call(ProjectMember request) {
        Project visible = accessContext.requireProject(projectId, employee);
        Project project = projectRepository.findDetailedByProjectIdAndCompany_ComId(
                        visible.getProjectId(), visible.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问")));
        initializeMemberAssociations(project);
        Map<Long, Employee> employees = new LinkedHashMap<>();
        if (project.getDesigner() != null) employees.put(project.getDesigner().getEmpId(), project.getDesigner());
        if (project.getManager() != null) employees.put(project.getManager().getEmpId(), project.getManager());
        if (project.getQuote() != null && project.getQuote().getCreatedBy() != null) {
            employees.put(project.getQuote().getCreatedBy().getEmpId(), project.getQuote().getCreatedBy());
        }
        if (project.getCustomer() != null && project.getCustomer().getOwner() != null) {
            employees.put(project.getCustomer().getOwner().getEmpId(), project.getCustomer().getOwner());
        }
        java.util.List<ProjectMember> result = new ArrayList<>();
        for (Employee item : employees.values()) {
            ProjectMember member = new ProjectMember();
            member.setId("emp-" + project.getProjectId() + "-" + item.getEmpId());
            member.setProject(project);
            member.setEmp(item);
            member.setKind("employee");
            member.setRole(item.getRole());
            member.setLabel(item.getRole().getName());
            member.setJoinedAt(project.getCreateTime() == null ? null : DateTimeText.format(project.getCreateTime()));
            result.add(member);
        }
        if (project.getCustomer() != null) {
            ProjectMember owner = new ProjectMember();
            owner.setId("customer-" + project.getProjectId());
            owner.setProject(project);
            owner.setCustomer(project.getCustomer());
            owner.setKind("customer");
            owner.setLabel("业主");
            owner.setJoinedAt(project.getCreateTime() == null ? null : DateTimeText.format(project.getCreateTime()));
            result.add(owner);
        }
        return result;
    }

    private void initializeMemberAssociations(Project project) {
        if (project.getCustomer() != null && project.getCustomer().getOwner() != null) {
            project.getCustomer().getOwner().getName();
        }
        if (project.getQuote() != null && project.getQuote().getCreatedBy() != null) {
            project.getQuote().getCreatedBy().getName();
        }
        if (project.getManager() != null) {
            project.getManager().getName();
        }
    }
}
