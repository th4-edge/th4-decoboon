package com.th4.edge.decoboon.document.service.contract.build.support;

import com.th4.edge.decoboon.document.configure.DocumentAudience;
import com.th4.edge.decoboon.document.context.DocumentArchiveContext;
import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 合同打印稿生成归档实现。 */
@Note("合同打印稿生成归档")
@Properties(allow = @AllowProperties(model = DocumentRecord.class,
        properties = {"documentId", "type", "audience", "businessNo", "templateVersion", "path",
                "sha256", "fileName", "contentType", "size", "generatedAt", "status", "url", "urlExpiresAt"}))
@Service("document/contract/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.document.service.contract.build.Build {

    @AutoInject
    @NotNull
    private Long contractId;

    @AutoInject
    private DocumentAudience audience = DocumentAudience.Customer;

    @User
    private com.th4.edge.framework.provider.security.User<?> principal;

    private final DocumentArchiveContext archiveContext;

    public Build(DocumentArchiveContext archiveContext) {
        this.archiveContext = archiveContext;
    }

    @Override
    @Transactional
    public DocumentRecord call(ProjectContract request) {
        return archiveContext.renderContract(principal, contractId,
                audience == null ? DocumentAudience.Customer : audience);
    }
}
