package com.th4.edge.decoboon.document.service.profile.info.support;

import com.th4.edge.decoboon.document.context.DocumentProfileContext;
import com.th4.edge.decoboon.document.model.DocumentProfile;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("企业单据资料")
@Properties(allow = {
        @AllowProperties(model = DocumentProfile.class,
        properties = {"profileId", "company", "legalName", "creditCode", "legalRepresentative", "address",
                "serviceHotline", "bankName", "bankAccount", "logo", "createTime", "updateTime"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Company.class, properties = {"comId", "title", "name", "logo", "color", "addr"})
})
@Service("document/profile/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.document.service.profile.info.Info {
    @User private Employee employee;
    private final DocumentProfileContext profileContext;

    public Info(DocumentProfileContext profileContext) {
        this.profileContext = profileContext;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentProfile call(Employee request) {
        return profileContext.profile(employee);
    }
}
