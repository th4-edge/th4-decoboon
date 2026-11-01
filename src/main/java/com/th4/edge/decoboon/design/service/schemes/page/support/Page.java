package com.th4.edge.decoboon.design.service.schemes.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.DesignPriceContext;
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
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/** 设计方案分页实现。 */
@Note("设计方案分页")
@Properties(allow = {
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "customer", "owner", "store", "version", "style", "status", "area",
                        "layout", "coverUrl", "cadUrl", "createTime", "updateTime", "property",
                        "calculatedAmount", "spaceCount", "itemCount", "currentQuoteId", "currentQuoteNo",
                        "currentQuoteVersion", "currentQuoteStatus", "currentQuoteStatusName", "currentQuoteAmount"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore", "status"})
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
    private Long propertyId;

    @AutoInject
    private DesignStatus status;

    @AutoInject
    private List<DesignStatus> statuses;

    @AutoInject
    private Long ownerId;

    @AutoInject
    private Long storeId;

    @AutoInject
    private String keyword;

    private final DesignRepository designRepository;
    private final DesignContext designContext;
    private final DesignPriceContext priceContext;
    private final StoreFilterContext storeFilterContext;

    public Page(DesignRepository designRepository, DesignContext designContext,
                DesignPriceContext priceContext, StoreFilterContext storeFilterContext) {
        this.designRepository = designRepository;
        this.designContext = designContext;
        this.priceContext = priceContext;
        this.storeFilterContext = storeFilterContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Design> call(Design request) {
        Employee currentEmployee = designContext.requireEmployee(employee);
        Long effectiveStoreId = storeFilterContext.resolve(currentEmployee, storeId);
        Specification<Design> condition = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customerId != null) {
                predicates.add(builder.equal(root.get("customer").get("custId"), customerId));
            }
            if (propertyId != null) {
                predicates.add(builder.equal(root.get("property").get("propertyId"), propertyId));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (ownerId != null) {
                predicates.add(builder.equal(root.get("owner").get("empId"), ownerId));
            }
            if (effectiveStoreId != null && effectiveStoreId > 0) {
                predicates.add(builder.equal(root.get("store").get("storeId"), effectiveStoreId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), like),
                        builder.like(builder.lower(root.get("no")), like),
                        builder.like(builder.lower(root.get("customer").get("name")), like),
                        builder.like(builder.lower(root.get("property").get("address")), like)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        org.springframework.data.domain.Page<Design> result = designRepository.findAll(
                condition.and(designContext.visibleReadableDesign(employee)),
                PageRequest.of(page, pageSize));
        priceContext.enrich(result.getContent());
        return result;
    }
}
