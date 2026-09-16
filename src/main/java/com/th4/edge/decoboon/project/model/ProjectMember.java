package com.th4.edge.decoboon.project.model;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;

/** 项目同频成员视图。成员由项目角色关系实时派生，不作为独立授权真源。 */
@Note("项目成员")
@Model
public class ProjectMember {

    private String id;
    private Project project;
    private Employee emp;
    private Customer customer;
    private String kind;
    private EmployeeRole role;
    private String label;
    private String joinedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Employee getEmp() { return emp; }
    public void setEmp(Employee emp) { this.emp = emp; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public EmployeeRole getRole() { return role; }
    public void setRole(EmployeeRole role) { this.role = role; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getJoinedAt() { return joinedAt; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }
}
