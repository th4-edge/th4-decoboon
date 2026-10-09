package com.th4.edge.decoboon.designtemplate.service.templates.page.support;

import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateItemRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRoomRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 设计模版分页：按可见范围（个人本人 / 本门店 / 本企业）服务端过滤与分页。 */
@Note("设计模版分页")
@Properties(allow = {
        @AllowProperties(model = DesignTemplate.class,
                properties = {"templateId", "scope", "status", "currentVersion", "sourceTemplate", "remark",
                        "createTime", "updateTime", "owner", "store",
                        // 列表派生计数：当前版本区域数/分项数、实例化使用次数
                        "roomCount", "itemCount", "usageCount"}),
        @AllowProperties(model = DesignTemplateVersion.class,
                properties = {"versionId", "version", "status", "name", "houseType", "areaMin", "areaMax",
                        "tags", "publishedAt"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("design/template/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.designtemplate.service.templates.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private DesignTemplate.Scope scope;

    @AutoInject
    private DesignTemplate.Status status;

    @AutoInject
    private String keyword;

    @AutoInject
    private String houseType;

    @AutoInject
    private BigDecimal areaMin;

    @AutoInject
    private BigDecimal areaMax;

    @AutoInject
    private String tag;

    private final DesignTemplateRepository templateRepository;
    private final DesignTemplateRoomRepository roomRepository;
    private final DesignTemplateItemRepository itemRepository;
    private final DesignRepository designRepository;
    private final DesignTemplateAccessContext accessContext;

    public Page(DesignTemplateRepository templateRepository,
                DesignTemplateRoomRepository roomRepository,
                DesignTemplateItemRepository itemRepository,
                DesignRepository designRepository,
                DesignTemplateAccessContext accessContext) {
        this.templateRepository = templateRepository;
        this.roomRepository = roomRepository;
        this.itemRepository = itemRepository;
        this.designRepository = designRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DesignTemplate> call(DesignTemplate request) {
        Employee me = accessContext.requireEmployee(employee);
        Long comId = me.getCompany().getComId();
        Long storeId = me.getStore() == null ? null : me.getStore().getStoreId();
        int pageIndex = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);

        Specification<DesignTemplate> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("company").get("comId"), comId));

            Predicate companyScope = builder.equal(root.get("scope"), DesignTemplate.Scope.Company);
            Predicate personalScope = builder.and(
                    builder.equal(root.get("scope"), DesignTemplate.Scope.Personal),
                    builder.equal(root.get("owner").get("empId"), me.getEmpId()));
            if (storeId == null) {
                predicates.add(builder.or(companyScope, personalScope));
            } else {
                Predicate storeScope = builder.and(
                        builder.equal(root.get("scope"), DesignTemplate.Scope.Store),
                        builder.equal(root.get("store").get("storeId"), storeId));
                predicates.add(builder.or(companyScope, storeScope, personalScope));
            }

            if (scope != null) {
                predicates.add(builder.equal(root.get("scope"), scope));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            } else {
                predicates.add(builder.notEqual(root.get("status"), DesignTemplate.Status.Disabled));
            }

            boolean needsVersionJoin = hasText(keyword) || hasText(houseType) || areaMin != null || areaMax != null || hasText(tag);
            if (needsVersionJoin) {
                Join<DesignTemplate, DesignTemplateVersion> version = root.join("currentVersion", JoinType.LEFT);
                if (hasText(keyword)) {
                    String like = "%" + keyword.trim() + "%";
                    predicates.add(builder.or(
                            builder.like(version.get("name"), like),
                            builder.like(version.get("houseType"), like),
                            builder.like(version.get("tags"), like)));
                }
                if (hasText(houseType)) {
                    predicates.add(builder.like(version.get("houseType"), "%" + houseType.trim() + "%"));
                }
                if (areaMin != null) {
                    predicates.add(builder.greaterThanOrEqualTo(version.get("areaMax"), areaMin));
                }
                if (areaMax != null) {
                    predicates.add(builder.lessThanOrEqualTo(version.get("areaMin"), areaMax));
                }
                if (hasText(tag)) {
                    predicates.add(builder.like(version.get("tags"), "%" + tag.trim() + "%"));
                }
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };

        org.springframework.data.domain.Page<DesignTemplate> page = templateRepository.findAll(specification,
                PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "templateId")));
        fillCounts(page.getContent(), comId);
        return page;
    }

    /** 列表计数一次分组查询：区域数/分项数按当前版本，使用次数按模板实例化方案数。 */
    private void fillCounts(List<DesignTemplate> templates, Long comId) {
        if (templates.isEmpty()) return;
        List<Long> versionIds = templates.stream()
                .map(DesignTemplate::getCurrentVersion)
                .filter(java.util.Objects::nonNull)
                .map(DesignTemplateVersion::getVersionId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        List<Long> templateIds = templates.stream()
                .map(DesignTemplate::getTemplateId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Long> roomCounts = versionIds.isEmpty()
                ? Map.of() : toCountMap(roomRepository.countByVersionIds(comId, versionIds));
        Map<Long, Long> itemCounts = versionIds.isEmpty()
                ? Map.of() : toCountMap(itemRepository.countByVersionIds(comId, versionIds));
        Map<Long, Long> usageCounts = templateIds.isEmpty()
                ? Map.of() : toCountMap(designRepository.countUsageByTemplateIds(comId, templateIds));
        for (DesignTemplate template : templates) {
            Long versionId = template.getCurrentVersion() == null ? null : template.getCurrentVersion().getVersionId();
            template.setRoomCount(versionId == null ? 0L : roomCounts.getOrDefault(versionId, 0L));
            template.setItemCount(versionId == null ? 0L : itemCounts.getOrDefault(versionId, 0L));
            template.setUsageCount(usageCounts.getOrDefault(template.getTemplateId(), 0L));
        }
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null || row[1] == null) continue;
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return result;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
