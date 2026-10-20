package com.th4.edge.decoboon.catalog;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogImportContext;
import com.th4.edge.decoboon.catalog.service.importtemplate.build.support.Build;
import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.common.file.model.FileGrant;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 报价项目库模板下载必须返回微信小程序可访问的业务域名网关地址。 */
class CatalogImportTemplateBuildTest {

    private static final String OBJECT_KEY = "system/catalog-import/catalog-import-template-v1.0.xlsx";

    @Test
    void returnsGatewayUrlInsteadOfOssPresignedUrlForMiniProgramDownload() {
        CatalogContext catalogContext = mock(CatalogContext.class);
        CatalogImportContext importContext = mock(CatalogImportContext.class);
        ObjectStorage storage = mock(ObjectStorage.class);
        FileSignatureContext signatureContext = mock(FileSignatureContext.class);
        Build provider = new Build(catalogContext, importContext, storage, signatureContext);
        ReflectionTestUtils.setField(provider, "employee", new Employee());

        when(importContext.fingerprint(any(byte[].class))).thenReturn(Build.TEMPLATE_SHA256);
        when(storage.exists(OBJECT_KEY)).thenReturn(true);
        when(signatureContext.signGateway(OBJECT_KEY, Build.TEMPLATE_NAME))
                .thenReturn("/file/private/download?path=" + OBJECT_KEY + "&expires=1&signature=test");

        FileGrant result = provider.call(new FileGrant());

        assertThat(result.getUrl()).startsWith("/file/private/download");
        verify(signatureContext).signGateway(OBJECT_KEY, Build.TEMPLATE_NAME);
        verify(signatureContext, never()).sign(anyString());
    }
}
