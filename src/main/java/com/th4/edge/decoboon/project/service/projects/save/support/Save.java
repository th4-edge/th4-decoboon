package com.th4.edge.decoboon.project.service.projects.save.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 项目创建 Provider，企业取登录员工，项目只能由已接受报价生成。 */
@Note("项目创建")
@Description("从当前企业已接受报价生成项目、阶段、施工任务和材料计划")
@Properties(allow = {
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "property", "customer", "quote", "designer", "manager", "support",
                        "siteAddr", "area", "contractAmount", "status", "progress", "startDate", "planEndDate" }),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Quote.class, properties = {"quoteId", "no", "amount", "status", "property"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/project/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.projects.save.Save {

    @AutoInject
    @NotNull
    private Long quoteId;

    @AutoInject
    @NotEmpty
    private String name;

    @AutoInject
    @NotEmpty
    private String siteAddr;

    @AutoInject
    private Long designerId;

    @AutoInject
    private Long managerId;

    @AutoInject
    private Long supportId;

    @AutoInject
    private LocalDate startDate;

    @AutoInject
    private LocalDate planEndDate;

    @User
    private Employee employee;

    private final ProjectContext projectContext;
    private final EmployeeRepository employeeRepository;
    private final ProjectAccessContext accessContext;

    public Save(ProjectContext projectContext,
                EmployeeRepository employeeRepository,
                ProjectAccessContext accessContext) {
        this.projectContext = projectContext;
        this.employeeRepository = employeeRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Project call(Project request) {
        Employee current = accessContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        Quote quote = accessContext.requireQuote(quoteId, current);

        Employee designer = designerId == null ? quote.getCreatedBy()
                : employeeRepository.findByEmpIdAndCompany_ComId(designerId, comId);
        Employee manager = managerId == null ? null
                : employeeRepository.findByEmpIdAndCompany_ComId(managerId, comId);
        Employee support = supportId == null ? null
                : employeeRepository.findByEmpIdAndCompany_ComId(supportId, comId);
        if (designer != null && designer.getRole() != EmployeeRole.Designer && designer.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("400", "项目设计师必须为设计师角色"));
        }
        if (manager != null && manager.getRole() != EmployeeRole.Pm && manager.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("400", "项目经理必须为项目经理角色"));
        }
        if (support != null && support.getRole() != EmployeeRole.Finance
                && support.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("400", "履约负责人必须为财务角色或企业负责人"));
        }

        Project draft = new Project();
        draft.setCompany(current.getCompany());
        draft.setName(name);
        draft.setSiteAddr(siteAddr);
        draft.setStartDate(startDate);
        draft.setPlanEndDate(planEndDate);
        draft.setDesigner(designer);
        draft.setManager(manager);
        draft.setSupport(support);
        return projectContext.createFromQuote(quote, draft);
    }
}
