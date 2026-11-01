package com.th4.edge.decoboon.content.service.template.page.support;

import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentTemplate;
import com.th4.edge.decoboon.content.repository.ContentTemplateRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 企业自有与系统内置内容模板分页。 */
@Note("内容模板选择器分页")
@Properties(allow = @AllowProperties(model = ContentTemplate.class,
        properties = {"templateId", "name", "format", "durationSec", "clipMin", "clipMax",
                "scriptSchema", "coverSchema", "allowedAssetTypes", "status", "version", "createTime", "updateTime"}))
@Service("content/template/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.content.service.template.page.Page {
    @User private Employee employee;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private String keyword;
    @AutoInject private String format;
    @AutoInject private ContentAssetStatus status;
    private final ContentTemplateRepository repository;
    private final ContentAccessContext accessContext;

    public Page(ContentTemplateRepository repository, ContentAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<ContentTemplate> call(ContentTemplate request) {
        Employee currentEmployee = accessContext.requireEmployee(employee);
        Long comId = currentEmployee.getCompany().getComId();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        Specification<ContentTemplate> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.or(
                    builder.equal(root.get("company").get("comId"), comId),
                    builder.isNull(root.get("company"))
            ));
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.like(builder.lower(root.get("name")), pattern));
            }
            if (format != null && !format.isBlank()) {
                predicates.add(builder.equal(root.get("format"), format.trim()));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        org.springframework.data.domain.Page<ContentTemplate> result = repository.findAll(
                specification,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "templateId"))
        );
        return new PageImpl<>(result.getContent(), PageRequest.of(page, pageSize), result.getTotalElements());
    }
}
