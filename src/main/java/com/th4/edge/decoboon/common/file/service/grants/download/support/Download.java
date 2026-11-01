package com.th4.edge.decoboon.common.file.service.grants.download.support;

import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.common.file.model.FileGrant;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/** 私有文件签名下载实现。 */
@Note("私有文件下载")
@Service("file/private/download")
@Scope(ScopeTypes.PROTOTYPE)
public class Download implements com.th4.edge.decoboon.common.file.service.grants.download.Download {

    /** 只放行服务端生成的员工头像对象键，防止绕过私有文件签名读取其他 OSS 对象。 */
    private static final Pattern PUBLIC_AVATAR_PATH =
            Pattern.compile("^avatar/\\d+/\\d+-[0-9a-fA-F]{32}\\.(jpg|jpeg|png|webp)$");
    /** 企业品牌 Logo 是邀请页、业主页可匿名读取的公开品牌资产，只放行服务端生成的固定对象键。 */
    private static final Pattern PUBLIC_COMPANY_BRAND_PATH =
            Pattern.compile("^company-brand/\\d+/logo-[0-9a-fA-F]{32}\\.(jpg|jpeg|png|webp)$");

    @AutoInject
    private String path;

    @AutoInject
    private Long expires;

    @AutoInject
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
        boolean publicAsset = path != null
                && (PUBLIC_AVATAR_PATH.matcher(path).matches() || PUBLIC_COMPANY_BRAND_PATH.matcher(path).matches());
        if (!publicAsset && !signatureContext.verify(path, expires, signature)) {
            throw new DefinedException(new DefaultErrorCode("404", "文件不存在或无权访问"));
        }
        return signatureContext.materialize(path, publicAsset ? null : name);
    }
}
