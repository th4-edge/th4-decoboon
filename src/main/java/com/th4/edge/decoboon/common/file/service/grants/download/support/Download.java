package com.th4.edge.decoboon.common.file.service.grants.download.support;

import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.common.file.model.FileGrant;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 私有文件签名下载实现。 */
@Note("私有文件下载")
@Service("file/private/download")
@Scope(ScopeTypes.PROTOTYPE)
public class Download implements com.th4.edge.decoboon.common.file.service.grants.download.Download {

    @AutoInject
    @NotEmpty
    private String path;

    @AutoInject
    @NotNull
    private Long expires;

    @AutoInject
    @NotEmpty
    private String signature;

    /** 下载展示文件名，仅影响 Content-Disposition，不参与签名校验。 */
    @AutoInject
    private String name;

    private final FileSignatureContext signatureContext;

    public Download(FileSignatureContext signatureContext) {
        this.signatureContext = signatureContext;
    }

    @Override
    public java.io.File call(FileGrant request) {
        if (!signatureContext.verify(path, expires, signature)) {
            throw new DefinedException(new DefaultErrorCode("404", "文件不存在或无权访问"));
        }
        return signatureContext.materialize(path, name);
    }
}
