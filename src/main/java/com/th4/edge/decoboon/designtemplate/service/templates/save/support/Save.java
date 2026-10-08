package com.th4.edge.decoboon.designtemplate.service.templates.save.support;

import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateVersionRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.model.Company;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 设计模版创建：同时创建第 1 版草稿。
 *
 * <p>模版主档只存身份与治理，名称/房型/面积区间/标签落在版本上，保证发布后不可变。
 * 保存不校验审批（发布不设审批），但创建企业级/门店级需要管理权限。</p>
 */
@Note("设计模版创建")
@Description("创建设计模版及其第 1 版草稿")
@Properties(allow = {
        @AllowProperties(model = DesignTemplate.class,
                properties = {"templateId", "scope", "status", "currentVersion", "remark", "createTime", "owner", "store"}),
        @AllowProperties(model = DesignTemplateVersion.class,
                properties = {"versionId", "version", "status", "name", "houseType", "areaMin", "areaMax", "tags"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "color"})
})
@Service("design/template/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.designtemplate.service.templates.save.Save {

    @AutoInject
    @NotEmpty
    private String name;

    @AutoInject
    @NotEmpty
    private String houseType;

    @AutoInject
    private BigDecimal areaMin;

    @AutoInject
    private BigDecimal areaMax;

    @AutoInject
    private String tags;

    @AutoInject
    private DesignTemplate.Scope scope;

    @AutoInject
    private String remark;

    @User
    private Employee employee;

    private final DesignTemplateRepository templateRepository;
    private final DesignTemplateVersionRepository versionRepository;
    private final DesignTemplateAccessContext accessContext;
    private final TenantContext tenantContext;

    public Save(DesignTemplateRepository templateRepository,
                DesignTemplateVersionRepository versionRepository,
                DesignTemplateAccessContext accessContext,
                TenantContext tenantContext) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.accessContext = accessContext;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional
    public DesignTemplate call(@ModelSaveInject DesignTemplate request) {
        Employee me = accessContext.requireEmployee(employee);
        if (me.getCompany() == null) {
            throw new com.th4.edge.framework.exception.DefinedException(
                    new com.th4.edge.framework.exception.DefaultErrorCode("403", "当前账号未加入企业"));
        }
        DesignTemplate.Scope targetScope = scope == null ? DesignTemplate.Scope.Personal : scope;
        accessContext.requireCreate(me, targetScope);

        DesignTemplate template = new DesignTemplate();
        template.setCompany(me.getCompany());
        template.setOwner(me);
        template.setScope(targetScope);
        template.setStatus(DesignTemplate.Status.Draft);
        template.setRemark(remark);
        if (targetScope == DesignTemplate.Scope.Store) {
            template.setStore(me.getStore());
        }
        templateRepository.save(template);

        DesignTemplateVersion version = new DesignTemplateVersion();
        version.setCompany(me.getCompany());
        version.setTemplate(template);
        version.setVersion(1);
        version.setStatus(DesignTemplate.Status.Draft);
        version.setName(name);
        version.setHouseType(houseType);
        version.setAreaMin(areaMin);
        version.setAreaMax(areaMax);
        version.setTags(tags);
        versionRepository.save(version);

        template.setCurrentVersion(version);
        return templateRepository.save(template);
    }
}
