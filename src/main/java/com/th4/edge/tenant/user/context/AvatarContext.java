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

/**
 * 员工头像存储。
 *
 * <p>生产对象存储使用 OSS：头像对象写入私有 Bucket，复用受约束的公开头像下载分支提供稳定地址，
 * 不使用私有文件的短期预签名地址，也不依赖对象级公共 ACL。local provider 继续写显式
 * public-dir，便于开发与历史数据兼容。</p>
 *
 * <p>安全边界：文件名由服务端 UUID 生成，只接受图片类型与 2MB 以内文件，
 * 不接受客户端传入路径，避免路径穿越与任意文件写入。</p>
 */
@Component
public class AvatarContext {

    private static final long MAX_SIZE_BYTES = 2 * 1024 * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final ObjectStorage storage;
    private final Path publicDir;
    private final String publicBaseUrl;

    public AvatarContext(ObjectStorage storage,
                         @Value("${th4.edge.framework.public-dir:./data/public/}") String publicDir,
                         @Value("${th4.edge.framework.public-base-url:}") String publicBaseUrl) {
        this.storage = storage;
        this.publicDir = Paths.get(publicDir).toAbsolutePath().normalize();
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
        this.publicBaseUrl = base;
    }

    /** 保存头像并返回业务标识与可长期访问地址。 */
    public StoredAvatar save(Long companyId, Long employeeId, StreamFile file) {
        requireTarget(companyId, employeeId);
        requireImage(file);
        String extension = extensionOf(file.getOriginalFilename());
        return storage.remote()
                ? saveToObjectStorage(companyId, employeeId, extension, file)
                : saveToPublicDirectory(companyId, employeeId, extension, file);
    }

    private StoredAvatar saveToObjectStorage(Long companyId, Long employeeId, String extension, StreamFile file) {
        String key = "avatar/" + companyId + "/" + employeeId + "-"
                + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try (var input = file.getInputStream()) {
            storage.put(key, input, file.getSize(), file.getContentType());
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "头像保存失败，请稍后重试"));
        }
        String path = "/file/private/download?path=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
        return new StoredAvatar(key, publicBaseUrl.isBlank() ? path : publicBaseUrl + path);
    }

    private StoredAvatar saveToPublicDirectory(Long companyId, Long employeeId, String extension, StreamFile file) {
        String relative = "avatar/" + companyId + "/" + employeeId + "-"
                + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = publicDir.resolve(relative).normalize();
        if (!target.startsWith(publicDir)) {
            throw new DefinedException(new DefaultErrorCode("400", "头像路径无效"));
        }
        try {
            Files.createDirectories(target.getParent());
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "头像保存失败，请稍后重试"));
        }
        String path = "/public/" + relative;
        return new StoredAvatar(path, publicBaseUrl.isBlank() ? path : publicBaseUrl + path);
    }

    private void requireTarget(Long companyId, Long employeeId) {
        if (companyId == null || companyId <= 0 || employeeId == null || employeeId <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "头像归属参数无效"));
        }
    }

    private void requireImage(StreamFile file) {
        if (file == null || file.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择要上传的头像"));
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new DefinedException(new DefaultErrorCode("400", "头像不能超过 2MB"));
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.isBlank() && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new DefinedException(new DefaultErrorCode("400", "头像仅支持 JPG / PNG / WebP 图片"));
        }
        if (!ALLOWED_EXTENSIONS.contains(extensionOf(file.getOriginalFilename()))) {
            throw new DefinedException(new DefaultErrorCode("400", "头像仅支持 JPG / PNG / WebP 图片"));
        }
    }

    private String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }
        String normalized = originalName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        int dot = normalized.lastIndexOf('.');
        if (dot <= 0 || dot == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** path 是本地公开路径或 OSS 对象键，url 是可长期展示的匿名访问地址。 */
    public record StoredAvatar(String path, String url) {
    }
}
