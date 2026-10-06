package com.th4.edge.decoboon.document.service.templatever.page.support;

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

import java.util.List;

/** document/template-version/page 实现。 */
@Note("企业单据模板版本分页")
@Properties(allow = {
        @AllowProperties(model = DocumentTemplateVersion.class,
                properties = {"versionId", "version", "name", "presetCode", "baselineVersion",
                        "fieldConfig", "styleConfig", "layoutConfig", "designerConfig", "status", "publishedBy", "publishedAt", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("document/template-version/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.document.service.templatever.page.Page {

    @User private Employee employee;
    @AutoInject private Long templateId;
    private final DocumentTemplateContext context;

    public Page(DocumentTemplateContext context) {
        this.context = context;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentTemplateVersion> call(DocumentTemplate request) {
        return context.versions(employee, templateId);
    }
}
