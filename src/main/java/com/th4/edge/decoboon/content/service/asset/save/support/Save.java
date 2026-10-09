package com.th4.edge.decoboon.content.service.asset.save.support;

import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.decoboon.content.configure.ContentMediaType;
import com.th4.edge.decoboon.content.configure.ContentSourceType;
import com.th4.edge.decoboon.content.configure.ContentTruthType;
import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.decoboon.content.repository.ContentAssetRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 内容素材保存实现。 */
@Note("内容素材保存")
@Properties(allow = {
        @AllowProperties(model = ContentAsset.class,
                properties = {"assetId", "title", "mediaType", "sourceType", "truthType", "filePath",
                        "originalName", "contentType", "fileSize", "tags", "desensitized", "status", "remark",
                        "dataVersion", "previewUrl", "createTime", "updateTime", "project", "customer", "uploader", "store"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "name", "no", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("content/asset/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.content.service.asset.save.Save {

    @User
    private Employee employee;
    @AutoInject
    private Long assetId;
    @AutoInject
    @NotNull
    private Long projectId;
    @AutoInject
    private Long customerId;
    @AutoInject
    @NotEmpty
    @Length(max = 255)
    private String title;
    @AutoInject
    private ContentMediaType mediaType;
    @AutoInject
    private ContentSourceType sourceType;
    @AutoInject
    private ContentTruthType truthType;
    @AutoInject
    @NotEmpty
    @Length(max = 512)
    private String filePath;
    @AutoInject
    @Length(max = 255)
    private String originalName;
    @AutoInject
    @Length(max = 128)
    private String contentType;
    @AutoInject
    private Long fileSize;
    @AutoInject
    @Length(max = 2000)
    private String tags;
    @AutoInject
    private Boolean desensitized;
    @AutoInject
    @Length(max = 1000)
    private String remark;

    private final ContentAssetRepository assetRepository;
    private final ContentAccessContext accessContext;
    private final ProjectAccessContext projectAccessContext;
    private final FileSignatureContext signatureContext;

    public Save(ContentAssetRepository assetRepository,
                ContentAccessContext accessContext,
                ProjectAccessContext projectAccessContext,
                FileSignatureContext signatureContext) {
        this.assetRepository = assetRepository;
        this.accessContext = accessContext;
        this.projectAccessContext = projectAccessContext;
        this.signatureContext = signatureContext;
    }

    @Override
    @Transactional
    public ContentAsset call(ContentAsset request) {
        Employee me = accessContext.requireEmployee(employee);
        accessContext.requireCanUpload(me, projectId);
        Project project = projectAccessContext.requireProject(projectId, me);

        ContentAsset asset;
        if (assetId == null) {
            asset = new ContentAsset();
            asset.setCompany(me.getCompany());
            asset.setStore(project.getStore() != null ? project.getStore() : me.getStore());
            asset.setProject(project);
            asset.setUploader(me);
            asset.setStatus(ContentAssetStatus.Draft);
        } else {
            asset = accessContext.requireAsset(assetId, me);
            if (!asset.getUploader().getEmpId().equals(me.getEmpId()) && !accessContext.isManager(me)) {
                throw new DefinedException(new DefaultErrorCode("403", "只能编辑本人上传的素材"));
            }
            asset.setDataVersion((asset.getDataVersion() == null ? 1 : asset.getDataVersion()) + 1);
        }
        asset.setProject(project);
        asset.setCustomer(project.getCustomer());
        asset.setTitle(title.trim());
        asset.setMediaType(mediaType == null ? ContentMediaType.Image : mediaType);
        asset.setSourceType(sourceType == null ? ContentSourceType.Camera : sourceType);
        asset.setTruthType(truthType == null ? ContentTruthType.Native : truthType);
        asset.setFilePath(filePath.trim());
        asset.setOriginalName(blankToNull(originalName));
        asset.setContentType(blankToNull(contentType));
        asset.setFileSize(fileSize);
        asset.setTags(blankToNull(tags));
        asset.setDesensitized(Boolean.TRUE.equals(desensitized));
        asset.setRemark(blankToNull(remark));
        ContentAsset saved = assetRepository.save(asset);
        saved.setPreviewUrl(signatureContext.sign(saved.getFilePath()));
        return saved;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
