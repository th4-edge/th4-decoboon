package com.th4.edge.decoboon.document.service.template.save.support;

import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.context.DocumentTemplateContext;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("单据模板继承与保存")
@Properties(allow = {
        @AllowProperties(model = DocumentTemplate.class,
        properties = {"templateId", "company", "type", "scope", "name", "version", "baselineVersion",
                "fieldConfig", "styleConfig", "layoutConfig", "designerConfig", "status", "presetCode", "presetNote",
                "publishedVersion", "publishedBy", "publishedAt", "createTime", "updateTime"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Company.class, properties = {"comId", "title", "name", "logo", "color", "addr"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Employee.class, properties = {"empId", "name", "role", "status"})
})
@Service("document/template/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.document.service.template.save.Save {
    @AutoInject @NotNull private DocumentType type;
    @AutoInject private String presetCode;
    @AutoInject private String name;
    @AutoInject @NotNull private String fieldConfig;
    @AutoInject @NotNull private String styleConfig;
    @AutoInject private String layoutConfig;
    @AutoInject private String designerConfig;
    @User private Employee employee;
    private final DocumentTemplateContext templateContext;

    public Save(DocumentTemplateContext templateContext) {
        this.templateContext = templateContext;
    }

    @Override
    @Transactional
    public DocumentTemplate call(Employee request) {
        return templateContext.save(employee, type, presetCode, name, fieldConfig, styleConfig, layoutConfig, designerConfig);
    }
}
