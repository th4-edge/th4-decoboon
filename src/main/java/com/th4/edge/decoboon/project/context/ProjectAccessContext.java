package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
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
    private final QuoteRepository quoteRepository;

    public ProjectAccessContext(TenantContext tenantContext,
                                DataScopeContext dataScopeContext,
                                ProjectRepository projectRepository,
                                QuoteRepository quoteRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.projectRepository = projectRepository;
        this.quoteRepository = quoteRepository;
    }

    public Employee requireEmployee(Employee employee) {
        return tenantContext.requireEmployee(employee);
    }

    /**
     * 合同发起角色矩阵（2026-10-08 用户确认）：设计师负责发起/登记客户签约，店长和企业负责人可复核。
     * 设计师不能直接确认签约或生效立项。
     */
    public Employee requireContractPreparer(Employee employee) {
        return requireRoles(employee,
                "只有设计师、店长或企业负责人可以发起合同签约",
                EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Designer);
    }

    /** 合同确认角色矩阵：企业负责人 + 店长。 */
    public Employee requireContractConfirmer(Employee employee) {
        return requireRoles(employee,
                "只有企业负责人或店长可以确认合同签约",
                EmployeeRole.Boss, EmployeeRole.StoreManager);
    }

    /** 合同生效与项目生成角色矩阵：企业负责人 + 店长。 */
    public Employee requireProjectCreator(Employee employee) {
        return requireRoles(employee,
                "只有企业负责人或店长可以确认合同生效并生成项目",
                EmployeeRole.Boss, EmployeeRole.StoreManager);
    }

    private Employee requireRoles(Employee employee, String message, EmployeeRole... allowedRoles) {
        Employee current = requireEmployee(employee);
        for (EmployeeRole allowed : allowedRoles) {
            if (current.getRole() == allowed) {
                return current;
            }
        }
        throw new DefinedException(new DefaultErrorCode("403", message));
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

    /** 业务员对本客户项目的只读范围；不用于任何写操作。 */
    public Specification<Project> visibleReadableProject(Employee employee) {
        Employee current = requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Marketer) {
            return visibleProject(current);
        }
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        return (root, query, builder) -> builder.and(
                builder.equal(root.get("company").get("comId"), comId),
                builder.or(
                        builder.equal(root.get("owner").get("empId"), empId),
                        builder.equal(root.get("customer").get("owner").get("empId"), empId),
                        builder.equal(root.get("quote").get("createdBy").get("empId"), empId)));
    }

    /** 只读加载业务员本客户项目，避免扩大写权限。 */
    public Project requireReadableProject(Long projectId, Employee employee) {
        Employee current = requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Marketer) {
            return requireProject(projectId, current);
        }
        Specification<Project> byId = (root, query, builder) -> builder.equal(root.get("projectId"), projectId);
        return projectRepository.findOne(byId.and(visibleReadableProject(current)))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "项目不存在或无权访问")));
    }

    /** 直接按实体自身归属快照应用当前数据范围。 */
    public <T> Specification<T> visibleEntity(Employee employee, Class<T> modelClass) {
        Employee current = requireEmployee(employee);
        return DataScopeSpecification.visible(modelClass, dataScopeContext.resolve(current));
    }

    /** 报价主键必须与当前员工数据范围在同一查询中命中。 */
    public Quote requireQuote(Long quoteId, Employee employee) {
        if (quoteId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "报价索引不能为空"));
        }
        Employee current = requireEmployee(employee);
        Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        return quoteRepository.findOne(byId.and(visibleQuote(current)))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "报价不存在或无权访问")));
    }

    /**
     * 报价是否属于当前企业（不返回对象内容，仅用于无能力角色的 403/404 语义判定）：
     * 同企业但无 contract:write 的能力拒绝返回 403，跨企业/不存在仍返回 404，避免对象存在性泄露。
     */
    public boolean sameCompanyQuote(Long quoteId, Employee employee) {
        if (quoteId == null) {
            return false;
        }
        Employee current = requireEmployee(employee);
        if (current.getCompany() == null) {
            return false;
        }
        return quoteRepository.existsByQuoteIdAndCompany_ComId(quoteId, current.getCompany().getComId());
    }

    /** 报价四级范围与负责人关系统一 Specification。 */
    public Specification<Quote> visibleQuote(Employee employee) {
        Employee current = requireEmployee(employee);
        return DataScopeSpecification.visible(Quote.class, dataScopeContext.resolve(current));
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

    /**
     * 项目参与人按显式参与关系只读房屋事实（量房 readiness、开工前置）。
     * 不参与任何项目时统一 404，不泄露对象存在性。
     */
    public com.th4.edge.decoboon.property.model.Property requireParticipantProperty(Long propertyId, Employee employee) {
        Employee current = requireEmployee(employee);
        if (propertyId == null) {
            throw new DefinedException(new DefaultErrorCode("404", "房屋不存在或无权访问"));
        }
        Long empId = current.getEmpId();
        Long comId = current.getCompany() == null ? null : current.getCompany().getComId();
        Specification<Project> spec = (root, query, builder) -> builder.and(
                builder.equal(root.get("company").get("comId"), comId),
                builder.equal(root.get("property").get("propertyId"), propertyId),
                builder.or(
                        builder.equal(root.get("designer").get("empId"), empId),
                        builder.equal(root.get("manager").get("empId"), empId),
                        builder.equal(root.get("support").get("empId"), empId),
                        builder.equal(root.get("quote").get("createdBy").get("empId"), empId)));
        return projectRepository.findOne(spec)
                .map(Project::getProperty)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "房屋不存在或无权访问")));
    }

    public Project requireVisible(Project project, Employee employee) {
        Employee current = requireEmployee(employee);
        if (project == null || project.getProjectId() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问"));
        }
        return requireProject(project.getProjectId(), current);
    }

    /**
     * 项目成员切换（设计师 / 项目经理 / 履约负责人）：企业负责人或本门店店长。
     *
     * <p>项目经理不得自行改派项目成员，避免出现「自行指派/自行接单」的权限扩张。</p>
     */
    public Project requireMemberEditor(Project project, Employee employee) {
        Employee current = requireEmployee(employee);
        requireVisible(project, current);
        if (current.getRole() == EmployeeRole.Boss) {
            return project;
        }
        if (current.getRole() == EmployeeRole.StoreManager && sameStore(project, current)) {
            return project;
        }
        throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人或本门店店长可以切换项目成员"));
    }

    /** 增减项发起：企业负责人、店长、项目经理或本项目设计师。 */
    public Project requireChangeInitiator(Project project, Employee employee) {
        Employee current = requireEmployee(employee);
        requireVisible(project, current);
        if (current.getRole() == EmployeeRole.Boss
                || current.getRole() == EmployeeRole.Pm
                || current.getRole() == EmployeeRole.StoreManager
                || isEmployee(project.getDesigner(), current)) {
            return project;
        }
        throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人、店长、项目经理或项目设计师可以发起增减项"));
    }

    /** 项目是否属于当前店长的门店（优先取项目自身归属，回退客户门店）。 */
    private boolean sameStore(Project project, Employee current) {
        Long storeId = current.getStore() == null ? null : current.getStore().getStoreId();
        if (storeId == null) {
            return false;
        }
        Long projectStoreId = project.getStore() == null ? null : project.getStore().getStoreId();
        if (projectStoreId == null && project.getCustomer() != null && project.getCustomer().getStore() != null) {
            projectStoreId = project.getCustomer().getStore().getStoreId();
        }
        return storeId.equals(projectStoreId);
    }

    public Project requirePlanEditor(Project project, Employee employee) {
        Employee current = requireEmployee(employee);
        requireVisible(project, current);
        if (current.getRole() == EmployeeRole.Boss
                || current.getRole() == EmployeeRole.Pm
                || isEmployee(project.getDesigner(), current)
                || (current.getRole() == EmployeeRole.StoreManager && isStoreProject(project, current))) {
            return project;
        }
        throw new DefinedException(new DefaultErrorCode("403", "只有设计师、项目经理、本店店长或企业负责人可以编排施工计划"));
    }

    private boolean isEmployee(Employee target, Employee current) {
        return target != null && current != null && target.getEmpId().equals(current.getEmpId());
    }

    private boolean isStoreProject(Project project, Employee current) {
        if (project == null || current == null || current.getStore() == null) {
            return false;
        }
        Long projectStoreId = project.getStore() != null
                ? project.getStore().getStoreId()
                : project.getCustomer() != null && project.getCustomer().getStore() != null
                        ? project.getCustomer().getStore().getStoreId() : null;
        return projectStoreId != null && projectStoreId.equals(current.getStore().getStoreId());
    }

    private boolean isTeamProject(Project project, Employee current) {
        return project.getCustomer() != null
                && project.getCustomer().getTeam() != null
                && current.getTeam() != null
                && project.getCustomer().getTeam().getTeamId().equals(current.getTeam().getTeamId());
    }
}
