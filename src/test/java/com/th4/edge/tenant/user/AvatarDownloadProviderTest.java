package com.th4.edge.tenant.user;

import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.common.file.service.grants.download.support.Download;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.decoboon.common.file.model.FileGrant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 公开头像只走受约束的 avatar 路径，其他文件仍必须携带短期签名。 */
class AvatarDownloadProviderTest {

    private static final String AVATAR_KEY =
            "avatar/1789903520997907818/1789903521009050562-1829e9278ffa44fc86fb24244fccb39e.jpg";

    @Test
    void shouldAllowOnlyGeneratedAvatarPathWithoutSignature() {
        FileSignatureContext context = mock(FileSignatureContext.class);
        File file = new File("avatar.jpg");
        when(context.materialize(AVATAR_KEY, null)).thenReturn(file);
        Download provider = provider(context, AVATAR_KEY);

        assertThat(provider.call(new FileGrant())).isSameAs(file);
        verify(context).materialize(AVATAR_KEY, null);
    }

    @Test
    void shouldRejectAvatarLikePathThatDoesNotMatchGeneratedKey() {
        FileSignatureContext context = mock(FileSignatureContext.class);
        Download provider = provider(context, "avatar/12/34-short.jpg");

        assertThatThrownBy(() -> provider.call(new FileGrant()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("文件不存在或无权访问");
    }

    @Test
    void shouldKeepSignatureRequirementForOtherPrivateFiles() {
        FileSignatureContext context = mock(FileSignatureContext.class);
        File file = new File("private.pdf");
        when(context.verify("companies/1/private.pdf", 123L, "signed")).thenReturn(true);
        when(context.materialize("companies/1/private.pdf", "private.pdf")).thenReturn(file);
        Download provider = provider(context, "companies/1/private.pdf");
        ReflectionTestUtils.setField(provider, "expires", 123L);
        ReflectionTestUtils.setField(provider, "signature", "signed");
        ReflectionTestUtils.setField(provider, "name", "private.pdf");

        assertThat(provider.call(new FileGrant())).isSameAs(file);
    }

    private Download provider(FileSignatureContext context, String path) {
        Download provider = new Download(context);
        ReflectionTestUtils.setField(provider, "path", path);
        return provider;
    }
}
