package com.th4.edge.decoboon.designtemplate.service.templates.disable.support;

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
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 停用设计模版。
 *
 * <p>停用是治理状态，不删除模版，也不影响历史报价（BR-040 §5）。
 * 当前草稿版本同步置为 Disabled，确保停用后草稿不可继续编辑；
 * 已发布版本保持 Published，历史引用不变。停用后不可被实例化或新建版本。</p>
 */
@Note("设计模版停用")
@Description("停用模版；当前草稿同步置为不可编辑，历史报价不受影响")
@Properties(allow = {
        @AllowProperties(model = DesignTemplate.class,
                properties = {"templateId", "scope", "status", "currentVersion", "sourceTemplate", "remark",
                        "owner", "store"}),
        @AllowProperties(model = DesignTemplateVersion.class,
                properties = {"versionId", "version", "status", "name", "houseType", "areaMin", "areaMax",
                        "tags", "publishedAt"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Store.class, properties = {"storeId", "name", "status"})
})
@Service("design/template/disable")
@Scope(ScopeTypes.PROTOTYPE)
public class Disable implements com.th4.edge.decoboon.designtemplate.service.templates.disable.Disable {

    @AutoInject
    private Long templateId;

    @User
    private Employee employee;

    private final DesignTemplateRepository templateRepository;
    private final DesignTemplateVersionRepository versionRepository;
    private final DesignTemplateAccessContext accessContext;

    public Disable(DesignTemplateRepository templateRepository,
                   DesignTemplateVersionRepository versionRepository,
                   DesignTemplateAccessContext accessContext) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public DesignTemplate call(DesignTemplate request) {
        Employee me = accessContext.requireEmployee(employee);
        Long comId = me.getCompany().getComId();
        if (templateId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "templateId不能为空"));
        }
        DesignTemplate template = templateRepository.findByTemplateIdAndCompany_ComId(templateId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版不存在")));
        accessContext.requireMaintain(me, template);
        if (template.getStatus() == DesignTemplate.Status.Disabled) {
            return template;
        }
        DesignTemplateVersion current = template.getCurrentVersion();
        if (current != null && current.getStatus() == DesignTemplate.Status.Draft) {
            current.setStatus(DesignTemplate.Status.Disabled);
            versionRepository.save(current);
        }
        template.setStatus(DesignTemplate.Status.Disabled);
        return templateRepository.save(template);
    }
}
