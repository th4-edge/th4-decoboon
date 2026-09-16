package com.th4.edge.decoboon.project.service.contract.save.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectContractContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 从客户确认报价创建合同草稿或待签合同。 */
@Note("创建合同基线")
@Description("报价确认后先形成合同，不直接创建项目")
@Properties(allow = {
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "project", "quote", "customer", "property", "baselineAmount",
                        "currentAmount", "status", "signedAt", "startDate", "planEndDate", "effectiveAt",
                        "paymentTerms", "createdBy", "remark", "receivableCount", "receivableAmount",
                        "receivedAmount", "pendingChangeCount", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status", "property" }),
        @AllowProperties(model = Quote.class, properties = {"quoteId", "no", "title", "amount", "status"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "address", "area", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.contract.save.Save {

    @AutoInject @NotNull private Long quoteId;
    @AutoInject private LocalDate signedAt;
    @AutoInject private LocalDate startDate;
    @AutoInject private LocalDate planEndDate;
    @AutoInject private String paymentTerms;
    @AutoInject private String remark;
    @User private Employee employee;

    private final QuoteRepository quoteRepository;
    private final TenantContext tenantContext;
    private final ProjectContractContext contractContext;

    public Save(QuoteRepository quoteRepository, TenantContext tenantContext,
                ProjectContractContext contractContext) {
        this.quoteRepository = quoteRepository;
        this.tenantContext = tenantContext;
        this.contractContext = contractContext;
    }

    @Override
    @Transactional
    public ProjectContract call(ProjectContract request) {
        Employee current = tenantContext.requireEmployee(employee);
        Quote quote = quoteRepository.findByQuoteIdAndCompany_ComId(quoteId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        if (quote.getStatus() != QuoteStatus.Accepted) {
            throw new DefinedException(new DefaultErrorCode("400", "只有客户已确认的报价可以生成合同"));
        }
        ProjectContract contract = contractContext.createFromQuote(
                quote, current, signedAt, startDate, planEndDate, remark);
        if (paymentTerms != null) {
            contract.setPaymentTerms(paymentTerms);
        }
        return contractContext.enrich(contract);
    }
}
