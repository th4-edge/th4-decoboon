package com.th4.edge.decoboon.catalog.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.catalog.configure.CatalogRevisionType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.model.CatalogItemRevision;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRevisionRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 报价项目 revision 与不可变审计。 */
@Component
public class CatalogRevisionContext {

    private final CatalogItemRevisionRepository repository;
    private final ObjectMapper objectMapper;

    public CatalogRevisionContext(CatalogItemRevisionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public int nextRevision(CatalogItem item) {
        int revision = item.getRevision() == null || item.getRevision() < 1 ? 1 : item.getRevision() + 1;
        item.setRevision(revision);
        return revision;
    }

    /** 判断两次状态是否包含实质业务变化；修订号、排序和来源类型不单独触发修订。 */
    public boolean effectiveChange(CatalogItem before, CatalogItem after) {
        if (before == null || after == null) return true;
        return !fingerprint(before).equals(fingerprint(after));
    }

    /** 计算用于比较的业务值摘要，不落库。 */
    public String fingerprint(CatalogItem item) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("type", item.getType() == null ? null : item.getType().toString());
        content.put("code", item.getCode());
        content.put("category", item.getCategory());
        content.put("brand", item.getBrand());
        content.put("series", item.getSeries());
        content.put("spec", item.getSpec());
        content.put("name", item.getName());
        content.put("unit", item.getUnit() == null ? null : item.getUnit().toString());
        content.put("pricingMode", item.getPricingMode() == null ? null : item.getPricingMode().toString());
        content.put("materialPrice", canonicalMoney(item.getMaterialPrice()));
        content.put("manualPrice", canonicalMoney(item.getManualPrice()));
        content.put("fixedTotalPrice", canonicalMoney(item.getFixedTotalPrice()));
        content.put("costPrice", canonicalMoney(item.getCostPrice()));
        content.put("formula", item.getFormula());
        content.put("craft", item.getCraft());
        content.put("criterion", item.getCriterion());
        content.put("description", item.getDescription());
        content.put("images", item.getImages() == null ? List.of() : List.copyOf(item.getImages()));
        content.put("active", item.getActive());
        content.put("durationDays", item.getDurationDays());
        content.put("lossRate", canonicalMoney(item.getLossRate()));
        content.put("minimumOrder", canonicalMoney(item.getMinimumOrder()));
        content.put("leadTimeDays", item.getLeadTimeDays());
        content.put("warrantyMonths", item.getWarrantyMonths());
        content.put("board", item.getBoard());
        content.put("countertop", item.getCountertop());
        return sha256(writeValue(content));
    }

    public CatalogItemRevision record(CatalogItem item, Employee employee, CatalogRevisionType changeType) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("itemId", item.getItemId());
        content.put("type", item.getType() == null ? null : item.getType().toString());
        content.put("code", item.getCode());
        content.put("category", item.getCategory());
        content.put("brand", item.getBrand());
        content.put("series", item.getSeries());
        content.put("spec", item.getSpec());
        content.put("name", item.getName());
        content.put("unit", item.getUnit() == null ? null : item.getUnit().toString());
        content.put("pricingMode", item.getPricingMode() == null ? null : item.getPricingMode().toString());
        content.put("materialPrice", item.getMaterialPrice());
        content.put("manualPrice", item.getManualPrice());
        content.put("fixedTotalPrice", item.getFixedTotalPrice());
        content.put("costPrice", item.getCostPrice());
        content.put("formula", item.getFormula());
        content.put("craft", item.getCraft());
        content.put("criterion", item.getCriterion());
        content.put("description", item.getDescription());
        content.put("images", item.getImages() == null ? List.of() : List.copyOf(item.getImages()));
        content.put("active", item.getActive());
        content.put("sort", item.getSort());
        content.put("sourceType", item.getSourceType() == null ? null : item.getSourceType().toString());
        content.put("durationDays", item.getDurationDays());
        content.put("lossRate", item.getLossRate());
        content.put("minimumOrder", item.getMinimumOrder());
        content.put("leadTimeDays", item.getLeadTimeDays());
        content.put("warrantyMonths", item.getWarrantyMonths());
        content.put("board", item.getBoard());
        content.put("countertop", item.getCountertop());
        content.put("revision", item.getRevision());
        String json = writeValue(content);
        CatalogItemRevision audit = new CatalogItemRevision();
        audit.setCompany(item.getCompany());
        audit.setItemId(item.getItemId());
        audit.setItemCode(item.getCode());
        audit.setRevision(item.getRevision());
        audit.setChangeType(changeType);
        audit.setSnapshotJson(json);
        audit.setSnapshotHash(sha256(json));
        audit.setChangedBy(employee);
        audit.setChangedAt(LocalDateTime.now());
        return repository.save(audit);
    }

    private String canonicalMoney(java.math.BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new DefinedException(new DefaultErrorCode("500", "报价项目修订快照生成失败"));
        }
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new DefinedException(new DefaultErrorCode("500", "报价项目修订哈希生成失败"));
        }
    }
}
