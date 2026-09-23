package com.th4.edge.decoboon.document.service.reprint.save.support;

import com.th4.edge.decoboon.document.context.DocumentArchiveContext;
import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("打印稿重打归档")
@Properties(allow = @AllowProperties(model = DocumentRecord.class,
        properties = {"documentId", "type", "audience", "businessNo", "templateVersion", "path",
                "sha256", "fileName", "contentType", "size", "generatedAt", "status", "url", "urlExpiresAt"}))
@Service("document/reprint/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.document.service.reprint.save.Save {
    @AutoInject @NotNull private Long documentId;
    @AutoInject @NotEmpty private String reason;
    @User private com.th4.edge.framework.provider.security.User<?> principal;
    private final DocumentArchiveContext archiveContext;

    public Save(DocumentArchiveContext archiveContext) {
        this.archiveContext = archiveContext;
    }

    @Override
    @Transactional
    public DocumentRecord call(DocumentRecord request) {
        return archiveContext.reprint(principal, documentId, reason);
    }
}
