package com.th4.edge.decoboon.pack.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageRule;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.model.PackageTemplateItem;
import com.th4.edge.decoboon.pack.model.PackageTemplateSnapshot;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 套餐发布快照生成。 */
@Component
public class PackageSnapshotContext {

    private final ObjectMapper objectMapper;

    public PackageSnapshotContext(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PackageTemplateSnapshot snapshot(PackageTemplate template) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("templateId", template.getTemplateId());
        content.put("code", template.getCode());
        content.put("version", template.getVersion());
        content.put("name", template.getName());
        content.put("area", template.getArea());
        content.put("price", template.getPrice());
        content.put("discount", template.getDiscount());
        content.put("cost", template.getCost());
        content.put("criterions", safeList(template.getCriterions()));
        content.put("lines", lines(template.getItems()));
        content.put("measures", measures(template.getMeasures()));
        try {
            String json = objectMapper.writeValueAsString(content);
            PackageTemplateSnapshot snapshot = new PackageTemplateSnapshot();
            snapshot.setJson(json);
            snapshot.setHash(sha256(json));
            return snapshot;
        } catch (JsonProcessingException ex) {
            throw new DefinedException(new DefaultErrorCode("500", "套餐发布快照生成失败"));
        }
    }

    private List<Map<String, Object>> lines(List<PackageLine> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (items == null) return result;
        for (PackageLine line : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", line.getItemId());
            row.put("sectionId", line.getSection() == null ? null : line.getSection().getGroupId());
            row.put("sectionName", line.getSectionNameSnapshot() != null
                    ? line.getSectionNameSnapshot() : line.getSection() == null ? null : line.getSection().getName());
            row.put("catalogItemId", line.getItem() == null ? null : line.getItem().getItemId());
            row.put("catalogType", line.getCatalogTypeSnapshot() != null
                    ? line.getCatalogTypeSnapshot().toString() : line.getItem() == null ? null : line.getItem().getType().toString());
            row.put("catalogCategory", line.getCatalogCategorySnapshot() != null
                    ? line.getCatalogCategorySnapshot() : line.getItem() == null ? null : line.getItem().getCategory());
            row.put("craft", line.getCraftSnapshot() != null
                    ? line.getCraftSnapshot() : line.getItem() == null ? null : line.getItem().getCraft());
            row.put("costPrice", line.getCostPriceSnapshot() != null
                    ? line.getCostPriceSnapshot() : line.getItem() == null ? null : line.getItem().getCostPrice());
            row.put("name", line.getName());
            row.put("brand", line.getBrand());
            row.put("spec", line.getSpec());
            row.put("unit", line.getUnit() == null ? null : line.getUnit().toString());
            row.put("quantity", line.getQuantity());
            row.put("quantityFactor", line.getQuantityFactor());
            row.put("pricingMethod", line.getPricingMethod() == null ? null : line.getPricingMethod().toString());
            row.put("included", line.getIncluded());
            row.put("deductible", line.getDeductible());
            row.put("sort", line.getSort());
            row.put("criterions", safeList(line.getCriterions()));
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> measures(Map<PackageTemplateItem, PackageRule> measures) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (measures == null) return result;
        for (Map.Entry<PackageTemplateItem, PackageRule> entry : measures.entrySet()) {
            PackageTemplateItem item = entry.getKey();
            PackageRule rule = entry.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", item == null ? null : item.getItemId());
            row.put("name", item == null ? null : item.getName());
            row.put("unit", item == null || item.getUnit() == null ? null : item.getUnit().toString());
            row.put("spaceTypes", item == null || item.getSpaceTypes() == null ? List.of() : item.getSpaceTypes());
            row.put("priority", item == null ? null : item.getPriority());
            row.put("price", rule == null ? null : rule.getPrice());
            row.put("standard", rule == null ? null : rule.getStandard());
            row.put("remark", rule == null ? null : rule.getRemark());
            result.add(row);
        }
        return result;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new DefinedException(new DefaultErrorCode("500", "套餐发布快照哈希生成失败"));
        }
    }
}
