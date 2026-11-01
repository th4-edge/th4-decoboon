package com.th4.edge.decoboon.supply.service.partnerprice.page.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
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

/** 合作方价格分页。 */
@Note("合作方价格分页")
@Properties(allow = {
        @AllowProperties(model = PartnerPrice.class,
                properties = {"partnerPriceId", "partner", "item", "purchasePrice", "laborPrice",
                        "minimumOrder", "leadTimeDays", "settlementDays", "effectiveFrom", "effectiveTo",
                        "active", "remark", "createTime"}),
        @AllowProperties(model = Partner.class,
                properties = {"partnerId", "type", "name", "contactName", "telNo", "status", "settlementTerms",
                        "settlementMode", "settlementDays", "cutoffDay"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "code", "category", "name", "unit", "brand", "series", "spec",
                        "pricingMode", "materialPrice", "manualPrice", "fixedTotalPrice", "active"})
})
@Service("partner-price/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.supply.service.partnerprice.page.Page {
    @User private Employee employee;
    @AutoInject private Long partnerPriceId;
    @AutoInject private Long partnerId;
    @AutoInject private Long itemId;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final TenantContext tenantContext;
    private final PartnerPriceRepository priceRepository;

    public Page(TenantContext tenantContext, PartnerPriceRepository priceRepository) {
        this.tenantContext = tenantContext;
        this.priceRepository = priceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PartnerPrice> call(PartnerPrice request) {
        Employee currentEmployee = tenantContext.requireRole(employee, EmployeeRole.Boss,
                EmployeeRole.StoreManager, EmployeeRole.Finance);
        Long comId = currentEmployee.getCompany().getComId();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        Specification<PartnerPrice> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("company").get("comId"), comId));
            if (partnerPriceId != null) {
                predicates.add(builder.equal(root.get("partnerPriceId"), partnerPriceId));
            }
            if (partnerId != null) {
                predicates.add(builder.equal(root.get("partner").get("partnerId"), partnerId));
            }
            if (itemId != null) {
                predicates.add(builder.equal(root.get("item").get("itemId"), itemId));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        org.springframework.data.domain.Page<PartnerPrice> result = priceRepository.findAll(
                specification,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "partnerPriceId"))
        );
        return new PageImpl<>(result.getContent(), PageRequest.of(page, pageSize), result.getTotalElements());
    }
}
