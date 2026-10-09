package com.th4.edge.decoboon.content.service.authorization.revoke.support;

import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.decoboon.content.configure.ContentAuthorizationPurpose;
import com.th4.edge.decoboon.content.configure.ContentAuthorizationStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.decoboon.content.model.ContentAuthorization;
import com.th4.edge.decoboon.content.repository.ContentAssetRepository;
import com.th4.edge.decoboon.content.repository.ContentAuthorizationRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 素材授权撤回实现。 */
@Note("素材授权撤回")
@Properties(allow = @AllowProperties(model = ContentAuthorization.class,
        properties = {"authId", "status", "revokedAt", "revokeReason", "remark", "updateTime"}))
@Service("content/authorization/revoke")
@Scope(ScopeTypes.PROTOTYPE)
public class Revoke implements com.th4.edge.decoboon.content.service.authorization.revoke.Revoke {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long authId;
    @AutoInject
    @Length(max = 500)
    private String reason;

    private final ContentAuthorizationRepository authorizationRepository;
    private final ContentAssetRepository assetRepository;
    private final ContentAccessContext accessContext;

    public Revoke(ContentAuthorizationRepository authorizationRepository,
                  ContentAssetRepository assetRepository,
                  ContentAccessContext accessContext) {
        this.authorizationRepository = authorizationRepository;
        this.assetRepository = assetRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public ContentAuthorization call(ContentAuthorization request) {
        Employee me = accessContext.requireManager(employee);
        ContentAuthorization authorization = authorizationRepository
                .findByAuthIdAndCompany_ComId(authId, me.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "授权记录不存在或无权访问")));
        authorization.setStatus(ContentAuthorizationStatus.Revoked);
        authorization.setRevokedBy(me);
        authorization.setRevokedAt(LocalDateTime.now());
        authorization.setRevokeReason(reason == null || reason.isBlank() ? "授权撤回" : reason.trim());
        ContentAuthorization saved = authorizationRepository.save(authorization);

        ContentAsset asset = authorization.getAsset();
        if (authorization.getPurpose() == ContentAuthorizationPurpose.ZnDqMarketing && asset.getStatus() == ContentAssetStatus.MarketingReady) {
            asset.setStatus(ContentAssetStatus.PublicReady);
            assetRepository.save(asset);
        } else if (authorization.getPurpose() == ContentAuthorizationPurpose.Public
                && (asset.getStatus() == ContentAssetStatus.PublicReady || asset.getStatus() == ContentAssetStatus.MarketingReady)) {
            asset.setStatus(ContentAssetStatus.InternalReady);
            assetRepository.save(asset);
        }
        return saved;
    }
}
