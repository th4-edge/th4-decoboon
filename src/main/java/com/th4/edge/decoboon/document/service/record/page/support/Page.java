package com.th4.edge.decoboon.document.service.record.page.support;

import com.th4.edge.decoboon.document.context.DocumentArchiveContext;
import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.decoboon.quote.model.Quote;
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

@Note("报价PDF归档记录")
@Properties(allow = @AllowProperties(model = DocumentRecord.class,
        properties = {"documentId", "type", "audience", "businessNo", "templateVersion", "path",
                "sha256", "fileName", "contentType", "size", "generatedAt", "status", "url", "urlExpiresAt"}))
@Service("document/record/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.document.service.record.page.Page {
    @AutoInject @NotNull private Long quoteId;
    @User private com.th4.edge.framework.provider.security.User<?> principal;
    private final DocumentArchiveContext archiveContext;

    public Page(DocumentArchiveContext archiveContext) {
        this.archiveContext = archiveContext;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<DocumentRecord> call(Quote request) {
        return archiveContext.records(principal, quoteId);
    }
}
