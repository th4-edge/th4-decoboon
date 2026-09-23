package com.th4.edge.decoboon.finance.service.receivables.save.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 应收计划创建实现。 */
@Note("应收计划创建")
@Properties(allow = {
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "project", "property", "customer", "name", "stageName", "stageCode", "triggerType", "type", "amount",
                        "receivedAmount", "dueDate", "status", "invoiceRequired", "invoiceNo", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status" }),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/receivable/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.receivables.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long projectId;
    @AutoInject
    @NotEmpty
    private String name;
    @AutoInject
    private String stageName;
    @AutoInject
    private ReceivableType type;
    @AutoInject
    @NotNull
    private BigDecimal amount;
    @AutoInject
    @NotNull
    private LocalDate dueDate;
    @AutoInject
    private Boolean invoiceRequired;
    @AutoInject
    private String invoiceNo;
    @AutoInject
    private String remark;

    private final ReceivableRepository receivableRepository;
    private final FinanceAccessContext accessContext;
    private final ProjectContractRepository contractRepository;

    public Save(ReceivableRepository receivableRepository, FinanceAccessContext accessContext,
                ProjectContractRepository contractRepository) {
        this.receivableRepository = receivableRepository;
        this.accessContext = accessContext;
        this.contractRepository = contractRepository;
    }

    @Override
    @Transactional
    public Receivable call(Receivable request) {
        Employee current = accessContext.requireEditor(employee);
        BigDecimal total = amount == null ? BigDecimal.ZERO : amount;
        if (total.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "应收金额必须大于 0"));
        }
        Project project = accessContext.resolveProject(current, projectId);
        request.setCompany(current.getCompany());
        request.setProject(project);
        request.setProperty(project.getProperty());
        contractRepository.findByProject(project).ifPresent(request::setContract);
        request.setName(name.trim());
        request.setStageName(stageName);
        request.setType(type == null ? ReceivableType.Other : type);
        request.setAmount(total);
        request.setReceivedAmount(BigDecimal.ZERO);
        request.setDueDate(dueDate);
        request.setInvoiceRequired(Boolean.TRUE.equals(invoiceRequired));
        request.setInvoiceNo(invoiceNo);
        request.setRemark(remark);
        request.setCreatedBy(current);
        accessContext.applyOwnership(request, current, project);
        accessContext.refreshReceivableStatus(request);
        return receivableRepository.save(request);
    }
}
