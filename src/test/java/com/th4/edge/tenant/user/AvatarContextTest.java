package com.th4.edge.tenant.user;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.io.StreamFile;
import com.th4.edge.tenant.user.context.AvatarContext;
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

/** 员工头像存储：私有 OSS + 稳定代理地址、local 公开目录、类型与大小校验。 */
class AvatarContextTest {

    @TempDir
    Path publicDir;

    @Test
    void shouldStoreAvatarInPrivateOssAndReturnStableProxyUrl() throws Exception {
        ObjectStorage storage = mock(ObjectStorage.class);
        when(storage.remote()).thenReturn(true);
        AvatarContext context = new AvatarContext(storage, publicDir.toString(), "https://api.example.com/");

        AvatarContext.StoredAvatar stored = context.save(12L, 34L, file("头像.webp", "image/webp", 2048L));

        assertThat(stored.path()).startsWith("avatar/12/34-").endsWith(".webp");
        assertThat(stored.url()).isEqualTo("https://api.example.com/file/private/download?path=" + java.net.URLEncoder.encode(stored.path(), java.nio.charset.StandardCharsets.UTF_8));
        verify(storage).put(eq(stored.path()), any(InputStream.class), eq(2048L), eq("image/webp"));
    }

    @Test
    void shouldStoreLocalProviderAvatarUnderPublicDirAndReturnAbsoluteUrl() throws Exception {
        ObjectStorage storage = mock(ObjectStorage.class);
        when(storage.remote()).thenReturn(false);
        AvatarContext context = new AvatarContext(storage, publicDir.toString(), "https://api.example.com/");

        AvatarContext.StoredAvatar stored = context.save(12L, 34L, file("头像.png", "image/png", 2048L));

        assertThat(stored.path()).startsWith("/public/avatar/12/34-").endsWith(".png");
        assertThat(stored.url()).isEqualTo("https://api.example.com" + stored.path());
        assertThat(Files.isRegularFile(publicDir.resolve(stored.path().substring("/public/".length())))).isTrue();
    }

    @Test
    void shouldFallBackToRelativePathWhenNoPublicBaseUrl() throws Exception {
        ObjectStorage storage = mock(ObjectStorage.class);
        when(storage.remote()).thenReturn(false);
        AvatarContext context = new AvatarContext(storage, publicDir.toString(), "");

        AvatarContext.StoredAvatar stored = context.save(12L, 34L, file("头像.jpg", "image/jpeg", 1024L));

        assertThat(stored.url()).isEqualTo(stored.path());
    }

    @Test
    void shouldRejectNonImageOversizeAndMissingOwner() throws Exception {
        ObjectStorage storage = mock(ObjectStorage.class);
        when(storage.remote()).thenReturn(false);
        AvatarContext context = new AvatarContext(storage, publicDir.toString(), "");

        assertThatThrownBy(() -> context.save(12L, 34L, file("说明.pdf", "application/pdf", 1024L)))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("仅支持 JPG / PNG / WebP");
        assertThatThrownBy(() -> context.save(12L, 34L, file("头像.png", "image/png", 3 * 1024 * 1024L)))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不能超过 2MB");
        assertThatThrownBy(() -> context.save(null, 34L, file("头像.png", "image/png", 1024L)))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("头像归属参数无效");
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
