package com.th4.edge.decoboon.design.service.schemes.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/** 设计方案分页实现。 */
@Note("设计方案分页")
@Properties(allow = {
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "customer", "owner", "version", "style", "status", "area",
                        "layout", "coverUrl", "cadUrl", "createTime", "updateTime", "property"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/scheme/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.design.service.schemes.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private Long customerId;

    @AutoInject
    private DesignStatus status;

    private final DesignRepository designRepository;
    private final DesignContext designContext;

    public Page(DesignRepository designRepository, DesignContext designContext) {
        this.designRepository = designRepository;
        this.designContext = designContext;
    }

    @Override
    public org.springframework.data.domain.Page<Design> call(Design request) {
        Specification<Design> condition = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customerId != null) {
                predicates.add(builder.equal(root.get("customer").get("custId"), customerId));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 100 : Math.min(size, 200);
        return designRepository.findAll(
                condition.and(designContext.visibleReadableDesign(employee)),
                PageRequest.of(page, pageSize));
    }
}
