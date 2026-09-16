package com.th4.edge.decoboon.project.service.projects.update.support;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
    private LocalDate startDate;

    @AutoInject
    private LocalDate planEndDate;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final ProjectAccessContext accessContext;

    public Update(ProjectRepository projectRepository, ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Project call(Project request) {
        Employee current = accessContext.requireEmployee(employee);
        Project project = accessContext.requireProject(projectId, current);
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Pm) {
            throw new DefinedException(new DefaultErrorCode("403", "只有项目经理或企业负责人可以调整项目状态"));
        }
        if (startDate != null) project.setStartDate(startDate);
        if (planEndDate != null) project.setPlanEndDate(planEndDate);
        return projectRepository.save(project);
    }
}
