package com.th4.edge.decoboon.document.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.configure.DocumentTemplateScope;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.decoboon.document.repository.DocumentTemplateRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 企业单据模板继承、复制和受控配置。 */
@Component
public class DocumentTemplateContext {

    private static final Set<String> COMMON_FIELDS = Set.of(
            "showCompanyName", "showCompanyAddress", "showServiceHotline");
    /** 凭证（收款单 / 付款单）字段键，与《凭证套打与单据模板字段冻结清单》一致。 */
    private static final Set<String> VOUCHER_FIELDS = Set.of(
            "showVoucherNo", "showVoucherDate", "showPartyName", "showWorkerIdCard", "showWorkerTrade",
            "showWorkerPhone", "showProject", "showAmount", "showAmountWords", "showSignBlock",
            "showDetailNo", "showDetailDate", "showDetailAccount", "showDetailAmount", "showDetailTarget",
            "showDetailCategory", "showDetailRemark");

    private static final Map<DocumentType, Set<String>> ALLOWED_FIELDS = Map.of(
            DocumentType.Quote, Set.of("showPropertyAddress", "showPropertyArea", "showLineBrand", "showLineSpec", "showTerms"),
            DocumentType.Material, Set.of("showProject", "showMaterial", "showQuantity", "showRequiredAt"),
            DocumentType.Receipt, voucherFields("showCustomer", "showReceivedAt"),
            DocumentType.Payment, voucherFields("showPartner", "showPaidAt"),
            DocumentType.Contract, Set.of("showLegalName", "showCustomer", "showSignedAmount", "showPaymentPlan", "showBlankSealPlaceholder")
    );

    /** 凭证允许字段 = 通用凭证键 + 该类型专有键。 */
    private static Set<String> voucherFields(String... specific) {
        java.util.LinkedHashSet<String> fields = new java.util.LinkedHashSet<>(VOUCHER_FIELDS);
        java.util.Collections.addAll(fields, specific);
        return Set.copyOf(fields);
    }
    private static final Set<String> STYLE_KEYS = Set.of("primaryColor", "fontSize", "rowSpacing");

    private final DocumentTemplateRepository templateRepository;
    private final DocumentTemplateBaseline baseline;
    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;

    public DocumentTemplateContext(DocumentTemplateRepository templateRepository,
                                   DocumentTemplateBaseline baseline,
                                   TenantContext tenantContext,
                                   ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.baseline = baseline;
        this.tenantContext = tenantContext;
        this.objectMapper = objectMapper;
    }

    public List<DocumentTemplate> page(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        Long comId = current.getCompany().getComId();
        List<DocumentTemplate> result = new ArrayList<>();
        for (DocumentType type : DocumentType.values()) {
            result.add(template(comId, type));
        }
        return result;
    }

    public DocumentTemplate template(Long comId, DocumentType type) {
        return templateRepository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(
                        comId, type, DocumentStatus.Active)
                .orElseGet(() -> baseline.template(type));
    }

    public DocumentTemplate save(Employee principal, DocumentType type, String name,
                                 String fieldConfig, String styleConfig) {
        // 2026-09-21 用户确认：店长与财务也可维护企业单据模板
        Employee current = tenantContext.requireRole(principal, EmployeeRole.Boss,
                EmployeeRole.StoreManager, EmployeeRole.Finance);
        Long comId = current.getCompany().getComId();
        String normalizedFields = normalizeFields(type, fieldConfig);
        String normalizedStyle = normalizeStyle(styleConfig);
        DocumentTemplate entity = templateRepository
                .findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(comId, type, DocumentStatus.Active)
                .orElseGet(() -> {
                    DocumentTemplate created = new DocumentTemplate();
                    created.setCompany(current.getCompany());
                    created.setType(type);
                    created.setScope(DocumentTemplateScope.Company);
                    created.setVersion(1);
                    created.setBaselineVersion(DocumentTemplateBaseline.VERSION);
                    return created;
                });
        if (entity.getTemplateId() != null) {
            entity.setVersion(entity.getVersion() + 1);
        }
        entity.setName(name == null || name.isBlank() ? baseline.template(type).getName() : name.trim());
        entity.setFieldConfig(normalizedFields);
        entity.setStyleConfig(normalizedStyle);
        entity.setStatus(DocumentStatus.Active);
        return templateRepository.save(entity);
    }

    private String normalizeFields(DocumentType type, String fieldConfig) {
        try {
            JsonNode parsed = objectMapper.readTree(fieldConfig);
            if (!(parsed instanceof ObjectNode objectNode)) throw invalid("字段配置必须是JSON对象");
            Set<String> allowed = new java.util.HashSet<>(COMMON_FIELDS);
            allowed.addAll(ALLOWED_FIELDS.getOrDefault(type, Set.of()));
            Iterator<String> names = objectNode.fieldNames();
            ObjectNode result = objectMapper.createObjectNode();
            while (names.hasNext()) {
                String name = names.next();
                if (!allowed.contains(name)) throw invalid("不支持的字段配置：" + name);
                JsonNode value = objectNode.get(name);
                if (!value.isBoolean()) throw invalid("字段配置必须是布尔值：" + name);
                result.put(name, value.booleanValue());
            }
            for (String name : allowed) {
                if (!result.has(name)) result.put(name, true);
            }
            return objectMapper.writeValueAsString(result);
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("字段配置JSON格式错误");
        }
    }

    private String normalizeStyle(String styleConfig) {
        try {
            JsonNode parsed = objectMapper.readTree(styleConfig);
            if (!(parsed instanceof ObjectNode objectNode)) throw invalid("样式配置必须是JSON对象");
            Iterator<String> names = objectNode.fieldNames();
            ObjectNode result = objectMapper.createObjectNode();
            while (names.hasNext()) {
                String name = names.next();
                if (!STYLE_KEYS.contains(name)) throw invalid("不支持的样式配置：" + name);
                JsonNode value = objectNode.get(name);
                if ("primaryColor".equals(name)) {
                    if (!value.isTextual() || !value.asText().matches("#[0-9A-Fa-f]{6}")) {
                        throw invalid("primaryColor 必须是六位十六进制颜色");
                    }
                    result.put(name, value.asText().toUpperCase());
                } else if ("fontSize".equals(name)) {
                    if (!value.canConvertToInt() || value.asInt() < 9 || value.asInt() > 14) {
                        throw invalid("fontSize 必须在 9 到 14 之间");
                    }
                    result.put(name, value.asInt());
                } else {
                    if (!value.canConvertToInt() || value.asInt() < 0 || value.asInt() > 6) {
                        throw invalid("rowSpacing 必须在 0 到 6 之间");
                    }
                    result.put(name, value.asInt());
                }
            }
            if (!result.has("primaryColor")) result.put("primaryColor", "#0E8A7D");
            if (!result.has("fontSize")) result.put("fontSize", 10);
            if (!result.has("rowSpacing")) result.put("rowSpacing", 2);
            return objectMapper.writeValueAsString(result);
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("样式配置JSON格式错误");
        }
    }

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }
}
