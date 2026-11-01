package com.th4.edge.tenant.user;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.io.StreamFile;
import com.th4.edge.tenant.user.context.CompanyBrandContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 企业 Logo 存储：私有 OSS 稳定地址、本地公开目录、类型与大小校验。 */
class CompanyBrandContextTest {

    @TempDir
    Path publicDir;

    @Test
    void shouldStoreLogoInPrivateOssAndReturnStableProxyUrl() throws Exception {
        ObjectStorage storage = mock(ObjectStorage.class);
        when(storage.remote()).thenReturn(true);
        CompanyBrandContext context = new CompanyBrandContext(storage, publicDir.toString(), "https://api.example.com/");

        CompanyBrandContext.StoredLogo stored = context.save(12L, file("企业Logo.png", "image/png", 2048L));

        assertThat(stored.path()).startsWith("company-brand/12/logo-").endsWith(".png");
        assertThat(stored.url()).isEqualTo("https://api.example.com/file/private/download?path="
                + java.net.URLEncoder.encode(stored.path(), java.nio.charset.StandardCharsets.UTF_8));
        verify(storage).put(eq(stored.path()), any(InputStream.class), eq(2048L), eq("image/png"));
    }

    @Test
    void shouldStoreLocalLogoUnderPublicDirAndReturnAbsoluteUrl() throws Exception {
        ObjectStorage storage = mock(ObjectStorage.class);
        when(storage.remote()).thenReturn(false);
        CompanyBrandContext context = new CompanyBrandContext(storage, publicDir.toString(), "https://api.example.com/");

        CompanyBrandContext.StoredLogo stored = context.save(12L, file("企业Logo.webp", "image/webp", 1024L));

        assertThat(stored.path()).startsWith("/public/company-brand/12/logo-").endsWith(".webp");
        assertThat(stored.url()).isEqualTo("https://api.example.com" + stored.path());
        assertThat(Files.isRegularFile(publicDir.resolve(stored.path().substring("/public/".length())))).isTrue();
    }

    @Test
    void shouldRejectNonImageOversizeAndMissingCompany() throws Exception {
        ObjectStorage storage = mock(ObjectStorage.class);
        when(storage.remote()).thenReturn(false);
        CompanyBrandContext context = new CompanyBrandContext(storage, publicDir.toString(), "");

        assertThatThrownBy(() -> context.save(12L, file("说明.pdf", "application/pdf", 1024L)))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("仅支持 JPG / PNG / WebP");
        assertThatThrownBy(() -> context.save(12L, file("企业Logo.png", "image/png", 3 * 1024 * 1024L)))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不能超过 2MB");
        assertThatThrownBy(() -> context.save(null, file("企业Logo.png", "image/png", 1024L)))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("企业Logo归属参数无效");
    }

    private StreamFile file(String name, String contentType, long size) throws Exception {
        StreamFile file = mock(StreamFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(size);
        when(file.getOriginalFilename()).thenReturn(name);
        when(file.getContentType()).thenReturn(contentType);
        when(file.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(new byte[] {1, 2, 3}));
        return file;
    }
}
