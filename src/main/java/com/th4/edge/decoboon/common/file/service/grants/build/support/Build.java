package com.th4.edge.decoboon.common.file.service.grants.build.support;

import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.decoboon.common.file.context.FileAuthorizationContext;
import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.common.file.context.FileStorageContext;
import com.th4.edge.decoboon.common.file.model.FileGrant;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.io.annotation.AutoFile;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.framework.io.StreamFile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 私有文件授权构建实现。 */
@Note("私有文件授权")
@Properties(allow = @AllowProperties(model = FileGrant.class,
        properties = {"businessType", "businessId", "projectId", "path", "url", "expiresAt", "name", "contentType", "size"}))
@Service("file/private/grant")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.common.file.service.grants.build.Build {

    @com.th4.edge.framework.provider.security.annotation.User
    private com.th4.edge.framework.provider.security.User<?> principal;

    @AutoInject
    @NotNull
    private FileScope businessType;

    @AutoInject
    private Long businessId;

    @AutoInject
    private List<String> paths;

    @AutoInject
    private Long projectId;

    @AutoFile
    private StreamFile file;

    private final FileAuthorizationContext authorizationContext;
    private final FileSignatureContext signatureContext;
    private final FileStorageContext storageContext;

    public Build(FileAuthorizationContext authorizationContext,
                 FileSignatureContext signatureContext,
                 FileStorageContext storageContext) {
        this.authorizationContext = authorizationContext;
        this.signatureContext = signatureContext;
        this.storageContext = storageContext;
    }

    @Override
    public List<FileGrant> call(FileGrant request) {
        if (file != null && !file.isEmpty()) {
            return List.of(storageContext.upload(principal, businessType, projectId, file));
        }
        if (businessId == null) {
            throw new com.th4.edge.framework.exception.DefinedException(
                    new com.th4.edge.framework.exception.DefaultErrorCode("400", "业务对象不能为空"));
        }
        if (paths == null || paths.isEmpty()) {
            throw new com.th4.edge.framework.exception.DefinedException(
                    new com.th4.edge.framework.exception.DefaultErrorCode("400", "文件路径不能为空"));
        }
        Set<String> requested = new LinkedHashSet<>(paths);
        FileAuthorizationContext.Authorization authorization = authorizationContext.authorize(
                principal, businessType, businessId, requested);
        List<FileGrant> grants = new ArrayList<>();
        for (String path : authorization.paths()) {
            FileGrant grant = new FileGrant();
            grant.setBusinessType(businessType);
            grant.setBusinessId(businessId);
            grant.setProjectId(authorization.projectId());
            grant.setPath(path);
            grant.setUrl(signatureContext.sign(path));
            grant.setExpiresAt(signatureContext.expiresAt());
            grants.add(grant);
        }
        return grants;
    }
}
