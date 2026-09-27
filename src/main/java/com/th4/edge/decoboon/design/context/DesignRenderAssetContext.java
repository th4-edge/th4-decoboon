package com.th4.edge.decoboon.design.context;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

/** 将AI模型临时图片持久化到私有对象存储。 */
@Note("AI效果图资产上下文")
@Component
public class DesignRenderAssetContext {

    private static final long MAX_IMAGE_BYTES = 30L * 1024 * 1024;

    private final ObjectStorage objectStorage;
    private final HttpClient httpClient;

    public DesignRenderAssetContext(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    public String persist(Long designId, Long generationId, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new DesignRenderException("AI_RENDER_STORE_FAILED", "AI效果图为空");
        }
        if (imageUrl.startsWith("upload/") || imageUrl.startsWith("/upload/")) {
            return imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(120))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new DesignRenderException("AI_RENDER_STORE_FAILED", "AI效果图下载失败");
            }
            byte[] bytes = response.body();
            if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
                throw new DesignRenderException("AI_RENDER_STORE_FAILED", "AI效果图大小异常");
            }
            String contentType = response.headers().firstValue("content-type")
                    .orElse("image/jpeg").split(";")[0].trim();
            String extension = extension(contentType, imageUrl);
            String key = "design/ai-render/" + designId + "/" + generationId + "-"
                    + UUID.randomUUID().toString().replace("-", "") + extension;
            objectStorage.put(key, new ByteArrayInputStream(bytes), bytes.length, contentType);
            return "upload/" + key;
        } catch (DesignRenderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DesignRenderException("AI_RENDER_STORE_FAILED", "AI效果图持久化失败", ex);
        }
    }

    private String extension(String contentType, String imageUrl) {
        String lower = (contentType + " " + imageUrl).toLowerCase(Locale.ROOT);
        if (lower.contains("png")) return ".png";
        if (lower.contains("webp")) return ".webp";
        if (lower.contains("jpeg") || lower.contains("jpg")) return ".jpg";
        return ".jpg";
    }
}
