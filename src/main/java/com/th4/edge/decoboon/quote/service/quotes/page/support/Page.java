package com.th4.edge.decoboon.quote.service.quotes.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 客户报价分页实现。 */
@Note("客户报价分页")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "packageTemplate", "packageTemplateCode", "packageTemplateVersion", "packageTemplateSnapshotHash", "createdBy", "version", "mode",
                        "area", "subtotal", "amount", "cost", "costComplete", "designFee", "managementFee", "discount", "taxRate", "taxAmount", "status", "createTime", "property"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "area", "budget", "status"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout"}),
        @AllowProperties(model = com.th4.edge.decoboon.pack.model.PackageTemplate.class,
                properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price", "discount", "snapshotHash"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("quote/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.quote.service.quotes.page.Page {

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
    private QuoteStatus status;

    @AutoInject
    private String keyword;

    private final QuoteRepository quoteRepository;
    private final TenantContext tenantContext;

    public Page(QuoteRepository quoteRepository, TenantContext tenantContext) {
        this.quoteRepository = quoteRepository;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Quote> call(Quote request) {
        Employee currentEmployee = tenantContext.requireEmployee(employee);
        EmployeeRole role = currentEmployee.getRole();
        if (role == EmployeeRole.Designer) {
            throw new DefinedException(new DefaultErrorCode("403", "设计师请使用设计师报价列表服务"));
        }
        Long comId = currentEmployee.getCompany().getComId();
        Long empId = currentEmployee.getEmpId();
        Long storeId = currentEmployee.getStore() == null ? null : currentEmployee.getStore().getStoreId();
        Long teamId = currentEmployee.getTeam() == null ? null : currentEmployee.getTeam().getTeamId();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);

        if (role == EmployeeRole.StoreManager && storeId == null) {
            return new PageImpl<>(List.of(), PageRequest.of(page, pageSize), 0);
        }
        if (role == EmployeeRole.TeamLeader && teamId == null) {
            return new PageImpl<>(List.of(), PageRequest.of(page, pageSize), 0);
        }

        Specification<Quote> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("company").get("comId"), comId));
            switch (role) {
                case Boss -> { }
                case StoreManager -> predicates.add(criteriaBuilder.equal(
                        root.get("customer").get("store").get("storeId"), storeId));
                case TeamLeader -> predicates.add(criteriaBuilder.equal(
                        root.get("customer").get("team").get("teamId"), teamId));
                case Marketer -> predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("customer").get("owner").get("empId"), empId),
                        criteriaBuilder.equal(root.get("property").get("salesman").get("empId"), empId)));
                default -> predicates.add(criteriaBuilder.disjunction());
            }
            if (customerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("customer").get("custId"), customerId));
            }
            if (propertyId != null) {
                predicates.add(criteriaBuilder.equal(root.get("property").get("propertyId"), propertyId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customer").get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("property").get("community")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("property").get("address")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        return quoteRepository.findAll(
                specification,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "quoteId"))
        );
    }
}
