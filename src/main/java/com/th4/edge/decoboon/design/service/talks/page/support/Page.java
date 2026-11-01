package com.th4.edge.decoboon.design.service.talks.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.TalkStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.property.model.Property;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/** 谈单 PPT 分页实现。 */
@Note("谈单PPT分页")
@Properties(allow = {
        @AllowProperties(model = Talk.class,
                properties = {"talkId", "no", "customer", "design", "property", "quote", "title", "version", "status", "coverUrl",
                        "outputUrl", "builtAt", "owner", "createTime"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "style", "status", "area", "coverUrl"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.quote.model.Quote.class, properties = {"quoteId", "no", "title", "status", "area", "amount"})
})
@Service("design/talk/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.design.service.talks.page.Page {

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private Long designId;

    @AutoInject
    private TalkStatus status;

    @AutoInject
    private Long customerId;

    @AutoInject
    private Long propertyId;

    @User
    private Employee employee;

    private final TalkRepository talkRepository;
    private final DataScopeContext dataScopeContext;

    public Page(TalkRepository talkRepository, DataScopeContext dataScopeContext) {
        this.talkRepository = talkRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public org.springframework.data.domain.Page<Talk> call(Talk request) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<Talk> condition = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (designId != null) {
                predicates.add(builder.equal(root.get("design").get("designId"), designId));
            }
            if (customerId != null) {
                predicates.add(builder.equal(root.get("customer").get("custId"), customerId));
            }
            if (propertyId != null) {
                predicates.add(builder.equal(root.get("property").get("propertyId"), propertyId));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return talkRepository.findAll(
                condition.and(DataScopeSpecification.visible(Talk.class, scope)),
                PageRequest.of(page, pageSize));
    }
}
