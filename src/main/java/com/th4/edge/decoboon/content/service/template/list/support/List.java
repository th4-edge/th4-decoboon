package com.th4.edge.decoboon.content.service.template.list.support;

import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentTemplate;
import com.th4.edge.decoboon.content.repository.ContentTemplateRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 内容模板列表实现。 */
@Note("内容模板列表")
@Properties(allow = {
        @AllowProperties(model = ContentTemplate.class,
                properties = {"templateId", "name", "format", "durationSec", "clipMin", "clipMax",
                        "scriptSchema", "coverSchema", "allowedAssetTypes", "status", "version",
                        "createTime", "updateTime", "company", "store"}),
        @AllowProperties(model = Company.class, properties = {"comId", "name", "title"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("content/template/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.content.service.template.list.List {
    @User
    private Employee employee;
    private final ContentTemplateRepository repository;
    private final ContentAccessContext accessContext;

    public List(ContentTemplateRepository repository, ContentAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    public java.util.List<ContentTemplate> call(ContentTemplate request) {
        Employee me = accessContext.requireEmployee(employee);
        return repository.findByCompany_ComIdOrCompanyIsNullOrderByTemplateIdAsc(me.getCompany().getComId());
    }
}
