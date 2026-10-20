package com.th4.edge.decoboon.catalog.service.importtemplate.build.support;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogImportContext;
import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.common.file.model.FileGrant;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;

/** 报价项目库模板下载实现：返回仓库中同一份 V1.0 模板的签名下载地址。 */
@Note("报价项目库导入模板下载")
@Properties(allow = @AllowProperties(model = FileGrant.class,
        properties = {"businessType", "path", "url", "expiresAt", "name", "contentType", "size"}))
@Service("catalog/import-template/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.catalog.service.importtemplate.build.Build {

    /** 模板文件名含版本号，便于小程序转发到电脑微信后识别。 */
    public static final String TEMPLATE_NAME = "报价项目库批量导入模板_v1.0.xlsx";
    private static final String TEMPLATE_RESOURCE = "catalog/" + TEMPLATE_NAME;
    /** 对象键保持 ASCII：中文对象键在网关签名/下载链路存在编码差异，下载名单独用 name 参数。 */
    private static final String TEMPLATE_KEY = "system/catalog-import/catalog-import-template-v1.0.xlsx";
    /** 仓库模板冻结 SHA-256（与 ANS-BIZ-20260926-001 一致）。 */
    public static final String TEMPLATE_SHA256 =
            "0791273625f65cf6f021089d58284d17aefaab654ddadea50a67766e0f3638a9";

    @User
    private Employee employee;

    private final CatalogContext catalogContext;
    private final CatalogImportContext importContext;
    private final ObjectStorage storage;
    private final FileSignatureContext signatureContext;

    public Build(CatalogContext catalogContext,
                 CatalogImportContext importContext,
                 ObjectStorage storage,
                 FileSignatureContext signatureContext) {
        this.catalogContext = catalogContext;
        this.importContext = importContext;
        this.storage = storage;
        this.signatureContext = signatureContext;
    }

    @Override
    public FileGrant call(FileGrant request) {
        catalogContext.requireEmployee(employee);
        byte[] bytes = templateBytes();
        String fingerprint = importContext.fingerprint(bytes);
        if (!TEMPLATE_SHA256.equalsIgnoreCase(fingerprint)) {
            throw new DefinedException(new DefaultErrorCode("500", "模板文件校验失败，请联系管理员"));
        }
        if (!storage.exists(TEMPLATE_KEY)) {
            storage.put(TEMPLATE_KEY, new ByteArrayInputStream(bytes), bytes.length,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        FileGrant grant = new FileGrant();
        grant.setBusinessType(FileScope.Catalog);
        grant.setPath(TEMPLATE_KEY);
        grant.setName(TEMPLATE_NAME);
        grant.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        grant.setSize((long) bytes.length);
        // 小程序 downloadFile 必须走业务域名网关；预签名 OSS 地址会触发 url not in domain list。
        grant.setUrl(signatureContext.signGateway(TEMPLATE_KEY, TEMPLATE_NAME));
        grant.setExpiresAt(signatureContext.expiresAt());
        return grant;
    }

    private byte[] templateBytes() {
        try (InputStream input = new ClassPathResource(TEMPLATE_RESOURCE).getInputStream()) {
            return input.readAllBytes();
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "模板文件缺失：" + TEMPLATE_NAME));
        }
    }
}
