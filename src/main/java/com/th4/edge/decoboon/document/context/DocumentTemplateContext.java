package com.th4.edge.decoboon.document.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.configure.DocumentTemplateScope;
import com.th4.edge.decoboon.document.configure.DocumentTemplateVersionStatus;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.decoboon.document.model.DocumentTemplateVersion;
import com.th4.edge.decoboon.document.repository.DocumentTemplateRepository;
import com.th4.edge.decoboon.document.repository.DocumentTemplateVersionRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
    private static final Set<String> LAYOUT_PAGE_SIZES = Set.of("A3", "A4", "A5");
    private static final Set<String> LAYOUT_ORIENTATIONS = Set.of("portrait", "landscape");
    private static final Set<String> LAYOUT_BLOCKS = Set.of(
            "company", "title", "meta", "amount", "detail", "sign", "footer");

    private final DocumentTemplateRepository templateRepository;
    private final DocumentTemplateVersionRepository versionRepository;
    private final DocumentTemplateBaseline baseline;
    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;

    public DocumentTemplateContext(DocumentTemplateRepository templateRepository,
                                   DocumentTemplateVersionRepository versionRepository,
                                   DocumentTemplateBaseline baseline,
                                   TenantContext tenantContext,
                                   ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.baseline = baseline;
        this.tenantContext = tenantContext;
        this.objectMapper = objectMapper;
    }

    public List<DocumentTemplate> page(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        Long comId = current.getCompany().getComId();
        List<DocumentTemplate> result = new ArrayList<>();
        result.addAll(templateRepository.findByCompany_ComIdAndStatusOrderByTypeAscPresetCodeAscVersionDesc(
                        comId, DocumentStatus.Active).stream()
                .map(item -> withVersions(item, comId))
                .toList());
        for (DocumentType type : DocumentType.values()) {
            if (type == DocumentType.Receipt || type == DocumentType.Payment) {
                baseline.presets().stream()
                        .filter(item -> item.getType() == type)
                        .forEach(result::add);
            } else {
                result.add(baseline.template(type));
            }
        }
        return result;
    }

    public DocumentTemplate template(Long comId, DocumentType type) {
        return templateRepository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(
                        comId, type, DocumentStatus.Active)
                .orElseGet(() -> baseline.template(type));
    }

    public DocumentTemplate template(Long comId, DocumentType type, String presetCode) {
        if (presetCode == null || presetCode.isBlank()) return template(comId, type);
        return templateRepository.findFirstByCompany_ComIdAndTypeAndPresetCodeAndStatusOrderByVersionDesc(
                        comId, type, presetCode.trim(), DocumentStatus.Active)
                .orElseGet(() -> {
                    DocumentTemplate preset = baseline.preset(presetCode.trim());
                    return preset == null ? baseline.template(type) : preset;
                });
    }

    /** 凭证发行使用的模板：优先当前发布版本，无版本记录时兼容旧模板头。 */
    public DocumentTemplate templateForIssue(Long comId, DocumentType type, String presetCode) {
        DocumentTemplate head = (presetCode == null || presetCode.isBlank()
                ? templateRepository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(
                        comId, type, DocumentStatus.Active)
                : templateRepository.findFirstByCompany_ComIdAndTypeAndPresetCodeAndStatusOrderByVersionDesc(
                        comId, type, presetCode.trim(), DocumentStatus.Active))
                .orElse(null);
        if (head == null) {
            DocumentTemplate preset = presetCode == null || presetCode.isBlank() ? null : baseline.preset(presetCode.trim());
            return preset == null ? baseline.template(type) : preset;
        }
        if (head.getPublishedVersion() != null && head.getPublishedVersion() > 0) {
            DocumentTemplateVersion published = versionRepository
                    .findByCompany_ComIdAndTemplate_TemplateIdAndVersion(
                            comId, head.getTemplateId(), head.getPublishedVersion())
                    .orElse(null);
            if (published != null) return fromVersion(published, head);
        }
        return head;
    }

    /** 指定模板版本转发行模板，供补录重渲染保持历史版本。 */
    public DocumentTemplate templateVersion(Long comId, Long templateId, Integer version) {
        DocumentTemplate head = templateRepository.findByCompany_ComIdAndTemplateId(comId, templateId).orElse(null);
        if (head == null || version == null) return head;
        DocumentTemplateVersion snapshot = versionRepository
                .findByCompany_ComIdAndTemplate_TemplateIdAndVersion(comId, templateId, version)
                .orElse(null);
        return snapshot == null ? head : fromVersion(snapshot, head);
    }

    public DocumentTemplate publish(Employee principal, Long templateId) {
        Employee current = requireTemplateMaintainer(principal);
        Long comId = current.getCompany().getComId();
        DocumentTemplate head = requireCompanyTemplate(comId, templateId);
        DocumentTemplateVersion version = versionRepository
                .findByCompany_ComIdAndTemplate_TemplateIdAndVersion(comId, templateId, head.getVersion())
                .orElseGet(() -> snapshot(head, current, DocumentTemplateVersionStatus.Draft).get(0));
        versionRepository.findByCompany_ComIdAndTemplate_TemplateIdAndStatus(
                        comId, templateId, DocumentTemplateVersionStatus.Published)
                .forEach(item -> {
                    if (!item.getVersion().equals(version.getVersion())) {
                        item.setStatus(DocumentTemplateVersionStatus.Superseded);
                        versionRepository.save(item);
                    }
                });
        version.setStatus(DocumentTemplateVersionStatus.Published);
        version.setPublishedBy(current);
        version.setPublishedAt(LocalDateTime.now());
        versionRepository.save(version);
        head.setPublishedVersion(version.getVersion());
        head.setPublishedBy(current);
        head.setPublishedAt(version.getPublishedAt());
        head.setStatus(DocumentStatus.Active);
        templateRepository.save(head);
        return withVersions(head, comId);
    }

    public DocumentTemplate disable(Employee principal, Long templateId) {
        Employee current = requireTemplateMaintainer(principal);
        Long comId = current.getCompany().getComId();
        DocumentTemplate head = requireCompanyTemplate(comId, templateId);
        versionRepository.findFirstByCompany_ComIdAndTemplate_TemplateIdAndStatusOrderByVersionDesc(
                        comId, templateId, DocumentTemplateVersionStatus.Published)
                .ifPresent(version -> {
                    version.setStatus(DocumentTemplateVersionStatus.Disabled);
                    versionRepository.save(version);
                });
        head.setPublishedVersion(0);
        templateRepository.save(head);
        return withVersions(head, comId);
    }

    public List<DocumentTemplateVersion> versions(Employee principal, Long templateId) {
        Employee current = requireTemplateMaintainer(principal);
        Long comId = current.getCompany().getComId();
        requireCompanyTemplate(comId, templateId);
        return versionRepository.findByCompany_ComIdAndTemplate_TemplateIdOrderByVersionDesc(comId, templateId);
    }

    public DocumentTemplate save(Employee principal, DocumentType type, String name,
                                 String fieldConfig, String styleConfig) {
        return save(principal, type, null, name, fieldConfig, styleConfig, null, null);
    }

    public DocumentTemplate save(Employee principal, DocumentType type, String presetCode, String name,
                                 String fieldConfig, String styleConfig) {
        return save(principal, type, presetCode, name, fieldConfig, styleConfig, null, null);
    }

    public DocumentTemplate save(Employee principal, DocumentType type, String presetCode, String name,
                                 String fieldConfig, String styleConfig, String layoutConfig) {
        return save(principal, type, presetCode, name, fieldConfig, styleConfig, layoutConfig, null);
    }

    public DocumentTemplate save(Employee principal, DocumentType type, String presetCode, String name,
                                 String fieldConfig, String styleConfig, String layoutConfig, String designerConfig) {
        // 2026-09-21 用户确认：店长与财务也可维护企业单据模板
        Employee current = tenantContext.requireRole(principal, EmployeeRole.Boss,
                EmployeeRole.StoreManager, EmployeeRole.Finance);
        Long comId = current.getCompany().getComId();
        String normalizedPreset = presetCode == null || presetCode.isBlank() ? null : presetCode.trim();
        String normalizedFields = normalizeFields(type, fieldConfig);
        String normalizedStyle = normalizeStyle(styleConfig);
        String normalizedLayout = normalizeLayout(layoutConfig);
        String normalizedDesigner = normalizeDesignerConfig(designerConfig);
        DocumentTemplate entity = (normalizedPreset == null
                ? templateRepository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(
                        comId, type, DocumentStatus.Active)
                : templateRepository.findFirstByCompany_ComIdAndTypeAndPresetCodeAndStatusOrderByVersionDesc(
                        comId, type, normalizedPreset, DocumentStatus.Active))
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
        DocumentTemplate source = normalizedPreset == null ? baseline.template(type) : baseline.preset(normalizedPreset);
        if (source == null) source = baseline.template(type);
        entity.setPresetCode(normalizedPreset);
        entity.setName(name == null || name.isBlank() ? source.getName() : name.trim());
        entity.setFieldConfig(normalizedFields);
        entity.setStyleConfig(normalizedStyle);
        entity.setLayoutConfig(normalizedLayout);
        entity.setDesignerConfig(normalizedDesigner);
        entity.setStatus(DocumentStatus.Active);
        DocumentTemplate saved = templateRepository.save(entity);
        boolean firstPublish = saved.getPublishedVersion() == null || saved.getPublishedVersion() == 0;
        DocumentTemplateVersion version = snapshot(saved, current,
                firstPublish ? DocumentTemplateVersionStatus.Published : DocumentTemplateVersionStatus.Draft).get(0);
        if (firstPublish) {
            version.setPublishedBy(current);
            version.setPublishedAt(LocalDateTime.now());
            versionRepository.save(version);
            saved.setPublishedVersion(saved.getVersion());
            saved.setPublishedBy(current);
            saved.setPublishedAt(version.getPublishedAt());
            saved = templateRepository.save(saved);
        }
        return withVersions(saved, comId);
    }

    private List<DocumentTemplateVersion> snapshot(DocumentTemplate template, Employee current,
                                                   DocumentTemplateVersionStatus status) {
        Long comId = current.getCompany().getComId();
        DocumentTemplateVersion version = versionRepository
                .findByCompany_ComIdAndTemplate_TemplateIdAndVersion(
                        comId, template.getTemplateId(), template.getVersion())
                .orElseGet(DocumentTemplateVersion::new);
        version.setCompany(current.getCompany());
        version.setTemplate(template);
        version.setVersion(template.getVersion());
        version.setName(template.getName());
        version.setPresetCode(template.getPresetCode());
        version.setBaselineVersion(template.getBaselineVersion());
        version.setFieldConfig(template.getFieldConfig());
        version.setStyleConfig(template.getStyleConfig());
        version.setLayoutConfig(template.getLayoutConfig());
        version.setDesignerConfig(template.getDesignerConfig());
        version.setStatus(status);
        return List.of(versionRepository.save(version));
    }

    private DocumentTemplate fromVersion(DocumentTemplateVersion version, DocumentTemplate head) {
        DocumentTemplate view = new DocumentTemplate();
        view.setTemplateId(head.getTemplateId());
        view.setCompany(head.getCompany());
        view.setType(head.getType());
        view.setScope(DocumentTemplateScope.Company);
        view.setPresetCode(version.getPresetCode());
        view.setPresetNote(head.getPresetNote());
        view.setName(version.getName());
        view.setVersion(version.getVersion());
        view.setBaselineVersion(version.getBaselineVersion());
        view.setFieldConfig(version.getFieldConfig());
        view.setStyleConfig(version.getStyleConfig());
        view.setLayoutConfig(version.getLayoutConfig());
        view.setDesignerConfig(version.getDesignerConfig());
        view.setStatus(DocumentStatus.Active);
        view.setPublishedVersion(head.getPublishedVersion());
        view.setPublishedAt(head.getPublishedAt());
        view.setPublishedBy(head.getPublishedBy());
        return view;
    }

    private DocumentTemplate withVersions(DocumentTemplate template, Long comId) {
        if (template == null || template.getTemplateId() == null) return template;
        template.setVersions(versionRepository.findByCompany_ComIdAndTemplate_TemplateIdOrderByVersionDesc(
                comId, template.getTemplateId()));
        return template;
    }

    private Employee requireTemplateMaintainer(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss,
                EmployeeRole.StoreManager, EmployeeRole.Finance);
    }

    private DocumentTemplate requireCompanyTemplate(Long comId, Long templateId) {
        if (templateId == null) throw invalid("模板索引不能为空");
        return templateRepository.findByCompany_ComIdAndTemplateId(comId, templateId)
                .orElseThrow(() -> invalid("模板不存在或无权访问"));
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

    private String normalizeDesignerConfig(String designerConfig) {
        try {
            JsonNode parsed = objectMapper.readTree(designerConfig == null || designerConfig.isBlank()
                    ? DocumentDesignerDefaults.JSON : designerConfig);
            if (!(parsed instanceof ObjectNode root)) throw invalid("设计器配置必须是JSON对象");
            ObjectNode result = objectMapper.createObjectNode();
            result.put("schemaVersion", Math.max(1, root.path("schemaVersion").asInt(1)));

            JsonNode pageNode = root.path("page");
            ObjectNode page = objectMapper.createObjectNode();
            page.put("widthMm", clamp(pageNode.path("widthMm").asDouble(210), 50, 500));
            page.put("heightMm", clamp(pageNode.path("heightMm").asDouble(297), 50, 500));
            String orientation = pageNode.path("orientation").asText("portrait").toLowerCase();
            page.put("orientation", Set.of("portrait", "landscape").contains(orientation) ? orientation : "portrait");
            page.put("marginMm", clamp(pageNode.path("marginMm").asDouble(12), 0, 50));
            page.put("gridSizeMm", clamp(pageNode.path("gridSizeMm").asDouble(5), 1, 20));
            result.set("page", page);

            result.set("elements", normalizeDesignerElements(root.path("elements")));
            result.set("tables", normalizeDesignerTables(root.path("tables")));

            JsonNode printNode = root.path("print");
            ObjectNode print = objectMapper.createObjectNode();
            print.put("scale", clamp(printNode.path("scale").asDouble(100), 50, 200));
            print.put("offsetXMm", clamp(printNode.path("offsetXMm").asDouble(0), -20, 20));
            print.put("offsetYMm", clamp(printNode.path("offsetYMm").asDouble(0), -20, 20));
            result.set("print", print);
            return objectMapper.writeValueAsString(result);
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("设计器配置JSON格式错误");
        }
    }

    private com.fasterxml.jackson.databind.node.ArrayNode normalizeDesignerElements(JsonNode source) {
        var result = objectMapper.createArrayNode();
        if (!source.isArray()) return result;
        if (source.size() > 200) throw invalid("设计器元素不能超过200个");
        int index = 0;
        for (JsonNode item : source) {
            if (!(item instanceof ObjectNode sourceNode)) continue;
            String id = sourceNode.path("id").asText("").trim();
            String type = sourceNode.path("type").asText("").trim();
            if (id.isBlank() || type.isBlank()) continue;
            ObjectNode element = objectMapper.createObjectNode();
            element.put("id", id.length() > 64 ? id.substring(0, 64) : id);
            element.put("type", type.length() > 32 ? type.substring(0, 32) : type);
            element.put("xMm", clamp(sourceNode.path("xMm").asDouble(0), 0, 1000));
            element.put("yMm", clamp(sourceNode.path("yMm").asDouble(0), 0, 1000));
            element.put("widthMm", clamp(sourceNode.path("widthMm").asDouble(40), 1, 1000));
            element.put("heightMm", clamp(sourceNode.path("heightMm").asDouble(8), 1, 1000));
            element.put("zIndex", Math.max(0, Math.min(sourceNode.path("zIndex").asInt(index), 9999)));
            String binding = sourceNode.path("binding").asText("").trim();
            if (!binding.isBlank()) element.put("binding", binding.length() > 100 ? binding.substring(0, 100) : binding);
            String fixedText = sourceNode.path("fixedText").asText("").trim();
            if (!fixedText.isBlank()) element.put("fixedText", fixedText.length() > 500 ? fixedText.substring(0, 500) : fixedText);
            JsonNode style = sourceNode.path("style");
            element.set("style", style.isObject() ? style.deepCopy() : objectMapper.createObjectNode());
            JsonNode border = sourceNode.path("border");
            element.set("border", border.isObject() ? border.deepCopy() : objectMapper.createObjectNode());
            result.add(element);
            index++;
        }
        return result;
    }

    private com.fasterxml.jackson.databind.node.ArrayNode normalizeDesignerTables(JsonNode source) {
        var result = objectMapper.createArrayNode();
        if (!source.isArray()) return result;
        if (source.size() > 20) throw invalid("设计器表格不能超过20个");
        for (JsonNode item : source) {
            if (!(item instanceof ObjectNode sourceNode)) continue;
            String id = sourceNode.path("id").asText("").trim();
            if (id.isBlank()) continue;
            ObjectNode table = objectMapper.createObjectNode();
            table.put("id", id.length() > 64 ? id.substring(0, 64) : id);
            table.put("binding", sourceNode.path("binding").asText("voucher.items"));
            table.put("xMm", clamp(sourceNode.path("xMm").asDouble(0), 0, 1000));
            table.put("yMm", clamp(sourceNode.path("yMm").asDouble(0), 0, 1000));
            table.put("widthMm", clamp(sourceNode.path("widthMm").asDouble(180), 10, 1000));
            table.put("heightMm", clamp(sourceNode.path("heightMm").asDouble(80), 10, 1000));
            table.put("headerRepeat", sourceNode.path("headerRepeat").asBoolean(true));
            table.put("rowsPerPage", Math.max(1, Math.min(sourceNode.path("rowsPerPage").asInt(10), 50)));
            table.put("rowHeightMm", clamp(sourceNode.path("rowHeightMm").asDouble(8), 4, 40));
            var columns = objectMapper.createArrayNode();
            JsonNode columnSource = sourceNode.path("columns");
            if (columnSource.isArray()) {
                for (JsonNode columnNode : columnSource) {
                    if (columns.size() >= 30) break;
                    ObjectNode column = objectMapper.createObjectNode();
                    column.put("key", columnNode.path("key").asText(""));
                    column.put("label", columnNode.path("label").asText(""));
                    column.put("widthMm", clamp(columnNode.path("widthMm").asDouble(20), 5, 200));
                    String align = columnNode.path("align").asText("left");
                    column.put("align", Set.of("left", "center", "right").contains(align) ? align : "left");
                    columns.add(column);
                }
            }
            table.set("columns", columns);
            result.add(table);
        }
        return result;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private String normalizeLayout(String layoutConfig) {
        try {
            JsonNode parsed = objectMapper.readTree(layoutConfig == null || layoutConfig.isBlank()
                    ? DocumentLayoutDefaults.JSON : layoutConfig);
            if (!(parsed instanceof ObjectNode objectNode)) throw invalid("版式配置必须是JSON对象");
            ObjectNode result = objectMapper.createObjectNode();
            String pageSize = objectNode.path("pageSize").asText("A4").toUpperCase();
            if (!LAYOUT_PAGE_SIZES.contains(pageSize)) throw invalid("不支持的纸张大小");
            result.put("pageSize", pageSize);
            String orientation = objectNode.path("orientation").asText("portrait").toLowerCase();
            if (!LAYOUT_ORIENTATIONS.contains(orientation)) throw invalid("不支持的纸张方向");
            result.put("orientation", orientation);
            int margin = objectNode.path("marginMm").asInt(12);
            if (margin < 8 || margin > 20) throw invalid("页边距必须在8到20毫米之间");
            result.put("marginMm", margin);

            var blocks = objectMapper.createArrayNode();
            var incoming = objectMapper.createObjectNode();
            if (objectNode.path("blocks").isArray()) {
                for (JsonNode item : objectNode.path("blocks")) {
                    String key = item.path("key").asText("");
                    if (!LAYOUT_BLOCKS.contains(key)) throw invalid("不支持的版式区块：" + key);
                    incoming.set(key, item);
                }
            }
            int defaultSort = 10;
            for (String key : List.of("company", "title", "meta", "amount", "detail", "sign", "footer")) {
                JsonNode item = incoming.path(key);
                ObjectNode block = objectMapper.createObjectNode();
                block.put("key", key);
                block.put("visible", item.has("visible") ? item.path("visible").asBoolean(true) : true);
                int sort = item.has("sort") ? item.path("sort").asInt(defaultSort) : defaultSort;
                if (sort < 1 || sort > 999) throw invalid("版式区块排序必须在1到999之间");
                block.put("sort", sort);
                blocks.add(block);
                defaultSort += 10;
            }
            result.set("blocks", blocks);

            var signatures = objectMapper.createArrayNode();
            JsonNode signatureNode = objectNode.path("signatures");
            if (signatureNode.isArray()) {
                if (signatureNode.size() > 4) throw invalid("签字栏最多4项");
                for (JsonNode item : signatureNode) {
                    String label = item.asText("").trim();
                    if (!label.isBlank()) {
                        if (label.length() > 20) throw invalid("签字栏名称不能超过20个字");
                        signatures.add(label);
                    }
                }
            }
            if (signatures.isEmpty()) {
                signatures.add("制单");
                signatures.add("审核");
                signatures.add("领款/付款人");
            }
            result.set("signatures", signatures);
            String footer = objectNode.path("footer").asText("").trim();
            if (footer.length() > 200) throw invalid("页脚说明不能超过200个字");
            if (!footer.isBlank()) result.put("footer", footer);
            return objectMapper.writeValueAsString(result);
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("版式配置JSON格式错误");
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
