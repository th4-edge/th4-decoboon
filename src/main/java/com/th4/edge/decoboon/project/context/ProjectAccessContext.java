package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** 项目参与人权限上下文。 */
@Component
public class ProjectAccessContext {

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final ProjectRepository projectRepository;

    public ProjectAccessContext(TenantContext tenantContext,
                                DataScopeContext dataScopeContext,
                                ProjectRepository projectRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.projectRepository = projectRepository;
    }

    public Employee requireEmployee(Employee employee) {
        return tenantContext.requireEmployee(employee);
    }

    /** 主键 + 当前数据范围的单次项目查询。 */
    public Project requireProject(Long projectId, Employee employee) {
        if (projectId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "项目索引不能为空"));
        }
        Specification<Project> byId = (root, query, builder) -> builder.equal(root.get("projectId"), projectId);
        return projectRepository.findOne(byId.and(visibleProject(employee)))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "项目不存在或无权访问")));
    }

    /** 直接按实体自身归属快照应用当前数据范围。 */
    public <T> Specification<T> visibleEntity(Employee employee, Class<T> modelClass) {
        Employee current = requireEmployee(employee);
        return DataScopeSpecification.visible(modelClass, dataScopeContext.resolve(current));
    }

    /** 项目四级范围与显式参与关系的统一 Specification。 */
    public Specification<Project> visibleProject(Employee employee) {
        Employee current = requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Project> scoped = DataScopeSpecification.visible(Project.class, scope);
        Long empId = current.getEmpId();
        Long comId = scope.comId();
        Specification<Project> participant = (root, query, builder) -> builder.and(
                builder.equal(root.get("company").get("comId"), comId),
                builder.or(
                        builder.equal(root.get("designer").get("empId"), empId),
                        builder.equal(root.get("manager").get("empId"), empId),
                        builder.equal(root.get("support").get("empId"), empId),
                        builder.equal(root.get("quote").get("createdBy").get("empId"), empId)));
        return scope.type() == com.th4.edge.tenant.user.configure.DataScopeType.Company
                ? scoped : scoped.or(participant);
    }


    /** 子对象四级范围与项目显式参与关系的统一 Specification。 */
    public <T> Specification<T> visibleChild(Employee employee, Class<T> modelClass, String projectPath) {
        Employee current = requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);
        Specification<T> scoped = DataScopeSpecification.visible(modelClass, scope);
        Long empId = current.getEmpId();
        Long comId = scope.comId();
        Specification<T> participant = (root, query, builder) -> builder.and(
                builder.equal(root.get(projectPath).get("company").get("comId"), comId),
                builder.or(
                        builder.equal(root.get(projectPath).get("owner").get("empId"), empId),
                        builder.equal(root.get(projectPath).get("designer").get("empId"), empId),
                        builder.equal(root.get(projectPath).get("manager").get("empId"), empId),
                        builder.equal(root.get(projectPath).get("support").get("empId"), empId),
                        builder.equal(root.get(projectPath).get("quote").get("createdBy").get("empId"), empId)));
        return scope.type() == com.th4.edge.tenant.user.configure.DataScopeType.Company
                ? scoped : scoped.or(participant);
    }


    /** 合同自身归属与签约/项目参与关系的统一 Specification。 */
    public Specification<com.th4.edge.decoboon.project.model.ProjectContract> visibleContract(Employee employee) {
        Employee current = requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);
        Specification<com.th4.edge.decoboon.project.model.ProjectContract> scoped =
                DataScopeSpecification.visible(com.th4.edge.decoboon.project.model.ProjectContract.class, scope);
        Long empId = current.getEmpId();
        Long comId = scope.comId();
        Specification<com.th4.edge.decoboon.project.model.ProjectContract> participant =
                (root, query, builder) -> builder.and(
                        builder.equal(root.get("company").get("comId"), comId),
                        builder.or(
                                builder.equal(root.get("createdBy").get("empId"), empId),
                                builder.equal(root.get("quote").get("createdBy").get("empId"), empId),
                                builder.equal(root.join("project", JoinType.LEFT).get("designer").get("empId"), empId),
                                builder.equal(root.join("project", JoinType.LEFT).get("manager").get("empId"), empId),
                                builder.equal(root.join("project", JoinType.LEFT).get("support").get("empId"), empId)));
        return scope.type() == com.th4.edge.tenant.user.configure.DataScopeType.Company
                ? scoped : scoped.or(participant);
    }

    public Project requireVisible(Project project, Employee employee) {
        Employee current = requireEmployee(employee);
        if (project == null || project.getProjectId() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问"));
        }
        return requireProject(project.getProjectId(), current);
    }

    public Project requirePlanEditor(Project project, Employee employee) {
        Employee current = requireEmployee(employee);
        requireVisible(project, current);
        if (current.getRole() == EmployeeRole.Boss
                || current.getRole() == EmployeeRole.Pm
                || isEmployee(project.getDesigner(), current)) {
            return project;
        }
        throw new DefinedException(new DefaultErrorCode("403", "只有设计师、项目经理或企业负责人可以编排施工计划"));
    }

    private boolean isEmployee(Employee target, Employee current) {
        return target != null && current != null && target.getEmpId().equals(current.getEmpId());
    }

    private boolean isStoreProject(Project project, Employee current) {
        return project.getCustomer() != null
                && project.getCustomer().getStore() != null
                && current.getStore() != null
                && project.getCustomer().getStore().getStoreId().equals(current.getStore().getStoreId());
    }

    private boolean isTeamProject(Project project, Employee current) {
        return project.getCustomer() != null
                && project.getCustomer().getTeam() != null
                && current.getTeam() != null
                && project.getCustomer().getTeam().getTeamId().equals(current.getTeam().getTeamId());
    }
}
