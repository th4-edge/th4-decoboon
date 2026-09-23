package com.th4.edge.decoboon.common.file;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 业务域名网关签名与对象存储回源的独立单元验证。 */
@ExtendWith(MockitoExtension.class)
class FileSignatureContextTest {

    private static final String OBJECT_KEY = "companies/100/files/document/2026/09/quote-1.pdf";
    private static final String OSS_URL =
            "https://decoboon.oss-cn-hangzhou.aliyuncs.com/" + OBJECT_KEY + "?Expires=1&Signature=x";

    @Mock private ObjectStorage storage;
    @TempDir Path uploadDir;

    @Test
    void documentGatewayUrlStaysOnBusinessDomainInsteadOfOssUrl() {
        FileSignatureContext context = context();

        String url = context.signGateway(OBJECT_KEY);

        assertThat(url).startsWith("/file/private/download?path=");
        assertThat(url).contains("expires=").contains("signature=");
        assertThat(url).doesNotContain("aliyuncs.com");
        verify(storage, never()).presignedGet(anyString(), any());
    }

    @Test
    void attachmentSigningStillReturnsDirectOssUrl() {
        when(storage.presignedGet(eq(OBJECT_KEY), any())).thenReturn(OSS_URL);
        FileSignatureContext context = context();

        assertThat(context.sign(OBJECT_KEY)).isEqualTo(OSS_URL);
    }

    @Test
    void remoteObjectIsMaterializedIntoDownloadableLocalFile() throws Exception {
        byte[] content = "%PDF-1.4 archived quote".getBytes(StandardCharsets.UTF_8);
        when(storage.remote()).thenReturn(true);
        when(storage.get(OBJECT_KEY)).thenReturn(new ByteArrayInputStream(content));
        FileSignatureContext context = context();

        File file = context.materialize(OBJECT_KEY);

        assertThat(file).exists();
        assertThat(Files.readAllBytes(file.toPath())).isEqualTo(content);
        assertThat(file.getName()).endsWith(".pdf");
    }

    @Test
    void localObjectStillResolvesFromUploadDirectory() throws Exception {
        Path local = uploadDir.resolve("reports/safe.txt");
        Files.createDirectories(local.getParent());
        Files.writeString(local, "local content");
        when(storage.remote()).thenReturn(false);
        FileSignatureContext context = context();

        File file = context.materialize("upload/reports/safe.txt");

        assertThat(file).exists();
        assertThat(Files.readString(file.toPath())).isEqualTo("local content");
    }

    @Test
    void gatewaySignatureVerifiesAndServesRemoteContent() throws Exception {
        byte[] content = "%PDF-1.4 archived quote".getBytes(StandardCharsets.UTF_8);
        when(storage.remote()).thenReturn(true);
        when(storage.get(OBJECT_KEY)).thenReturn(new ByteArrayInputStream(content));
        FileSignatureContext context = context();

        String url = context.signGateway(OBJECT_KEY);
        Map<String, String> query = Arrays.stream(url.substring(url.indexOf('?') + 1).split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(pair -> pair[0], pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));

        assertThat(context.verify(query.get("path"), Long.valueOf(query.get("expires")), query.get("signature")))
                .isTrue();
        assertThat(Files.readAllBytes(context.materialize(query.get("path")).toPath())).isEqualTo(content);
    }

    @Test
    void gatewayUrlCarriesBusinessFileNameForDownload() {
        FileSignatureContext context = context();

        String url = context.signGateway(OBJECT_KEY,
                "麦鸿设计_谢玲玲_许倩_香樟花园_50.54平方米_168000元_V2.pdf");

        assertThat(url).contains("&name=");
        assertThat(url).contains("V2.pdf");
        assertThat(url).doesNotContain("aliyuncs.com");
    }

    @Test
    void materializeUsesBusinessFileNameAndStripsSeparators() throws Exception {
        byte[] content = "%PDF-1.4 archived quote".getBytes(StandardCharsets.UTF_8);
        when(storage.get(OBJECT_KEY)).thenReturn(new ByteArrayInputStream(content));
        FileSignatureContext context = context();

        File file = context.materialize(OBJECT_KEY, "麦鸿设计/谢玲玲_许倩_V2.pdf");

        assertThat(file.getName()).isEqualTo("麦鸿设计谢玲玲_许倩_V2.pdf");
        assertThat(Files.readAllBytes(file.toPath())).isEqualTo(content);
    }

    private FileSignatureContext context() {
        return new FileSignatureContext(storage, uploadDir.toString(), "unit-test-secret", 300);
    }
}
