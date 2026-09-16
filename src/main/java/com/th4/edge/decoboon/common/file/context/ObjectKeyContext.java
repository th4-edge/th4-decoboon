package com.th4.edge.decoboon.common.file.context;

import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 企业/项目对象键策略：先企业隔离，再项目隔离，最后按业务域和年月归档。 */
@Component
public class ObjectKeyContext {

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "dll", "jar", "sh", "bat", "cmd", "com", "msi", "scr", "js", "mjs", "php", "jsp", "html", "htm"
    );

    public String file(Long companyId, Long projectId, FileScope scope, String originalName) {
        return key(companyId, projectId, scope, extension(originalName), "");
    }

    public String generated(Long companyId, Long projectId, FileScope scope, String originalName) {
        return key(companyId, projectId, scope, extension(originalName), "generated-");
    }

    private String key(Long companyId, Long projectId, FileScope scope, String extension, String marker) {
        if (companyId == null || companyId <= 0) throw invalid();
        if (projectId != null && projectId <= 0) throw invalid();
        LocalDate today = LocalDate.now();
        StringBuilder key = new StringBuilder("companies/").append(companyId).append('/');
        if (projectId != null) key.append("projects/").append(projectId).append('/');
        key.append("files/").append(scope.name().toLowerCase(Locale.ROOT)).append('/')
                .append(today.format(YEAR)).append('/').append(today.format(MONTH)).append('/')
                .append(marker).append(UUID.randomUUID().toString().replace("-", ""));
        if (!extension.isBlank()) key.append('.').append(extension);
        return key.toString();
    }

    public String extension(String originalName) {
        if (originalName == null || originalName.isBlank()) throw invalid();
        String normalized = originalName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) normalized = normalized.substring(slash + 1);
        int dot = normalized.lastIndexOf('.');
        if (dot <= 0 || dot == normalized.length() - 1) throw new DefinedException(
                new DefaultErrorCode("400", "文件必须包含扩展名"));
        String extension = normalized.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!extension.matches("[a-z0-9]{1,10}") || BLOCKED_EXTENSIONS.contains(extension)) {
            throw new DefinedException(new DefaultErrorCode("400", "不支持的文件类型"));
        }
        return extension;
    }

    private DefinedException invalid() {
        return new DefinedException(new DefaultErrorCode("400", "文件存储归属参数无效"));
    }
}
