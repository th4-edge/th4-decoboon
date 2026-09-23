package com.th4.edge.decoboon.document.service.template.page.support;

import com.th4.edge.decoboon.document.context.DocumentTemplateContext;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("单据模板列表")
@Properties(allow = @AllowProperties(model = DocumentTemplate.class,
        properties = {"templateId", "company", "type", "scope", "name", "version", "baselineVersion",
                "fieldConfig", "styleConfig", "status", "createTime", "updateTime"}))
@Service("document/template/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.document.service.template.page.Page {
    @User private Employee employee;
    private final DocumentTemplateContext templateContext;

    public Page(DocumentTemplateContext templateContext) {
        this.templateContext = templateContext;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<DocumentTemplate> call(Employee request) {
        return templateContext.page(employee);
    }
}
