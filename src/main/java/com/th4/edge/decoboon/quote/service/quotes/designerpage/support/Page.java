package com.th4.edge.decoboon.quote.service.quotes.designerpage.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteBusinessStateContext;
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

/** 设计师或 BOSS 报价分页实现，筛选、计数和切页均在数据库完成，响应不包含成本字段。 */
@Note("设计师报价分页")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "packageTemplate", "packageTemplateCode", "packageTemplateVersion", "packageTemplateSnapshotHash", "createdBy", "version", "mode",
                        "area", "subtotal", "amount", "designFee", "managementFee", "discount", "taxRate", "taxAmount", "status",
                        "submittedAt", "rejectedAt", "acceptedAt", "convertedAt", "createTime", "updateTime", "property", "businessStateView"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "area", "budget", "status"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout"}),
        @AllowProperties(model = com.th4.edge.decoboon.pack.model.PackageTemplate.class,
                properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price", "discount", "snapshotHash"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.common.model.BusinessStateView.class, properties = {"objectKey", "objectId", "status", "statusLabel", "statusTone", "derivedLabels", "nextAction", "blockedReasons", "nextOwner", "updatedAt", "businessRefs"}),
        @AllowProperties(model = com.th4.edge.decoboon.common.model.BusinessDerivedLabel.class, properties = {"key", "label", "tone"}),
        @AllowProperties(model = com.th4.edge.decoboon.common.model.BusinessNextAction.class, properties = {"key", "label", "hint", "ownerRole", "fromStatus", "toStatus", "enabled", "blockedReasons", "missingFields", "reloadScopes", "businessRefs"})
})
@Service("quote/designer/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.quote.service.quotes.designerpage.Page {

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
    private Long designId;

    @AutoInject
    private QuoteStatus status;

    @AutoInject
    private String keyword;

    private final QuoteRepository quoteRepository;
    private final TenantContext tenantContext;
    private final QuoteBusinessStateContext quoteBusinessStateContext;

    public Page(QuoteRepository quoteRepository, TenantContext tenantContext,
                QuoteBusinessStateContext quoteBusinessStateContext) {
        this.quoteRepository = quoteRepository;
        this.tenantContext = tenantContext;
        this.quoteBusinessStateContext = quoteBusinessStateContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Quote> call(Quote request) {
        Employee currentEmployee = tenantContext.requireEmployee(employee);
        EmployeeRole role = currentEmployee.getRole();
        // DEC-040 / BR-033：店长只读本店；设计师只读本人创建；BOSS 读本企业。
        if (role != EmployeeRole.Boss && role != EmployeeRole.Designer && role != EmployeeRole.StoreManager) {
            throw new DefinedException(new DefaultErrorCode("403", "仅设计师、店长或企业负责人可访问设计与报价列表"));
        }
        Long comId = currentEmployee.getCompany().getComId();
        Long empId = currentEmployee.getEmpId();
        Long storeId = role == EmployeeRole.StoreManager
                ? currentEmployee.getStore() == null ? null : currentEmployee.getStore().getStoreId()
                : null;
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        if (role == EmployeeRole.StoreManager && storeId == null) {
            return new PageImpl<>(List.of(), PageRequest.of(page, pageSize), 0);
        }

        Specification<Quote> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("company").get("comId"), comId));
            if (role == EmployeeRole.StoreManager) {
                predicates.add(criteriaBuilder.equal(root.get("customer").get("store").get("storeId"), storeId));
            } else if (role == EmployeeRole.Designer) {
                predicates.add(criteriaBuilder.equal(root.get("createdBy").get("empId"), empId));
            }
            if (customerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("customer").get("custId"), customerId));
            }
            if (propertyId != null) {
                predicates.add(criteriaBuilder.equal(root.get("property").get("propertyId"), propertyId));
            }
            if (designId != null) {
                predicates.add(criteriaBuilder.equal(root.get("design").get("designId"), designId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("no")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customer").get("name")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        org.springframework.data.domain.Page<Quote> pageQuotes = quoteRepository.findAll(
                specification,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "quoteId"))
        );
        pageQuotes.forEach(quoteBusinessStateContext::enrich);
        return pageQuotes;
    }
}
