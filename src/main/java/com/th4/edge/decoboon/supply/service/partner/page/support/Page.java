package com.th4.edge.decoboon.supply.service.partner.page.support;

import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 合作方分页。 */
@Note("合作方分页")
@Properties(allow = @AllowProperties(model = Partner.class,
        properties = {"partnerId", "type", "name", "contactName", "telNo", "taxNo", "addr",
                "settlementTerms", "settlementMode", "settlementDays", "cutoffDay", "bankName", "bankAccount", "status", "remark", "brands", "paymentCodeImages", "createTime",
                "payableAmount", "paidAmount", "outstandingAmount"}))
@Service("partner/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.supply.service.partner.page.Page {
    @User private Employee employee;
    @AutoInject private Long partnerId;
    @AutoInject private PartnerType type;
    @AutoInject private PartnerStatus status;
    @AutoInject private String brand;
    @AutoInject private String keyword;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final TenantContext tenantContext;
    private final PartnerRepository partnerRepository;
    private final PayableRepository payableRepository;

    public Page(TenantContext tenantContext, PartnerRepository partnerRepository,
                PayableRepository payableRepository) {
        this.tenantContext = tenantContext;
        this.partnerRepository = partnerRepository;
        this.payableRepository = payableRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Partner> call(Partner request) {
        Employee currentEmployee = tenantContext.requireEmployee(employee);
        Long comId = currentEmployee.getCompany().getComId();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);

        Specification<Partner> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("company").get("comId"), comId));
            if (partnerId != null) {
                predicates.add(builder.equal(root.get("partnerId"), partnerId));
            }
            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            Join<Partner, String> brands = null;
            if ((brand != null && !brand.isBlank()) || (keyword != null && !keyword.isBlank())) {
                brands = root.join("brands", JoinType.LEFT);
                query.distinct(true);
            }
            if (brand != null && !brand.isBlank()) {
                predicates.add(builder.equal(builder.lower(brands), brand.trim().toLowerCase(Locale.ROOT)));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("contactName")), pattern),
                        builder.like(builder.lower(root.get("telNo")), pattern),
                        builder.like(builder.lower(root.get("settlementTerms")), pattern),
                        builder.like(builder.lower(root.get("addr")), pattern),
                        builder.like(builder.lower(brands), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        org.springframework.data.domain.Page<Partner> result = partnerRepository.findAll(
                specification,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "partnerId"))
        );
        List<Long> ids = result.getContent().stream().map(Partner::getPartnerId).toList();
        if (!ids.isEmpty()) {
            partnerRepository.findDetailsByIdIn(comId, ids);
            applyPayableSummaries(comId, result.getContent());
        }
        return new PageImpl<>(result.getContent(), PageRequest.of(page, pageSize), result.getTotalElements());
    }

    private void applyPayableSummaries(Long comId, List<Partner> partners) {
        List<Long> ids = partners.stream().map(Partner::getPartnerId).toList();
        Map<Long, BigDecimal[]> summaries = new HashMap<>();
        for (Object[] row : payableRepository.summarizePartnerPayables(comId, ids)) {
            summaries.put((Long) row[0], new BigDecimal[]{value(row[1]), value(row[2])});
        }
        for (Partner partner : partners) {
            BigDecimal[] summary = summaries.getOrDefault(partner.getPartnerId(),
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            partner.setPayableAmount(summary[0]);
            partner.setPaidAmount(summary[1]);
            partner.setOutstandingAmount(summary[0].subtract(summary[1]));
        }
    }

    private BigDecimal value(Object value) {
        return value instanceof BigDecimal decimal ? decimal : BigDecimal.ZERO;
    }
}
