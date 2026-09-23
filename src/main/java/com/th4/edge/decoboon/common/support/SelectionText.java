package com.th4.edge.decoboon.common.support;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主材选材结果文本化。
 *
 * <p>历史坑：{@code MaterialSelection.selectionJson} 是给前端回显表单用的结构化快照，
 * 曾经被直接塞进业主旅程的 {@code lines}，业主端看到的是一串
 * <code>{"color":"...","spec":"..."}</code>。这里统一把 JSON / 可读文本收敛成
 * "标签 + 值"的有序字段，供旅程投影生成 facts 与兜底文本。</p>
 */
public final class SelectionText {

    /** 选材结果字段：key 稳定、label 面向业主、value 为原文。 */
    public record Field(String key, String label, String value) {
    }

    /** JSON 键 → 中文标签，顺序即前端录入顺序。 */
    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put("color", "颜色/花色");
        LABELS.put("spec", "规格/型号");
        LABELS.put("brand", "品牌/系列");
        LABELS.put("craft", "工艺");
        LABELS.put("remark", "备注");
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SelectionText() {
    }

    /**
     * 解析选材结果：优先结构化 JSON，其次按"；/："拆分可读文本。
     * 两者都没有有效内容时返回空列表，调用方据此决定是否展示占位。
     */
    public static List<Field> fields(String content, String json) {
        List<Field> fromJson = fromJson(json);
        return fromJson.isEmpty() ? fromContent(content) : fromJson;
    }

    private static List<Field> fromJson(String json) {
        List<Field> result = new ArrayList<>();
        if (json == null || json.isBlank() || !json.trim().startsWith("{")) {
            return result;
        }
        Map<?, ?> parsed;
        try {
            parsed = MAPPER.readValue(json, Map.class);
        } catch (Exception ignored) {
            return result;
        }
        for (Map.Entry<String, String> entry : LABELS.entrySet()) {
            Object raw = parsed.get(entry.getKey());
            if (raw == null) {
                continue;
            }
            String value = String.valueOf(raw).trim();
            if (!value.isEmpty()) {
                result.add(new Field(entry.getKey(), entry.getValue(), value));
            }
        }
        return result;
    }

    private static List<Field> fromContent(String content) {
        List<Field> result = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return result;
        }
        int index = 0;
        for (String segment : content.split("；")) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int colon = trimmed.indexOf('：');
            if (colon <= 0) {
                result.add(new Field("segment-" + (index++), "", trimmed));
                continue;
            }
            String label = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();
            if (!value.isEmpty()) {
                result.add(new Field(keyOf(label, index++), label, value));
            }
        }
        return result;
    }

    private static String keyOf(String label, int index) {
        for (Map.Entry<String, String> entry : LABELS.entrySet()) {
            if (entry.getValue().equals(label)) {
                return entry.getKey();
            }
        }
        return "field-" + index;
    }
}
