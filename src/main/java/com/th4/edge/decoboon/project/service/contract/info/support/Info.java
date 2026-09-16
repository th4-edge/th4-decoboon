package com.th4.edge.decoboon.project.service.contract.info.support;

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
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** 项目合同基线详情实现。 */
@Note("合同基线详情")
@Properties(allow = {
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "project", "quote", "customer", "property", "baselineAmount",
                        "currentAmount", "status", "signedAt", "startDate", "planEndDate", "effectiveAt",
                        "paymentTerms", "createdBy", "remark", "receivableCount", "receivableAmount",
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
@Service("project/contract/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.project.service.contract.info.Info {

    @AutoInject
    private Long projectId;

    @AutoInject
    private Long quoteId;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final ProjectContractRepository contractRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectContractContext contractContext;

    public Info(ProjectRepository projectRepository,
                ProjectContractRepository contractRepository,
                ProjectAccessContext accessContext,
                ProjectContractContext contractContext) {
        this.projectRepository = projectRepository;
        this.contractRepository = contractRepository;
        this.accessContext = accessContext;
        this.contractContext = contractContext;
    }

    @Override
    public ProjectContract call(ProjectContract request) {
        Employee current = accessContext.requireEmployee(employee);
        if (quoteId != null) {
            Specification<ProjectContract> byQuote = (root, query, builder) ->
                    builder.equal(root.get("quote").get("quoteId"), quoteId);
            return contractRepository.findOne(byQuote.and(accessContext.visibleContract(employee)))
                    .map(contractContext::enrich)
                    .orElse(null);
        }
        Project project = accessContext.requireProject(projectId, current);
        return contractRepository.findByProject(project)
                .map(contractContext::enrich)
                .orElse(null);
    }
}
