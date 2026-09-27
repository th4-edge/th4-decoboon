package com.th4.edge.decoboon.design.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.design.configure.DesignGenerationMode;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 将结构化设计上下文渲染为视觉、谈单和方案书三类输出。 */
@Note("AI设计Prompt渲染器")
@Component
public class DesignPromptRenderer {

    public static final String TEMPLATE_VERSION = "design-prompt-v1";
    public static final String DEFAULT_MODEL = "doubao-seedream-5-0-pro-260628";

    private final ObjectMapper objectMapper;

    public DesignPromptRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PromptBundle render(DesignAiContextBuilder.ContextData context) {
        String visualPrompt = visualPrompt(context);
        String talkPrompt = talkPrompt(context);
        String documentBriefJson = documentBriefJson(context);
        String parameters = "{\"size\":\"2048x1152\",\"response_format\":\"url\",\"watermark\":false}";
        return new PromptBundle(visualPrompt, talkPrompt, documentBriefJson, TEMPLATE_VERSION, DEFAULT_MODEL, parameters);
    }

    private String visualPrompt(DesignAiContextBuilder.ContextData context) {
        Map<String, Object> values = context.getValues();
        StringBuilder text = new StringBuilder();
        text.append("你是装得清的专业家装设计助手。请基于真实户型与客户信息生成方案效果图。\n");
        text.append("生成模式：").append(context.getMode() == DesignGenerationMode.Precise ? "精准共创" : "概念探索").append("\n");
        text.append("项目事实：").append(fact(values.get("property"))).append("\n");
        text.append("客户画像：").append(fact(values.get("portrait"))).append("\n");
        text.append("量房事实：").append(fact(values.get("measure"))).append("\n");
        text.append("设计方案：").append(fact(values.get("design"))).append("\n");
        text.append("区域尺寸：\n").append(listFact(values.get("spaces")));
        List<Map<String, Object>> requirements = maps(values.get("requirements"));
        text.append("必须满足：\n").append(requirementLines(requirements, "诉求"));
        text.append("必须避免：\n").append(requirementLines(requirements, "排斥点"));
        text.append("结构约束：严格保留原户型外墙轮廓、房间数量、主要隔墙、门窗和阳台/厨卫关系，不得新增或删除房间。\n");
        text.append("视觉要求：真实材质、自然光、合理家具尺度、商业家装效果图质量。\n");
        text.append("禁止：文字、标注、水印、施工承诺、虚构材料品牌、成本价格。\n");
        if (!context.getMissingFields().isEmpty()) {
            text.append("缺失信息：").append(String.join("、", context.getMissingFields())).append("，不得自行补造。\n");
        }
        if (!context.getConflictFlags().isEmpty()) {
            text.append("待确认冲突：").append(String.join("；", context.getConflictFlags())).append("。\n");
        }
        return text.toString().trim();
    }

    private String talkPrompt(DesignAiContextBuilder.ContextData context) {
        Map<String, Object> values = context.getValues();
        StringBuilder text = new StringBuilder();
        text.append("请为设计师生成一份简洁的客户讲解提纲，不得承诺价格、工期或施工结果。\n");
        text.append("先确认原户型和尺寸，再讲整体策略，最后逐间解释设计动作。\n");
        text.append("客户画像：").append(fact(values.get("portrait"))).append("\n");
        text.append("高优先级诉求：\n").append(requirementLines(maps(values.get("requirements")), "诉求"));
        text.append("排斥点：\n").append(requirementLines(maps(values.get("requirements")), "排斥点"));
        text.append("讲解顺序：1.确认房屋结构；2.说明空间策略；3.逐间说明设计动作；4.说明材质与风险；5.请客户选择喜欢、调整或不确定；6.进入现场复核与报价。\n");
        text.append("必须说明：AI图为概念效果，正式设计、材料、工艺和金额以责任角色确认及正式报价为准。\n");
        return text.toString().trim();
    }

    private String documentBriefJson(DesignAiContextBuilder.ContextData context) {
        Map<String, Object> values = context.getValues();
        Map<String, Object> brief = new LinkedHashMap<>();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("mode", context.getMode().name());
        meta.put("confidence", context.getConfidence());
        meta.put("templateVersion", TEMPLATE_VERSION);
        brief.put("meta", meta);
        brief.put("heard", maps(values.get("requirements")));
        brief.put("diagnosis", context.getConflictFlags());
        brief.put("spaces", maps(values.get("spaces")));
        brief.put("materials", List.of());
        Map<String, Object> budget = new LinkedHashMap<>();
        budget.put("status", "UNAVAILABLE");
        budget.put("message", "暂无正式报价依据，预算暂不可确认。");
        brief.put("budget", budget);
        brief.put("missingFields", context.getMissingFields());
        brief.put("disclaimer", "AI概念效果，正式设计、材料、工艺和金额以责任角色确认及正式报价为准。");
        try {
            return objectMapper.writeValueAsString(brief);
        } catch (JsonProcessingException ex) {
            throw new DefinedException(new DefaultErrorCode("500", "方案书结构化内容序列化失败"));
        }
    }

    private String requirementLines(List<Map<String, Object>> requirements, String kind) {
        List<String> lines = new ArrayList<>();
        for (Map<String, Object> requirement : requirements) {
            String actualKind = string(requirement.get("kind"));
            if (!kind.equals(actualKind)) {
                continue;
            }
            String priority = string(requirement.get("priority"));
            String title = string(requirement.get("title"));
            String detail = string(requirement.get("detail"));
            String line = "- " + (priority.isBlank() ? "" : "[" + priority + "] ") + title;
            if (!detail.isBlank()) line += "：" + detail;
            lines.add(line);
        }
        return lines.isEmpty() ? "- 暂无\n" : String.join("\n", lines) + "\n";
    }

    private String listFact(Object value) {
        List<Map<String, Object>> items = maps(value);
        if (items.isEmpty()) return "- 暂无\n";
        List<String> lines = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String name = string(item.get("name"));
            String type = string(item.get("type"));
            String length = string(item.get("length"));
            String width = string(item.get("width"));
            String height = string(item.get("height"));
            String area = string(item.get("area"));
            String remark = string(item.get("remark"));
            StringBuilder line = new StringBuilder("- ");
            line.append(name.isBlank() ? type : name);
            if (!length.isBlank() || !width.isBlank()) {
                line.append("：长").append(length).append("m，宽").append(width).append("m");
            }
            if (!height.isBlank()) line.append("，层高").append(height).append("m");
            if (!area.isBlank()) line.append("，面积").append(area).append("㎡");
            if (!remark.isBlank()) line.append("，说明：").append(remark);
            lines.add(line.toString());
        }
        return String.join("\n", lines) + "\n";
    }

    private String fact(Object value) {
        if (!(value instanceof Map<?, ?> map) || map.isEmpty()) return "暂无";
        List<String> parts = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof String text && !text.isBlank()) {
                parts.add(entry.getKey() + "=" + text);
            } else if (entry.getValue() instanceof Number || entry.getValue() instanceof Boolean) {
                parts.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return parts.isEmpty() ? "暂无" : String.join("，", parts);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) result.add((Map<String, Object>) map);
        }
        return result;
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** 三类Prompt输出。 */
    public static final class PromptBundle {
        private final String visualPrompt;
        private final String talkPrompt;
        private final String documentBriefJson;
        private final String templateVersion;
        private final String modelName;
        private final String modelParametersJson;

        private PromptBundle(String visualPrompt, String talkPrompt, String documentBriefJson,
                             String templateVersion, String modelName, String modelParametersJson) {
            this.visualPrompt = visualPrompt;
            this.talkPrompt = talkPrompt;
            this.documentBriefJson = documentBriefJson;
            this.templateVersion = templateVersion;
            this.modelName = modelName;
            this.modelParametersJson = modelParametersJson;
        }

        public String getVisualPrompt() { return visualPrompt; }
        public String getTalkPrompt() { return talkPrompt; }
        public String getDocumentBriefJson() { return documentBriefJson; }
        public String getTemplateVersion() { return templateVersion; }
        public String getModelName() { return modelName; }
        public String getModelParametersJson() { return modelParametersJson; }
    }
}
