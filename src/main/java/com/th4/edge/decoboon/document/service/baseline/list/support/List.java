package com.th4.edge.decoboon.document.service.baseline.list.support;

import com.th4.edge.decoboon.document.context.DocumentTemplateBaseline;
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

/** 平台常用单据模板库实现：返回收款单与付款单的内置预设，企业可套用后改名保存。 */
@Note("平台常用单据模板库")
@Properties(allow = @AllowProperties(model = DocumentTemplate.class,
        properties = {"type", "scope", "name", "version", "baselineVersion",
                "fieldConfig", "styleConfig", "status", "presetCode", "presetNote"}))
@Service("document/baseline/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.document.service.baseline.list.List {

    @User
    private Employee employee;

    private final DocumentTemplateBaseline baseline;

    public List(DocumentTemplateBaseline baseline) {
        this.baseline = baseline;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<DocumentTemplate> call(DocumentTemplate request) {
        return baseline.presets();
    }
}
