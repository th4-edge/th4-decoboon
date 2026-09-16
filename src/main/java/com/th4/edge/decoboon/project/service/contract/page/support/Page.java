package com.th4.edge.decoboon.project.service.contract.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContractContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.model.Quote;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/** 当前项目合同基线查询实现。 */
@Note("合同基线查询")
@Properties(allow = {
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "project", "quote", "customer", "baselineAmount", "currentAmount",
                        "status", "signedAt", "createdBy", "remark", "receivableCount", "receivableAmount",
                        "receivedAmount", "pendingChangeCount", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "siteAddr", "contractAmount", "status", "property", "customer", "quote" }),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "amount", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo", "addr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.contract.page.Page {

    @AutoInject
    private Long projectId;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final ProjectContractRepository contractRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectContractContext contractContext;

    public Page(ProjectRepository projectRepository,
                ProjectContractRepository contractRepository,
                ProjectAccessContext accessContext,
                ProjectContractContext contractContext) {
        this.projectRepository = projectRepository;
        this.contractRepository = contractRepository;
        this.accessContext = accessContext;
        this.contractContext = contractContext;
    }

    @Override
    public org.springframework.data.domain.Page<ProjectContract> call(ProjectContract request) {
        if (projectId == null) return new PageImpl<>(List.of());
        Project project = accessContext.requireProject(projectId, employee);
        return contractRepository.findByProject(project)
                .map(contract -> new PageImpl<>(List.of(contractContext.enrich(contract))))
                .orElseGet(() -> new PageImpl<>(List.of()));
    }
}
