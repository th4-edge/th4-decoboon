package com.th4.edge.decoboon.content.service.asset.info.support;

import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 内容素材详情实现。 */
@Note("内容素材详情")
@Properties(allow = {
        @AllowProperties(model = ContentAsset.class,
                properties = {"assetId", "title", "mediaType", "sourceType", "truthType", "filePath",
                        "originalName", "contentType", "fileSize", "tags", "desensitized", "status",
                        "remark", "dataVersion", "previewUrl", "createTime", "updateTime", "project", "customer", "uploader", "store"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "name", "no", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("content/asset/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.content.service.asset.info.Info {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long assetId;

    private final ContentAccessContext accessContext;
    private final FileSignatureContext signatureContext;

    public Info(ContentAccessContext accessContext, FileSignatureContext signatureContext) {
        this.accessContext = accessContext;
        this.signatureContext = signatureContext;
    }

    @Override
    public ContentAsset call(ContentAsset request) {
        ContentAsset asset = accessContext.requireAsset(assetId, employee);
        asset.setPreviewUrl(signatureContext.sign(asset.getFilePath()));
        return asset;
    }
}
