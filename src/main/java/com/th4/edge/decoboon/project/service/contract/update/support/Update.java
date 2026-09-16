package com.th4.edge.decoboon.project.service.contract.update.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.context.ProjectContractContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 推进合同签署和生效，合同生效时生成唯一项目。 */
@Note("合同签约与生效")
@Properties(allow = {
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "project", "quote", "customer", "property", "baselineAmount",
                        "currentAmount", "status", "signedAt", "startDate", "planEndDate", "effectiveAt",
                        "paymentTerms", "createdBy", "remark", "receivableCount", "receivableAmount",
                        "receivedAmount", "pendingChangeCount", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "customer", "property", "quote", "designer", "manager",
                        "siteAddr", "area", "contractAmount", "status", "progress", "startDate", "planEndDate" }),
        @AllowProperties(model = Quote.class, properties = {"quoteId", "no", "title", "amount", "status"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "address", "area", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.project.service.contract.update.Update {

    @AutoInject private Long contractId;
    @AutoInject private ContractStatus status;
    @AutoInject private LocalDate startDate;
    @AutoInject private LocalDate planEndDate;
    @AutoInject private String paymentTerms;

    @User private Employee employee;
    private final ProjectContractRepository contractRepository;
    private final ProjectContractContext contractContext;
    private final ProjectContext projectContext;
    private final TenantContext tenantContext;
    private final ProjectAccessContext accessContext;

    public Update(ProjectContractRepository contractRepository,
                  ProjectContractContext contractContext,
                  ProjectContext projectContext,
                  TenantContext tenantContext,
                  ProjectAccessContext accessContext) {
        this.contractRepository = contractRepository;
        this.contractContext = contractContext;
        this.projectContext = projectContext;
        this.tenantContext = tenantContext;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public ProjectContract call(ProjectContract request) {
        Employee current = tenantContext.requireEmployee(employee);
        Specification<ProjectContract> byId = (root, query, builder) ->
                builder.equal(root.get("contractId"), contractId);
        ProjectContract contract = contractRepository.findOne(byId.and(accessContext.visibleContract(employee)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "合同不存在或无权访问")));
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Designer) {
            throw new DefinedException(new DefaultErrorCode("403", "只有设计师或企业负责人可以处理签约"));
        }
        if (startDate != null) contract.setStartDate(startDate);
        if (planEndDate != null) contract.setPlanEndDate(planEndDate);
        if (paymentTerms != null) contract.setPaymentTerms(paymentTerms);

        if (status == null) {
            throw new DefinedException(new DefaultErrorCode("400", "合同动作不能为空"));
        }
        if (status == ContractStatus.Signed) {
            if (contract.getStatus() != ContractStatus.Draft
                    && contract.getStatus() != ContractStatus.PendingSign) {
                throw new DefinedException(new DefaultErrorCode("400", "只有草稿或待签约合同可以签约"));
            }
            contract.setStatus(ContractStatus.Signed);
            contract.setSignedAt(LocalDate.now());
            return contractContext.enrich(contractRepository.save(contract));
        }
        if (status == ContractStatus.Effective) {
            if (contract.getStatus() != ContractStatus.Signed || contract.getSignedAt() == null) {
                throw new DefinedException(new DefaultErrorCode("400", "合同签约后才能生效"));
            }
            contractContext.markEffective(contract);
            Project draft = new Project();
            draft.setCompany(contract.getCompany());
            draft.setName(contract.getCustomer().getName() + " · "
                    + (contract.getProperty().getAddress() == null ? "装修项目" : contract.getProperty().getAddress()));
            draft.setSiteAddr(contract.getProperty().getAddress());
            draft.setDesigner(contract.getQuote().getCreatedBy());
            draft.setStartDate(contract.getStartDate());
            draft.setPlanEndDate(contract.getPlanEndDate());
            Project project = projectContext.createFromEffectiveContract(contract, draft);
            contract.setProject(project);
            return contractContext.enrich(contractRepository.save(contract));
        }
        throw new DefinedException(new DefaultErrorCode("400", "当前仅支持合同签约或生效"));
    }
}
