package com.th4.edge.decoboon.document.service.template.disable.support;

import com.th4.edge.decoboon.document.context.DocumentTemplateContext;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.decoboon.document.model.DocumentTemplateVersion;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** document/template/disable 实现。 */
@Note("停用企业单据模板")
@Properties(allow = {
        @AllowProperties(model = DocumentTemplate.class,
                properties = {"templateId", "type", "scope", "name", "version", "baselineVersion",
                        "fieldConfig", "styleConfig", "layoutConfig", "designerConfig", "status", "presetCode", "presetNote",
                        "publishedVersion", "publishedBy", "publishedAt"}),
        @AllowProperties(model = DocumentTemplateVersion.class,
                properties = {"versionId", "version", "name", "presetCode", "baselineVersion",
                        "fieldConfig", "styleConfig", "layoutConfig", "designerConfig", "status", "publishedBy", "publishedAt", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("document/template/disable")
@Scope(ScopeTypes.PROTOTYPE)
public class Disable implements com.th4.edge.decoboon.document.service.template.disable.Disable {

    @User private Employee employee;
    @AutoInject private Long templateId;
    private final DocumentTemplateContext context;

    public Disable(DocumentTemplateContext context) {
        this.context = context;
    }

    @Override
    @Transactional
    public DocumentTemplate call(DocumentTemplate request) {
        return context.disable(employee, templateId);
    }
}
