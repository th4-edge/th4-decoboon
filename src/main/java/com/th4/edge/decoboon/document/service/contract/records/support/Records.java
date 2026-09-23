package com.th4.edge.decoboon.document.service.contract.records.support;

import com.th4.edge.decoboon.document.context.DocumentArchiveContext;
import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 合同打印稿归档记录实现。 */
@Note("合同打印稿归档记录")
@Properties(allow = @AllowProperties(model = DocumentRecord.class,
        properties = {"documentId", "type", "audience", "businessNo", "templateVersion", "path",
                "sha256", "fileName", "contentType", "size", "generatedAt", "status", "url", "urlExpiresAt"}))
@Service("document/contract/records")
@Scope(ScopeTypes.PROTOTYPE)
public class Records implements com.th4.edge.decoboon.document.service.contract.records.Records {

    @AutoInject
    @NotNull
    private Long contractId;

    @com.th4.edge.framework.provider.security.annotation.User
    private com.th4.edge.framework.provider.security.User<?> principal;

    private final DocumentArchiveContext archiveContext;

    public Records(DocumentArchiveContext archiveContext) {
        this.archiveContext = archiveContext;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentRecord> call(ProjectContract request) {
        return archiveContext.contractRecords(principal, contractId);
    }
}
