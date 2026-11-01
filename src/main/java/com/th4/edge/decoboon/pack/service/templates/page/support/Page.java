package com.th4.edge.decoboon.pack.service.templates.page.support;

import com.th4.edge.decoboon.pack.configure.PackageTemplateStatus;
import com.th4.edge.decoboon.pack.context.PackageCostContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 当前企业装修套餐选择器分页。 */
@Note("装修套餐选择器分页")
@Properties(allow = {
        @AllowProperties(model = PackageTemplate.class,
                properties = {"templateId", "code", "version", "status", "currentPublished", "publishedAt", "snapshotHash",
                        "name", "area", "price", "discount", "cost", "itemCount", "sectionCount",
                        "createTime", "updateTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("pack/template/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.pack.service.templates.page.Page {

    @User private Employee employee;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private String keyword;
    @AutoInject private BigDecimal minArea;
    @AutoInject private BigDecimal maxArea;
    private final PackageTemplateRepository templateRepository;
    private final PackageLineRepository lineRepository;
    private final TenantContext tenantContext;
    private final PackageCostContext costContext;

    public Page(PackageTemplateRepository templateRepository, PackageLineRepository lineRepository,
                TenantContext tenantContext, PackageCostContext costContext) {
        this.templateRepository = templateRepository;
        this.lineRepository = lineRepository;
        this.tenantContext = tenantContext;
        this.costContext = costContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PackageTemplate> call(PackageTemplate request) {
        Employee currentEmployee = tenantContext.requireEmployee(employee);
        Long comId = currentEmployee.getCompany().getComId();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        Specification<PackageTemplate> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("company").get("comId"), comId));
            predicates.add(builder.equal(root.get("status"), PackageTemplateStatus.Published));
            predicates.add(builder.isTrue(root.get("currentPublished")));
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.like(builder.lower(root.get("name")), pattern));
            }
            if (minArea != null) predicates.add(builder.greaterThanOrEqualTo(root.get("area"), minArea));
            if (maxArea != null) predicates.add(builder.lessThanOrEqualTo(root.get("area"), maxArea));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        org.springframework.data.domain.Page<PackageTemplate> result = templateRepository.findAll(
                specification,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "code").and(Sort.by(Sort.Direction.DESC, "version")))
        );
        fillCounts(result.getContent());
        return costContext.mask(result, currentEmployee);
    }

    private void fillCounts(List<PackageTemplate> templates) {
        if (templates.isEmpty()) return;
        List<Long> ids = templates.stream().map(PackageTemplate::getTemplateId).toList();
        Map<Long, long[]> counts = new HashMap<>();
        for (Object[] row : lineRepository.summarizeByTemplateIds(ids)) {
            counts.put((Long) row[0], new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        for (PackageTemplate template : templates) {
            long[] count = counts.getOrDefault(template.getTemplateId(), new long[]{0, 0});
            template.setItemCount((int) count[0]);
            template.setSectionCount((int) count[1]);
        }
    }
}
