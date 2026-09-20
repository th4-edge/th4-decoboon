package com.th4.edge.decoboon.design.service.requirements.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.RequirementKind;
import com.th4.edge.decoboon.design.configure.RequirementStatus;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/** 客户诉求分页实现。 */
@Note("客户诉求分页")
@Properties(allow = {
        @AllowProperties(model = Requirement.class,
                properties = {"requirementId", "customer", "type", "spaceName", "kind", "category", "priority",
                        "status", "title", "detail", "response", "source", "confirmedAt", "owner", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/requirement/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.design.service.requirements.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private Long customerId;

    @AutoInject
    private Long propertyId;

    @AutoInject
    private RequirementKind kind;

    @AutoInject
    private RequirementStatus status;

    private final RequirementRepository requirementRepository;
    private final DataScopeContext dataScopeContext;

    public Page(RequirementRepository requirementRepository, DataScopeContext dataScopeContext) {
        this.requirementRepository = requirementRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public org.springframework.data.domain.Page<Requirement> call(Requirement request) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<Requirement> condition = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customerId != null) {
                predicates.add(builder.equal(root.get("customer").get("custId"), customerId));
            }
            if (propertyId != null) {
                predicates.add(builder.equal(root.get("property").get("propertyId"), propertyId));
            }
            if (kind != null) {
                predicates.add(builder.equal(root.get("kind"), kind));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 100 : Math.min(size, 200);
        return requirementRepository.findAll(
                condition.and(DataScopeSpecification.visible(Requirement.class, scope)),
                PageRequest.of(page, pageSize));
    }
}
