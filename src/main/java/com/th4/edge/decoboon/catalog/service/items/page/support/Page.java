package com.th4.edge.decoboon.catalog.service.items.page.support;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/** 按类型、分类、品牌、规格分页加载材料库。 */
@Note("家装材料库分页")
@Properties(allow = {
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "code", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "fixedTotalPrice", "costPrice", "formula", "craft",
                        "criterion", "description", "images", "active", "sort", "maintainer", "createTime", "updateTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("catalog/item/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.catalog.service.items.page.Page {

    @AutoInject
    @NotNull
    private ItemType type;

    @AutoInject
    private String category;

    @AutoInject
    private String brand;

    @AutoInject
    private String series;

    @AutoInject
    private String spec;

    @AutoInject
    private String keyword;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private Boolean includeInactive;

    @User
    private Employee employee;

    private final CatalogItemRepository repository;
    private final CatalogCostContext costContext;
    private final CatalogContext context;

    public Page(CatalogItemRepository repository, CatalogCostContext costContext, CatalogContext context) {
        this.repository = repository;
        this.costContext = costContext;
        this.context = context;
    }

    @Override
    public org.springframework.data.domain.Page<CatalogItem> call(CatalogItem request) {
        Employee currentEmployee = context.requireEmployee(employee);
        Specification<CatalogItem> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("company").get("comId"), currentEmployee.getCompany().getComId()));
            predicates.add(builder.equal(root.get("type"), type));
            if (!Boolean.TRUE.equals(includeInactive)) {
                predicates.add(builder.isTrue(root.get("active")));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(builder.equal(root.get("category"), category.trim()));
            }
            if (brand != null && !brand.isBlank()) {
                predicates.add(builder.equal(root.get("brand"), brand.trim()));
            }
            if (series != null && !series.isBlank()) {
                predicates.add(builder.equal(root.get("series"), series.trim()));
            }
            if (spec != null && !spec.isBlank()) {
                predicates.add(builder.equal(root.get("spec"), spec.trim()));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), like),
                        builder.like(builder.lower(root.get("code")), like),
                        builder.like(builder.lower(root.get("category")), like),
                        builder.like(builder.lower(root.get("brand")), like),
                        builder.like(builder.lower(root.get("series")), like),
                        builder.like(builder.lower(root.get("spec")), like),
                        // 拼音首字母检索：输入 QGLG 可命中「轻钢龙骨石膏板吊平顶」
                        builder.like(builder.lower(root.get("nameInitials")), like)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 30 : Math.min(size, 100);
        return costContext.mask(repository.findAll(specification,
                PageRequest.of(page, pageSize, Sort.by("category", "brand", "series", "name"))), currentEmployee);
    }
}
