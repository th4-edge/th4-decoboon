package com.th4.edge.decoboon.document.service.quote.build.support;

import com.th4.edge.decoboon.document.configure.DocumentAudience;
import com.th4.edge.decoboon.document.context.DocumentArchiveContext;
import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("报价PDF生成归档")
@Properties(allow = @AllowProperties(model = DocumentRecord.class,
        properties = {"documentId", "type", "audience", "businessNo", "templateVersion", "path",
                "sha256", "fileName", "contentType", "size", "generatedAt", "status", "url", "urlExpiresAt"}))
@Service("document/quote/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.document.service.quote.build.Build {
    @AutoInject @NotNull private Long quoteId;
    @AutoInject private DocumentAudience audience = DocumentAudience.Customer;
    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;
    private final DocumentArchiveContext archiveContext;

    public Build(DocumentArchiveContext archiveContext) {
        this.archiveContext = archiveContext;
    }

    @Override
    @Transactional
    public DocumentRecord call(Quote request) {
        return archiveContext.renderQuote(principal, quoteId,
                audience == null ? DocumentAudience.Customer : audience);
    }
}
