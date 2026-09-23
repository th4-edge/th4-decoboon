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
    private Long targetPropertyId;

    @AutoInject
    private Long requirementId;

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
        Requirement target = resolveTarget(request, customer);
        Employee fallback = customer.getDesigner() == null ? current : customer.getDesigner();
        Employee owner = designContext.resolveOwner(current, ownerId, fallback);
        Long resolvedPropertyId = targetPropertyId != null ? targetPropertyId : propertyId;
        if (resolvedPropertyId == null && request.getProperty() != null) {
            resolvedPropertyId = request.getProperty().getPropertyId();
        }
        Property property = propertyContext.resolve(resolvedPropertyId, customer, current);
        designContext.requirePropertyOpen(property);
        copyEditableFields(request, target);
        designContext.apply(target, customer, owner, property);
        target.setStatus(RequirementStatus.Pending);
        target.setConfirmedAt(null);
        return requirementRepository.save(target);
    }

    private Requirement resolveTarget(Requirement request, Customer customer) {
        if (requirementId == null) {
            return request;
        }
        Requirement target = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "客户诉求不存在或无权访问")));
        if (target.getCompany() == null || customer.getCompany() == null
                || !target.getCompany().getComId().equals(customer.getCompany().getComId())
                || target.getCustomer() == null
                || !target.getCustomer().getCustId().equals(customer.getCustId())) {
            throw new DefinedException(new DefaultErrorCode("404", "客户诉求不存在或无权访问"));
        }
        return target;
    }

    private void copyEditableFields(Requirement source, Requirement target) {
        target.setType(source.getType());
        target.setSpaceName(source.getSpaceName());
        target.setKind(source.getKind());
        target.setCategory(source.getCategory());
        target.setPriority(source.getPriority());
        target.setTitle(source.getTitle());
        target.setDetail(source.getDetail());
        target.setResponse(source.getResponse());
        target.setSource(source.getSource());
    }
}
