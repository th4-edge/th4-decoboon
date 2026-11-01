package com.th4.edge.tenant.user.context;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.io.StreamFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 企业 Logo 文件存储：只接受图片，由服务端生成对象键并返回稳定展示地址。 */
@Component
public class CompanyBrandContext {

    private static final long MAX_SIZE_BYTES = 2 * 1024 * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final ObjectStorage storage;
    private final Path publicDir;
    private final String publicBaseUrl;

    public CompanyBrandContext(ObjectStorage storage,
                               @Value("${th4.edge.framework.public-dir:./data/public/}") String publicDir,
                               @Value("${th4.edge.framework.public-base-url:}") String publicBaseUrl) {
        this.storage = storage;
        this.publicDir = Paths.get(publicDir).toAbsolutePath().normalize();
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
        this.publicBaseUrl = base;
    }

    public StoredLogo save(Long companyId, StreamFile file) {
        if (companyId == null || companyId <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "企业Logo归属参数无效"));
        }
        requireImage(file);
        String extension = extensionOf(file.getOriginalFilename());
        return storage.remote()
                ? saveToObjectStorage(companyId, extension, file)
                : saveToPublicDirectory(companyId, extension, file);
    }

    private StoredLogo saveToObjectStorage(Long companyId, String extension, StreamFile file) {
        String key = "company-brand/" + companyId + "/logo-"
                + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try (var input = file.getInputStream()) {
            storage.put(key, input, file.getSize(), file.getContentType());
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "企业Logo保存失败，请稍后重试"));
        }
        String path = "/file/private/download?path=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
        return new StoredLogo(key, publicBaseUrl.isBlank() ? path : publicBaseUrl + path);
    }

    private StoredLogo saveToPublicDirectory(Long companyId, String extension, StreamFile file) {
        String relative = "company-brand/" + companyId + "/logo-"
                + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = publicDir.resolve(relative).normalize();
        if (!target.startsWith(publicDir)) {
            throw new DefinedException(new DefaultErrorCode("400", "企业Logo路径无效"));
        }
        try {
            Files.createDirectories(target.getParent());
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "企业Logo保存失败，请稍后重试"));
        }
        String path = "/public/" + relative;
        return new StoredLogo(path, publicBaseUrl.isBlank() ? path : publicBaseUrl + path);
    }

    private void requireImage(StreamFile file) {
        if (file == null || file.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择要上传的企业Logo"));
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new DefinedException(new DefaultErrorCode("400", "企业Logo不能超过 2MB"));
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.isBlank() && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new DefinedException(new DefaultErrorCode("400", "企业Logo仅支持 JPG / PNG / WebP 图片"));
        }
        if (!ALLOWED_EXTENSIONS.contains(extensionOf(file.getOriginalFilename()))) {
            throw new DefinedException(new DefaultErrorCode("400", "企业Logo仅支持 JPG / PNG / WebP 图片"));
        }
    }

    private String extensionOf(String originalName) {
        if (originalName == null) return "";
        String normalized = originalName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) normalized = normalized.substring(slash + 1);
        int dot = normalized.lastIndexOf('.');
        if (dot <= 0 || dot == normalized.length() - 1) return "";
        return normalized.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredLogo(String path, String url) {
    }
}
