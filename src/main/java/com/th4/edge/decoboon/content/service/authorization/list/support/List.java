package com.th4.edge.decoboon.content.service.authorization.list.support;

import com.th4.edge.decoboon.content.configure.ContentAuthorizationStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.decoboon.content.model.ContentAuthorization;
import com.th4.edge.decoboon.content.repository.ContentAuthorizationRepository;
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

/** 素材授权列表实现。 */
@Note("素材授权列表")
@Properties(allow = {
        @AllowProperties(model = ContentAuthorization.class,
                properties = {"authId", "subjectType", "subjectRefId", "purpose", "channels", "region",
                        "validFrom", "validTo", "allowModify", "allowAi", "allowAds", "allowSublicense",
                        "evidencePath", "status", "revokedAt", "revokeReason", "remark", "createTime", "updateTime"}),
        @AllowProperties(model = ContentAsset.class, properties = {"assetId", "title", "status", "desensitized"})
})
@Service("content/authorization/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.content.service.authorization.list.List {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long assetId;

    private final ContentAuthorizationRepository authorizationRepository;
    private final ContentAccessContext accessContext;

    public List(ContentAuthorizationRepository authorizationRepository,
                ContentAccessContext accessContext) {
        this.authorizationRepository = authorizationRepository;
        this.accessContext = accessContext;
    }

    @Override
    public java.util.List<ContentAuthorization> call(ContentAsset request) {
        Employee me = accessContext.requireEmployee(employee);
        accessContext.requireAsset(assetId, me);
        return authorizationRepository.findByAsset_AssetIdOrderByAuthIdDesc(assetId).stream()
                .peek(item -> {
                    if (item.getStatus() == ContentAuthorizationStatus.Authorized
                            && item.getValidTo() != null
                            && item.getValidTo().isBefore(java.time.LocalDate.now())) {
                        item.setStatus(ContentAuthorizationStatus.Expired);
                    }
                })
                .toList();
    }
}
