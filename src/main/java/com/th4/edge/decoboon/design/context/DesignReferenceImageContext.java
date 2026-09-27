package com.th4.edge.decoboon.design.context;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/** 读取设计图纸并转换为模型可接收的图片引用。 */
@Note("AI设计参考图上下文")
@Component
public class DesignReferenceImageContext {

    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;

    private final ObjectStorage objectStorage;

    public DesignReferenceImageContext(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    public String resolveDataUrl(Design design) {
        return resolveDataUrl(firstImagePath(design));
    }

    public String resolveDataUrl(String path) {
        if (path == null || path.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少可用于AI生成的CAD图片或户型图片"));
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String key = normalizeKey(path);
        try (InputStream input = objectStorage.get(key)) {
            if (input == null) {
                throw new DefinedException(new DefaultErrorCode("404", "CAD图片或户型图片不存在"));
            }
            byte[] bytes = input.readNBytes((int) MAX_IMAGE_BYTES + 1);
            if (bytes.length == 0) {
                throw new DefinedException(new DefaultErrorCode("400", "CAD图片或户型图片为空"));
            }
            if (bytes.length > MAX_IMAGE_BYTES) {
                throw new DefinedException(new DefaultErrorCode("400", "CAD图片或户型图片超过20MB"));
            }
            return "data:" + contentType(path) + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "CAD图片或户型图片读取失败"));
        }
    }

    public String firstImagePath(Design design) {
        List<String> candidates = new ArrayList<>();
        add(candidates, design.getCadPreviewUrl());
        add(candidates, design.getOriginalPlanUrl());
        if (design.getOriginalPlanImages() != null) candidates.addAll(design.getOriginalPlanImages());
        if (design.getSitePhotos() != null) candidates.addAll(design.getSitePhotos());
        if (design.getSurveyPhotos() != null) candidates.addAll(design.getSurveyPhotos());
        add(candidates, design.getCadUrl());
        return candidates.stream().filter(this::isImage).findFirst().orElse(null);
    }

    private void add(List<String> values, String value) {
        if (value != null && !value.isBlank()) values.add(value);
    }

    private boolean isImage(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".webp") || lower.startsWith("http://") || lower.startsWith("https://");
    }

    private String normalizeKey(String path) {
        if (path.startsWith("/upload/")) return path.substring("/upload/".length());
        if (path.startsWith("upload/")) return path.substring("upload/".length());
        return path;
    }

    private String contentType(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/png";
    }
}
