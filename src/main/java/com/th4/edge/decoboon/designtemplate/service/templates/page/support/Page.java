package com.th4.edge.decoboon.designtemplate.service.templates.page.support;

import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 设计模版分页：按可见范围（个人本人 / 本门店 / 本企业）服务端过滤与分页。 */
@Note("设计模版分页")
@Properties(allow = {
        @AllowProperties(model = DesignTemplate.class,
                // 列表不组装区域/分项（避免列表变重），也不返回企业/门店归属链明细
                properties = {"templateId", "scope", "status", "currentVersion", "sourceTemplate", "remark",
                        "createTime", "owner", "store"}),
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

    private final DesignTemplateRepository templateRepository;
    private final DesignTemplateAccessContext accessContext;

    public Page(DesignTemplateRepository templateRepository, DesignTemplateAccessContext accessContext) {
        this.templateRepository = templateRepository;
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
            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return templateRepository.findAll(specification,
                PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "templateId")));
    }
}
