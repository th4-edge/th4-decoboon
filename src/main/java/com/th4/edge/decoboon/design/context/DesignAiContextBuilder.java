package com.th4.edge.decoboon.design.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.common.support.DateTimeText;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerPortrait;
import com.th4.edge.decoboon.customer.repository.CustomerPortraitRepository;
import com.th4.edge.decoboon.design.configure.DesignGenerationMode;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.framework.data.enums.Enumerable;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 从装得清现有业务对象构建AI设计上下文。 */
@Note("AI设计上下文构建器")
@Component
public class DesignAiContextBuilder {

    private final ObjectMapper objectMapper;
    private final CustomerPortraitRepository portraitRepository;
    private final MeasureRepository measureRepository;
    private final SpaceRepository spaceRepository;
    private final RequirementRepository requirementRepository;

    public DesignAiContextBuilder(ObjectMapper objectMapper,
                                  CustomerPortraitRepository portraitRepository,
                                  MeasureRepository measureRepository,
                                  SpaceRepository spaceRepository,
                                  RequirementRepository requirementRepository) {
        this.objectMapper = objectMapper;
        this.portraitRepository = portraitRepository;
        this.measureRepository = measureRepository;
        this.spaceRepository = spaceRepository;
        this.requirementRepository = requirementRepository;
    }

    public ContextData build(Design design, DesignGenerationMode requestedMode) {
        if (design == null || design.getDesignId() == null || design.getCompany() == null
                || design.getCustomer() == null || design.getProperty() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "设计方案上下文不完整"));
        }
        if (!hasPlanInput(design)) {
            throw new DefinedException(new DefaultErrorCode("400", "请先上传CAD图纸或户型图片"));
        }

        Long comId = design.getCompany().getComId();
        Long customerId = design.getCustomer().getCustId();
        Long propertyId = design.getProperty().getPropertyId();
        CustomerPortrait portrait = portraitRepository
                .findByCompany_ComIdAndCustomer_CustId(comId, customerId)
                .orElse(null);
        Measure measure = measureRepository
                .findByCompany_ComIdAndProperty_PropertyIdOrderByMeasureTimeDesc(comId, propertyId)
                .stream().findFirst().orElse(null);
        List<Space> spaces = spaceRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId());
        List<Requirement> requirements = requirementRepository
                .findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(comId, List.of(propertyId));
        requirements = requirements.stream()
                .sorted(Comparator.comparingInt((Requirement item) ->
                                item.getPriority() == null ? 99 : item.getPriority().ordinal())
                        .thenComparing(Requirement::getRequirementId))
                .toList();

        List<String> missing = missingFields(design, portrait, measure, spaces, requirements);
        List<String> conflicts = conflictFlags(design, portrait, measure, spaces, requirements);
        boolean precise = requestedMode == DesignGenerationMode.Precise && measure != null && !spaces.isEmpty();
        DesignGenerationMode mode = precise ? DesignGenerationMode.Precise : DesignGenerationMode.Concept;
        String confidence = confidence(measure, spaces, requirements, missing, conflicts);

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("schemaVersion", "design-context-v1");
        values.put("mode", mode.name());
        values.put("generatedAt", LocalDateTime.now().toString());
        values.put("customer", customerFacts(design.getCustomer()));
        values.put("portrait", portraitFacts(portrait));
        values.put("property", propertyFacts(design.getProperty()));
        values.put("measure", measureFacts(measure));
        values.put("design", designFacts(design));
        values.put("spaces", spaceFacts(spaces));
        values.put("requirements", requirementFacts(requirements));
        values.put("missingFields", missing);
        values.put("conflicts", conflicts);
        values.put("confidence", confidence);

        try {
            String json = objectMapper.writeValueAsString(values);
            return new ContextData(mode, values, json, digest(json), missing, conflicts, confidence);
        } catch (JsonProcessingException ex) {
            throw new DefinedException(new DefaultErrorCode("500", "设计上下文序列化失败"));
        }
    }

    private Map<String, Object> customerFacts(Customer customer) {
        Map<String, Object> value = new LinkedHashMap<>();
        put(value, "area", customer.getArea());
        put(value, "budgetWan", customer.getBudget());
        put(value, "status", enumName(customer.getStatus()));
        return value;
    }

    private Map<String, Object> portraitFacts(CustomerPortrait portrait) {
        Map<String, Object> value = new LinkedHashMap<>();
        if (portrait == null) {
            return value;
        }
        put(value, "decisionRole", enumName(portrait.getDecisionRole()));
        put(value, "purpose", enumName(portrait.getPurpose()));
        put(value, "houseCondition", enumName(portrait.getHouseCondition()));
        put(value, "style", enumName(portrait.getStyle()));
        put(value, "eco", enumName(portrait.getEco()));
        put(value, "household", enumName(portrait.getHousehold()));
        put(value, "concerns", portrait.getConcerns() == null ? List.of()
                : portrait.getConcerns().stream().map(this::enumName).filter(this::hasText).toList());
        return value;
    }

    private Map<String, Object> propertyFacts(Property property) {
        Map<String, Object> value = new LinkedHashMap<>();
        put(value, "area", property.getArea());
        put(value, "layout", property.getLayout());
        put(value, "status", enumName(property.getStatus()));
        return value;
    }

    private Map<String, Object> measureFacts(Measure measure) {
        Map<String, Object> value = new LinkedHashMap<>();
        if (measure == null) {
            return value;
        }
        put(value, "originalStructure", measure.getOriginalStructure());
        put(value, "dimensions", measure.getDimensions());
        put(value, "customerRequirements", measure.getCustomerRequirements());
        put(value, "designerConclusion", measure.getDesignerConclusion());
        put(value, "sitePhotoCount", size(measure.getSitePhotos()));
        put(value, "structureImageCount", size(measure.getStructureImages()));
        put(value, "attachmentCount", size(measure.getAttachments()));
        put(value, "completedAt", measure.getCompletedAt() == null ? null : measure.getCompletedAt().toString());
        return value;
    }

    private Map<String, Object> designFacts(Design design) {
        Map<String, Object> value = new LinkedHashMap<>();
        put(value, "name", design.getName());
        put(value, "style", design.getStyle());
        put(value, "area", design.getArea());
        put(value, "layout", design.getLayout());
        put(value, "description", design.getDescription());
        put(value, "hasCad", hasText(design.getCadUrl()));
        put(value, "hasCadPreview", hasText(design.getCadPreviewUrl()));
        put(value, "hasOriginalPlan", hasText(design.getOriginalPlanUrl()));
        put(value, "originalPlanImageCount", size(design.getOriginalPlanImages()));
        put(value, "sitePhotoCount", size(design.getSitePhotos()));
        put(value, "surveyPhotoCount", size(design.getSurveyPhotos()));
        put(value, "updatedAt", design.getUpdateTime() == null ? null : DateTimeText.format(design.getUpdateTime()));
        return value;
    }

    private List<Map<String, Object>> spaceFacts(List<Space> spaces) {
        if (spaces == null) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Space space : spaces) {
            Map<String, Object> value = new LinkedHashMap<>();
            put(value, "name", space.getName());
            put(value, "type", enumName(space.getType()));
            put(value, "length", space.getLength());
            put(value, "width", space.getWidth());
            put(value, "height", space.getHeight());
            put(value, "area", space.getArea());
            put(value, "perimeter", space.getPerimeter());
            put(value, "remark", space.getRemark());
            result.add(value);
        }
        return result;
    }

    private List<Map<String, Object>> requirementFacts(List<Requirement> requirements) {
        if (requirements == null) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Requirement requirement : requirements) {
            Map<String, Object> value = new LinkedHashMap<>();
            put(value, "spaceName", requirement.getSpaceName());
            put(value, "kind", enumName(requirement.getKind()));
            put(value, "category", enumName(requirement.getCategory()));
            put(value, "priority", enumName(requirement.getPriority()));
            put(value, "title", requirement.getTitle());
            put(value, "detail", requirement.getDetail());
            put(value, "response", requirement.getResponse());
            result.add(value);
        }
        return result;
    }

    private List<String> missingFields(Design design, CustomerPortrait portrait, Measure measure,
                                       List<Space> spaces, List<Requirement> requirements) {
        List<String> missing = new ArrayList<>();
        if (portrait == null) {
            missing.add("客户画像");
        }
        if (measure == null) {
            missing.add("量房记录");
        } else {
            if (!hasText(measure.getOriginalStructure())) missing.add("原始结构说明");
            if (!hasText(measure.getDimensions())) missing.add("量房尺寸");
            if (!hasText(measure.getDesignerConclusion())) missing.add("设计师结论");
        }
        if (spaces == null || spaces.isEmpty()) {
            missing.add("方案区域");
        }
        if (requirements == null || requirements.isEmpty()) {
            missing.add("客户诉求");
        }
        if (!hasText(design.getCadUrl()) && !hasText(design.getCadPreviewUrl())) {
            missing.add("CAD源文件或预览图");
        }
        return missing;
    }

    private List<String> conflictFlags(Design design, CustomerPortrait portrait, Measure measure,
                                       List<Space> spaces, List<Requirement> requirements) {
        List<String> conflicts = new ArrayList<>();
        boolean hasBudget = design.getCustomer().getBudget() != null
                && design.getCustomer().getBudget().compareTo(BigDecimal.ZERO) > 0;
        if (!hasBudget && requirements != null && requirements.stream()
                .anyMatch(item -> item.getPriority() != null && item.getPriority().name().equals("High"))) {
            conflicts.add("存在高优先级诉求但客户预算未确认");
        }
        if (measure == null && spaces != null && !spaces.isEmpty()) {
            conflicts.add("已有区域尺寸但缺少量房原始记录，结构准确性待确认");
        }
        if (portrait != null && portrait.getHousehold() != null
                && portrait.getHousehold().name().equals("ThreeGenerations")
                && requirements != null && requirements.stream().noneMatch(item ->
                        contains(item.getTitle(), "老人") || contains(item.getDetail(), "老人")
                                || contains(item.getTitle(), "无障碍") || contains(item.getDetail(), "无障碍"))) {
            conflicts.add("画像为三代同堂，但未记录老人/无障碍相关诉求");
        }
        return conflicts;
    }

    private String confidence(Measure measure, List<Space> spaces, List<Requirement> requirements,
                              List<String> missing, List<String> conflicts) {
        boolean completeMeasure = measure != null && hasText(measure.getDimensions())
                && hasText(measure.getDesignerConclusion());
        boolean completeSpace = spaces != null && !spaces.isEmpty();
        boolean completeRequirement = requirements != null && !requirements.isEmpty();
        boolean coreMissing = missing.contains("量房记录") || missing.contains("量房尺寸")
                || missing.contains("方案区域");
        if (coreMissing) {
            return "Low";
        }
        if (completeMeasure && completeSpace && completeRequirement && missing.isEmpty() && conflicts.isEmpty()) {
            return "High";
        }
        if (completeSpace && (completeMeasure || completeRequirement)) {
            return "Medium";
        }
        return "Low";
    }

    private boolean hasPlanInput(Design design) {
        return hasText(design.getCadUrl())
                || hasText(design.getCadPreviewUrl())
                || hasText(design.getOriginalPlanUrl())
                || (design.getOriginalPlanImages() != null && !design.getOriginalPlanImages().isEmpty());
    }

    private int size(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private boolean contains(String value, String text) {
        return value != null && value.contains(text);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String enumName(Enumerable<?> value) {
        return value == null ? null : value.getName();
    }

    private void put(Map<String, Object> values, String key, Object value) {
        if (value instanceof String text) {
            if (hasText(text)) values.put(key, text);
        } else if (value != null) {
            values.put(key, value);
        }
    }

    private String digest(String json) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    /** 不对外暴露的上下文构建结果。 */
    public static final class ContextData {
        private final DesignGenerationMode mode;
        private final Map<String, Object> values;
        private final String contextJson;
        private final String sourceDigest;
        private final List<String> missingFields;
        private final List<String> conflictFlags;
        private final String confidence;

        private ContextData(DesignGenerationMode mode, Map<String, Object> values, String contextJson,
                            String sourceDigest, List<String> missingFields, List<String> conflictFlags,
                            String confidence) {
            this.mode = mode;
            this.values = values;
            this.contextJson = contextJson;
            this.sourceDigest = sourceDigest;
            this.missingFields = List.copyOf(missingFields);
            this.conflictFlags = List.copyOf(conflictFlags);
            this.confidence = confidence;
        }

        public DesignGenerationMode getMode() { return mode; }
        public Map<String, Object> getValues() { return values; }
        public String getContextJson() { return contextJson; }
        public String getSourceDigest() { return sourceDigest; }
        public List<String> getMissingFields() { return missingFields; }
        public List<String> getConflictFlags() { return conflictFlags; }
        public String getConfidence() { return confidence; }
    }
}
