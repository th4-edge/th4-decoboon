package com.th4.edge.decoboon.customer.service.contract.info.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业主合同摘要实现，不返回报价成本、供应商或内部财务字段。 */
@Note("业主合同摘要")
@Properties(allow = {
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "project", "no", "baselineAmount", "signedAmount", "currentAmount", "status", "signedAt", "startDate",
                        "planEndDate", "paymentTerms", "property", "customer"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "propertyNo", "community", "roomNo", "address"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.Customer.class,
                properties = {"custId", "name", "community", "addr"})
})
@Service("customer/contract/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.customer.service.contract.info.Info {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long projectId;

    private final CustomerAccessContext accessContext;
    private final ProjectContractRepository contractRepository;

    public Info(CustomerAccessContext accessContext, ProjectContractRepository contractRepository) {
        this.accessContext = accessContext;
        this.contractRepository = contractRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectContract call(ProjectContract request) {
        Credentials credential = accessContext.requireCredential(principal);
        Project project = accessContext.requireProject(credential, projectId);
        return contractRepository.findByProject_ProjectIdAndCompany_ComId(
                project.getProjectId(), project.getCompany().getComId()).orElse(null);
    }
}
