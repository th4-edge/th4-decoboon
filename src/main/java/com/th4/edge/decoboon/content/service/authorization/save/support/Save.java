package com.th4.edge.decoboon.content.service.authorization.save.support;

import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.decoboon.content.configure.ContentAuthorizationPurpose;
import com.th4.edge.decoboon.content.configure.ContentAuthorizationStatus;
import com.th4.edge.decoboon.content.configure.ContentAuthorizationSubject;
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

import java.time.LocalDate;

/** 素材授权保存实现。 */
@Note("素材授权保存")
@Properties(allow = {
        @AllowProperties(model = ContentAuthorization.class,
                properties = {"authId", "asset", "subjectType", "subjectRefId", "purpose", "channels", "region",
                        "validFrom", "validTo", "allowModify", "allowAi", "allowAds", "allowSublicense",
                        "evidencePath", "status", "remark", "createTime", "updateTime"}),
        @AllowProperties(model = ContentAsset.class, properties = {"assetId", "title", "status", "desensitized"})
})
@Service("content/authorization/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.content.service.authorization.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long assetId;
    @AutoInject
    private ContentAuthorizationSubject subjectType;
    @AutoInject
    private Long subjectRefId;
    @AutoInject
    @NotNull
    private ContentAuthorizationPurpose purpose;
    @AutoInject
    @Length(max = 500)
    private String channels;
    @AutoInject
    @Length(max = 255)
    private String region;
    @AutoInject
    @NotNull
    private LocalDate validFrom;
    @AutoInject
    @NotNull
    private LocalDate validTo;
    @AutoInject
    private Boolean allowModify;
    @AutoInject
    private Boolean allowAi;
    @AutoInject
    private Boolean allowAds;
    @AutoInject
    private Boolean allowSublicense;
    @AutoInject
    @Length(max = 512)
    private String evidencePath;
    @AutoInject
    @Length(max = 1000)
    private String remark;

    private final ContentAuthorizationRepository authorizationRepository;
    private final ContentAssetRepository assetRepository;
    private final ContentAccessContext accessContext;

    public Save(ContentAuthorizationRepository authorizationRepository,
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
        ContentAsset asset = accessContext.requireAsset(assetId, me);
        if (validTo.isBefore(validFrom)) {
            throw new DefinedException(new DefaultErrorCode("400", "授权失效日期不能早于生效日期"));
        }
        ContentAuthorization authorization = authorizationRepository
                .findByAsset_AssetIdOrderByAuthIdDesc(asset.getAssetId()).stream()
                .filter(item -> item.getPurpose() == purpose
                        && item.getSubjectType() == (subjectType == null ? ContentAuthorizationSubject.Company : subjectType)
                        && item.getSubjectRefId() != null
                        && item.getSubjectRefId().equals(subjectRefId == null ? me.getCompany().getComId() : subjectRefId)
                        && !item.getValidTo().isBefore(LocalDate.now())
                        && (item.getStatus() == ContentAuthorizationStatus.Authorized
                            || item.getStatus() == ContentAuthorizationStatus.Expiring))
                .findFirst()
                .orElseGet(ContentAuthorization::new);
        authorization.setCompany(me.getCompany());
        authorization.setAsset(asset);
        authorization.setSubjectType(subjectType == null ? ContentAuthorizationSubject.Company : subjectType);
        authorization.setSubjectRefId(subjectRefId == null ? me.getCompany().getComId() : subjectRefId);
        authorization.setPurpose(purpose);
        authorization.setChannels(blankToNull(channels));
        authorization.setRegion(blankToNull(region));
        authorization.setValidFrom(validFrom);
        authorization.setValidTo(validTo);
        authorization.setAllowModify(Boolean.TRUE.equals(allowModify));
        authorization.setAllowAi(Boolean.TRUE.equals(allowAi));
        authorization.setAllowAds(Boolean.TRUE.equals(allowAds));
        authorization.setAllowSublicense(Boolean.TRUE.equals(allowSublicense));
        authorization.setEvidencePath(blankToNull(evidencePath));
        authorization.setStatus(ContentAuthorizationStatus.Authorized);
        authorization.setRemark(blankToNull(remark));
        ContentAuthorization saved = authorizationRepository.save(authorization);

        if (purpose == ContentAuthorizationPurpose.Internal && asset.getStatus() == ContentAssetStatus.Draft) {
            asset.setStatus(ContentAssetStatus.InternalReady);
            assetRepository.save(asset);
        }
        return saved;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
