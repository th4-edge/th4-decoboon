package com.th4.edge.decoboon.content.service.asset.status.support;

import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.decoboon.content.repository.ContentAssetRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 内容素材状态变更实现。 */
@Note("内容素材状态变更")
@Properties(allow = @AllowProperties(model = ContentAsset.class,
        properties = {"assetId", "status", "desensitized", "updateTime"}))
@Service("content/asset/status")
@Scope(ScopeTypes.PROTOTYPE)
public class Status implements com.th4.edge.decoboon.content.service.asset.status.Status {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long assetId;
    @AutoInject
    @NotNull
    private ContentAssetStatus status;

    private final ContentAssetRepository assetRepository;
    private final ContentAccessContext accessContext;

    public Status(ContentAssetRepository assetRepository, ContentAccessContext accessContext) {
        this.assetRepository = assetRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public ContentAsset call(ContentAsset request) {
        Employee me = accessContext.requireAssetReviewer(employee);
        ContentAsset asset = accessContext.requireAsset(assetId, me);
        if (status == ContentAssetStatus.PublicReady || status == ContentAssetStatus.MarketingReady) {
            if (!Boolean.TRUE.equals(asset.getDesensitized())) {
                throw new DefinedException(new DefaultErrorCode("409", "素材未完成脱敏，不能进入公开或宣传状态"));
            }
        }
        if (status == ContentAssetStatus.PublicReady) accessContext.requirePublicAuthorization(asset);
        if (status == ContentAssetStatus.MarketingReady) {
            accessContext.requirePublicAuthorization(asset);
            accessContext.requireMarketingAuthorization(asset);
        }
        asset.setStatus(status);
        return assetRepository.save(asset);
    }
}
