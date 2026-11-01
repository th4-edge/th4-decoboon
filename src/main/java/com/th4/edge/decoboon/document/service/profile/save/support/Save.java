package com.th4.edge.decoboon.document.service.profile.save.support;

import com.th4.edge.decoboon.document.context.DocumentProfileContext;
import com.th4.edge.decoboon.document.model.DocumentProfile;
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

@Note("企业单据资料保存")
@Properties(allow = {
        @AllowProperties(model = DocumentProfile.class,
        properties = {"profileId", "company", "legalName", "creditCode", "legalRepresentative", "address",
                "serviceHotline", "bankName", "bankAccount", "logo", "createTime", "updateTime"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Company.class, properties = {"comId", "title", "name", "logo", "color", "addr"})
})
@Service("document/profile/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.document.service.profile.save.Save {
    @AutoInject private String legalName;
    @AutoInject private String creditCode;
    @AutoInject private String legalRepresentative;
    @AutoInject private String address;
    @AutoInject private String serviceHotline;
    @AutoInject private String bankName;
    @AutoInject private String bankAccount;
    @AutoInject private String logo;
    @User private Employee employee;
    private final DocumentProfileContext profileContext;

    public Save(DocumentProfileContext profileContext) {
        this.profileContext = profileContext;
    }

    @Override
    @Transactional
    public DocumentProfile call(Employee request) {
        return profileContext.save(employee, legalName, creditCode, legalRepresentative,
                address, serviceHotline, bankName, bankAccount, logo);
    }
}
