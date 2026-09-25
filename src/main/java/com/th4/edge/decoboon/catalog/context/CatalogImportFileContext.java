package com.th4.edge.decoboon.catalog.context;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.util.Locale;

/** 报价项目库导入文件读写：上传落对象存储，确认导入时按同一路径重新读取。 */
@Component
public class CatalogImportFileContext {

    private final ObjectStorage storage;

    public CatalogImportFileContext(ObjectStorage storage) {
        this.storage = storage;
    }

    /** 保存上传模板，返回对象键；同一企业同一文件指纹复用同一对象。 */
    public String store(Long comId, String fingerprint, String originalName, byte[] bytes) {
        String extension = extension(originalName);
        String key = "companies/" + comId + "/catalog-import/" + YearMonth.now() + "/"
                + fingerprint + extension;
        if (!storage.exists(key)) {
            storage.put(key, new ByteArrayInputStream(bytes), bytes.length, contentType(extension));
        }
        return key;
    }

    /** 读取已保存的导入文件（预览与确认必须使用同一份文件）。 */
    public byte[] read(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "导入文件不存在，请重新上传"));
        }
        try (InputStream input = storage.get(sourcePath)) {
            return input.readAllBytes();
        } catch (IOException | RuntimeException ex) {
            throw new DefinedException(new DefaultErrorCode("400", "导入文件已不可读取，请重新上传模板文件"));
        }
    }

    private String extension(String originalName) {
        String name = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
        return name.endsWith(".csv") ? ".csv" : ".xlsx";
    }

    private String contentType(String extension) {
        return ".csv".equals(extension)
                ? "text/csv"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }
}
