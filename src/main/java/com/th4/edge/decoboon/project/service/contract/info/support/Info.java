package com.th4.edge.decoboon.project.service.contract.info.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContractContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.plan.model.ContractConstructionPlan;
import com.th4.edge.decoboon.plan.model.ContractConstructionStage;
import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.decoboon.plan.model.ContractPaymentPlan;
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
import org.springframework.transaction.annotation.Transactional;

/** 项目合同基线详情实现。 */
@Note("合同基线详情")
@Properties(allow = {
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "project", "quote", "customer", "property", "baselineAmount",
                        "signedAmount", "currentAmount", "status", "signedAt", "startDate", "planEndDate", "effectiveAt",
                        "paymentTerms", "createdBy", "remark", "receivableCount", "receivableAmount",
                        "receivedAmount", "pendingChangeCount", "paymentPlan",
                        "constructionPlan", "createTime", "businessStateView"}),
        @AllowProperties(model = ContractPaymentPlan.class,
                properties = {"planId", "sourceCode", "sourceVersion", "name", "totalAmount", "status", "nodes"}),
        @AllowProperties(model = ContractPaymentNode.class,
                properties = {"nodeId", "code", "name", "ratio", "amount", "type", "triggerType", "triggerStageCode",
                        "triggerStageName", "offsetDays", "dueDate", "sort"}),
        @AllowProperties(model = ContractConstructionPlan.class,
                properties = {"planId", "sourceCode", "sourceVersion", "sourceTotalDays", "name", "startDate", "endDate", "status", "stages"}),
        @AllowProperties(model = ContractConstructionStage.class,
                properties = {"stageId", "code", "name", "sort", "defaultDays", "plannedStart", "plannedEnd",
                        "acceptanceMode", "description"}),
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
    @Transactional(readOnly = true)
    public ProjectContract call(ProjectContract request) {
        Employee current = accessContext.requireEmployee(employee);
        if (quoteId != null) {
            Specification<ProjectContract> byQuote = (root, query, builder) ->
                    builder.equal(root.get("quote").get("quoteId"), quoteId);
            ProjectContract visible = contractRepository.findOne(
                    byQuote.and(accessContext.visibleContract(employee))).orElse(null);
            if (visible == null) {
                return null;
            }
            ProjectContract contract = contractRepository.findDetailedByQuote_QuoteId(quoteId)
                    .orElseThrow(() -> new DefinedException(
                            new DefaultErrorCode("404", "合同不存在或无权访问")));
            initializeDetailAssociations(contract);
            return contractContext.enrich(contract);
        }
        Project project = accessContext.requireProject(projectId, current);
        ProjectContract contract = contractRepository.findDetailedByProject(project)
                .orElse(null);
        if (contract == null) {
            return null;
        }
        initializeDetailAssociations(contract);
        return contractContext.enrich(contract);
    }

    private void initializeDetailAssociations(ProjectContract contract) {
        if (contract.getProject() != null) {
            if (contract.getProject().getProperty() != null) {
                contract.getProject().getProperty().getPropertyId();
            }
            if (contract.getProject().getCustomer() != null) {
                contract.getProject().getCustomer().getCustId();
            }
            if (contract.getProject().getQuote() != null) {
                contract.getProject().getQuote().getQuoteId();
            }
        }
        if (contract.getCreatedBy() != null) {
            contract.getCreatedBy().getName();
        }
    }
}
