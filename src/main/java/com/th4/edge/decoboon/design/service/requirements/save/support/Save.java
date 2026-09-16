package com.th4.edge.decoboon.design.service.requirements.save.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.design.configure.RequirementStatus;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 客户诉求保存实现。 */
@Note("客户诉求保存")
@Properties(allow = {
        @AllowProperties(model = Requirement.class,
                properties = {"requirementId", "customer", "property", "type", "spaceName", "kind", "category", "priority",
                        "status", "title", "detail", "response", "source", "confirmedAt", "owner", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "propertyNo", "address", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/requirement/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.design.service.requirements.save.Save {

    @AutoInject
    @NotNull
    private Long customerId;

    @AutoInject
    private Long propertyId;

    @AutoInject
    private Long ownerId;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final RequirementRepository requirementRepository;
    private final PropertyContext propertyContext;

    public Save(DesignContext designContext, RequirementRepository requirementRepository,
                PropertyContext propertyContext) {
        this.designContext = designContext;
        this.requirementRepository = requirementRepository;
        this.propertyContext = propertyContext;
    }

    @Override
    public Requirement call(@ModelSaveInject Requirement request) {
        Employee current = designContext.requireEmployee(employee);
        Customer customer = designContext.requireCustomer(customerId, employee);
        Employee fallback = customer.getDesigner() == null ? current : customer.getDesigner();
        Employee owner = designContext.resolveOwner(current, ownerId, fallback);
        Property property = propertyContext.resolve(propertyId, customer, current);
        designContext.requirePropertyOpen(property);
        designContext.apply(request, customer, owner);
        request.setProperty(property);
        request.setStatus(RequirementStatus.Pending);
        request.setConfirmedAt(null);
        return requirementRepository.save(request);
    }
}
