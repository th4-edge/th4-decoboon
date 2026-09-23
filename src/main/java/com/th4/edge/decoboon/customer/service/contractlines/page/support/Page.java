package com.th4.edge.decoboon.customer.service.contractlines.page.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.context.CustomerContractContext;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 业主合同范围只读分页实现。 */
@Note("业主合同范围")
@Properties(allow = {
        @AllowProperties(model = ContractLine.class,
                properties = {"lineId", "contract", "property", "type", "category", "name", "brand", "spec",
                        "unit", "quantity", "amount", "spaceName", "sort"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "currentAmount", "status"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address"})
})
@Service("customer/contract-line/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.contractlines.page.Page {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    private Long projectId;

    @AutoInject
    private Long contractId;

    private final CustomerAccessContext accessContext;
    private final CustomerContractContext contractContext;
    private final ContractLineRepository lineRepository;

    public Page(CustomerAccessContext accessContext,
                CustomerContractContext contractContext,
                ContractLineRepository lineRepository) {
        this.accessContext = accessContext;
        this.contractContext = contractContext;
        this.lineRepository = lineRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ContractLine> call(ContractLine request) {
        Credentials credential = accessContext.requireCredential(principal);
        ProjectContract contract = contractContext.requireContract(credential, projectId, contractId);
        List<ContractLine> lines = lineRepository.findByContractOrderBySortAsc(contract);
        return new PageImpl<>(lines);
    }
}
