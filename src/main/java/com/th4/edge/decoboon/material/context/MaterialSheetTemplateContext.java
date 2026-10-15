package com.th4.edge.decoboon.material.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.material.configure.SheetTemplateScope;
import com.th4.edge.decoboon.material.configure.SheetTemplateStatus;
import com.th4.edge.decoboon.material.model.MaterialSheetTemplate;
import com.th4.edge.decoboon.material.repository.MaterialSheetTemplateRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 企业主材下单单据模板：复制平台行业基线 + 受控个性化（BR-044）。
 *
 * <p><b>个性化只碰呈现与编排</b>：字段显隐/必填/顺序/别名、默认值与值域、行骨架、签名位、编号规则可改；
 * 事实、口径、状态与权限为红线。字段键只能取平台登记的白名单，**不允许企业自定义新键**
 * （否则作业单行会与事实层失配）。</p>
 *
 * <p>平台基线不落企业表：以瞬态副本返回（与 {@code DocumentTemplate} 一致）。</p>
 */
@Component
public class MaterialSheetTemplateContext {

    /** 允许配置的单据头字段键（白名单）。 */
    public static final Set<String> HEADER_KEYS = Set.of(
            "manufacturerName", "supplier", "measureAt", "groupChatAt", "deliverAt", "installAt", "remark");

    /** 允许配置的作业单行字段键（白名单）。 */
    public static final Set<String> LINE_KEYS = Set.of(
            "locationName", "spaceName", "materialName", "brand", "model", "spec", "color", "size", "craft", "glass",
            "quantity", "unit", "standardIncluded", "remark");

    /** 允许配置的默认规则键（白名单）。 */
    public static final Set<String> RULE_KEYS = Set.of(
            "realAreaIncludingReturn", "lossRequired", "defaultUnit", "unitConversions", "backfill", "returnGoods", "defaults");

    /** 允许配置的签名位（白名单）。 */
    public static final Set<String> SIGNATURE_KEYS = Set.of("pm", "designer", "material", "customer");

    /** 数量口径红线：任何模板配置都不得改写（BR-044 §四）。 */
    private static final Set<String> FORBIDDEN_RULE_KEYS = Set.of(
            "lossRate", "maxQuantity", "overOrder", "price", "cost", "settlementPrice", "permission");

    private final MaterialSheetTemplateRepository templateRepository;
    private final MaterialSheetBaseline baseline;
    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;

    public MaterialSheetTemplateContext(MaterialSheetTemplateRepository templateRepository,
                                        MaterialSheetBaseline baseline,
                                        TenantContext tenantContext,
                                        ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.baseline = baseline;
        this.tenantContext = tenantContext;
        this.objectMapper = objectMapper;
    }

    /** 平台可选行业基线（build 用于让企业选择复制哪一类）。 */
    public List<MaterialSheetBaseline.Preset> baselines() {
        return baseline.presets();
    }

    /** 模板列表：企业自定义模板 + 平台行业基线瞬态副本。 */
    public List<MaterialSheetTemplate> page(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        List<MaterialSheetTemplate> result = new ArrayList<>(
                templateRepository.findByCompany_ComIdOrderBySheetTypeAscVersionDesc(current.getCompany().getComId()));
        for (MaterialSheetBaseline.Preset preset : baseline.presets()) {
            result.add(toPlatformTemplate(current, preset));
        }
        return result;
    }

    /** 企业模板详情（平台基线按编码取瞬态副本，不落库）。 */
    public MaterialSheetTemplate info(Employee principal, Long templateId, String sheetType) {
        Employee current = tenantContext.requireEmployee(principal);
        if (templateId != null) {
            return templateRepository.findFirstBySheetTemplateIdAndCompany_ComId(templateId, current.getCompany().getComId())
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模板不存在或无权访问")));
        }
        if (sheetType == null || sheetType.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少模板索引或单据类型"));
        }
        MaterialSheetBaseline.Preset preset = baseline.find(sheetType.trim());
        if (preset == null) {
            throw new DefinedException(new DefaultErrorCode("404", "平台未提供该品类的行业基线"));
        }
        return toPlatformTemplate(current, preset);
    }

    /**
     * 下单取用模板：优先企业已发布版本，其次企业最新版本，最后回退平台基线。
     *
     * <p>返回值必须由调用方**冻结版本号**写入作业单，模板后续升级不回溯改变历史单据。</p>
     */
    public MaterialSheetTemplate templateForIssue(Long comId, String sheetType) {
        MaterialSheetTemplate published = templateRepository
                .findFirstByCompany_ComIdAndSheetTypeAndPublishedVersionGreaterThanOrderByPublishedVersionDesc(comId, sheetType, 0)
                .orElse(null);
        if (published != null) return published;
        List<MaterialSheetTemplate> companyTemplates =
                templateRepository.findByCompany_ComIdAndSheetTypeOrderByVersionDesc(comId, sheetType);
        if (!companyTemplates.isEmpty()) return companyTemplates.get(0);
        MaterialSheetBaseline.Preset preset = baseline.find(sheetType);
        return preset == null ? null : toTransient(preset);
    }

    /** 复制平台基线或新建企业模板。 */
    public MaterialSheetTemplate save(Employee principal, String sheetType, String baselineCode, String name,
                                      String fieldConfig, String rowTemplateConfig, String ruleConfig,
                                      String signatureConfig, String numberRule) {
        Employee current = requireConfigurator(principal);
        if (sheetType == null || sheetType.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少单据类型"));
        }
        String type = sheetType.trim();
        MaterialSheetBaseline.Preset preset = baseline.find(baselineCode == null || baselineCode.isBlank() ? type : baselineCode.trim());
        MaterialSheetTemplate target = new MaterialSheetTemplate();
        target.setCompany(current.getCompany());
        target.setSheetType(type);
        target.setScope(SheetTemplateScope.Company);
        target.setBaselineVersion(preset == null ? MaterialSheetBaseline.VERSION : MaterialSheetBaseline.VERSION);
        target.setVersion(nextVersion(current.getCompany().getComId(), type));
        target.setPublishedVersion(0);
        target.setStatus(SheetTemplateStatus.Active);
        target.setName(name == null || name.isBlank() ? (preset == null ? type : preset.name()) : name.trim());
        target.setFieldConfig(fieldConfig == null || fieldConfig.isBlank()
                ? (preset == null ? fallbackFields() : preset.fieldConfig()) : validateFields(fieldConfig));
        target.setRowTemplateConfig(rowTemplateConfig == null || rowTemplateConfig.isBlank()
                ? (preset == null ? "[]" : preset.rowTemplateConfig()) : validateRowTemplate(rowTemplateConfig));
        target.setRuleConfig(ruleConfig == null || ruleConfig.isBlank()
                ? (preset == null ? "{}" : preset.ruleConfig()) : validateRules(ruleConfig));
        target.setSignatureConfig(signatureConfig == null || signatureConfig.isBlank()
                ? (preset == null ? "{}" : preset.signatureConfig()) : validateSignatures(signatureConfig));
        target.setNumberRule(numberRule == null || numberRule.isBlank()
                ? (preset == null ? "{type}-{yyyyMMdd}-{seq}" : preset.numberRule()) : numberRule.trim());
        target.setCreatedBy(current);
        return templateRepository.save(target);
    }

    /** 更新企业模板（平台基线不可改）。 */
    public MaterialSheetTemplate update(Employee principal, Long templateId, String name, String fieldConfig,
                                        String rowTemplateConfig, String ruleConfig, String signatureConfig,
                                        String numberRule) {
        Employee current = requireConfigurator(principal);
        MaterialSheetTemplate target = requireCompanyTemplate(current, templateId);
        if (name != null && !name.isBlank()) target.setName(name.trim());
        if (fieldConfig != null && !fieldConfig.isBlank()) target.setFieldConfig(validateFields(fieldConfig));
        if (rowTemplateConfig != null && !rowTemplateConfig.isBlank()) target.setRowTemplateConfig(validateRowTemplate(rowTemplateConfig));
        if (ruleConfig != null && !ruleConfig.isBlank()) target.setRuleConfig(validateRules(ruleConfig));
        if (signatureConfig != null && !signatureConfig.isBlank()) target.setSignatureConfig(validateSignatures(signatureConfig));
        if (numberRule != null && !numberRule.isBlank()) target.setNumberRule(numberRule.trim());
        return templateRepository.save(target);
    }

    /** 发布模板版本：publishedVersion = version（作业单生成时冻结该版本）。 */
    public MaterialSheetTemplate publish(Employee principal, Long templateId) {
        Employee current = requireConfigurator(principal);
        MaterialSheetTemplate target = requireCompanyTemplate(current, templateId);
        target.setPublishedVersion(target.getVersion());
        target.setStatus(SheetTemplateStatus.Active);
        return templateRepository.save(target);
    }

    /** 停用模板（不禁用平台基线；已生成的作业单不受影响）。 */
    public MaterialSheetTemplate disable(Employee principal, Long templateId) {
        Employee current = requireConfigurator(principal);
        MaterialSheetTemplate target = requireCompanyTemplate(current, templateId);
        target.setStatus(SheetTemplateStatus.Disabled);
        return templateRepository.save(target);
    }

    /** 删除企业模板（已生成的作业单按冻结版本呈现，不受影响）。 */
    public void delete(Employee principal, Long templateId) {
        Employee current = requireConfigurator(principal);
        templateRepository.delete(requireCompanyTemplate(current, templateId));
    }

    // ------------------------------------------------------------------ 内部

    private Employee requireConfigurator(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss, EmployeeRole.StoreManager);
    }

    private MaterialSheetTemplate requireCompanyTemplate(Employee current, Long templateId) {
        if (templateId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少模板索引"));
        }
        MaterialSheetTemplate target = templateRepository
                .findFirstBySheetTemplateIdAndCompany_ComId(templateId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模板不存在或无权访问")));
        if (target.getScope() == SheetTemplateScope.Platform) {
            throw new DefinedException(new DefaultErrorCode("403", "平台行业基线只读，请先复制为企业模板"));
        }
        return target;
    }

    private int nextVersion(Long comId, String sheetType) {
        return templateRepository.findByCompany_ComIdAndSheetTypeOrderByVersionDesc(comId, sheetType).stream()
                .map(MaterialSheetTemplate::getVersion)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private MaterialSheetTemplate toPlatformTemplate(Employee current, MaterialSheetBaseline.Preset preset) {
        MaterialSheetTemplate transientTemplate = toTransient(preset);
        transientTemplate.setCompany(current.getCompany());
        return transientTemplate;
    }

    /** 平台基线瞬态副本（templateId 为空，前端据 scope=Platform 判定只读）。 */
    private MaterialSheetTemplate toTransient(MaterialSheetBaseline.Preset preset) {
        MaterialSheetTemplate transientTemplate = new MaterialSheetTemplate();
        transientTemplate.setSheetType(preset.code());
        transientTemplate.setName(preset.name());
        transientTemplate.setScope(SheetTemplateScope.Platform);
        transientTemplate.setBaselineVersion(MaterialSheetBaseline.VERSION);
        transientTemplate.setVersion(MaterialSheetBaseline.VERSION);
        transientTemplate.setPublishedVersion(MaterialSheetBaseline.VERSION);
        transientTemplate.setStatus(SheetTemplateStatus.Active);
        transientTemplate.setFieldConfig(preset.fieldConfig());
        transientTemplate.setRowTemplateConfig(preset.rowTemplateConfig());
        transientTemplate.setRuleConfig(preset.ruleConfig());
        transientTemplate.setSignatureConfig(preset.signatureConfig());
        transientTemplate.setNumberRule(preset.numberRule());
        return transientTemplate;
    }

    private String fallbackFields() {
        return "{\"version\":1,\"header\":[],\"line\":[]}";
    }

    /** 校验字段配置：只允许白名单键，禁止自定义新键（BR-044 §四）。 */
    public String validateFields(String fieldConfig) {
        JsonNode root = readTree(fieldConfig, "字段配置");
        validateFieldArray(root.get("header"), HEADER_KEYS, "header");
        validateFieldArray(root.get("line"), LINE_KEYS, "line");
        return fieldConfig.trim();
    }

    private void validateFieldArray(JsonNode array, Set<String> allowed, String section) {
        if (array == null || array.isNull()) return;
        if (!array.isArray()) {
            throw new DefinedException(new DefaultErrorCode("400", "字段配置 " + section + " 必须是数组"));
        }
        LinkedHashSet<String> unknown = new LinkedHashSet<>();
        for (JsonNode node : array) {
            String key = node.path("key").asText("");
            if (key.isBlank()) {
                throw new DefinedException(new DefaultErrorCode("400", "字段配置 " + section + " 存在缺少 key 的项"));
            }
            if (!allowed.contains(key)) unknown.add(key);
        }
        if (!unknown.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "字段键不在平台白名单内，不允许新增自定义字段：" + String.join("、", unknown)));
        }
    }

    /** 校验标准行骨架：字符串数组，逐项非空。 */
    public String validateRowTemplate(String rowTemplateConfig) {
        JsonNode root = readTree(rowTemplateConfig, "行骨架");
        if (!root.isArray()) {
            throw new DefinedException(new DefaultErrorCode("400", "行骨架必须是字符串数组"));
        }
        for (JsonNode node : root) {
            if (node.asText("").isBlank()) {
                throw new DefinedException(new DefaultErrorCode("400", "行骨架存在空行"));
            }
        }
        return rowTemplateConfig.trim();
    }

    /** 校验默认规则：只允许白名单键；数量/价格/成本/权限口径为红线。 */
    public String validateRules(String ruleConfig) {
        JsonNode root = readTree(ruleConfig, "默认规则");
        if (!root.isObject()) {
            throw new DefinedException(new DefaultErrorCode("400", "默认规则必须是对象"));
        }
        Iterator<String> names = root.fieldNames();
        while (names.hasNext()) {
            String key = names.next();
            if (FORBIDDEN_RULE_KEYS.contains(key)) {
                throw new DefinedException(new DefaultErrorCode("403", "该配置属于事实口径红线，不允许通过模板改写：" + key));
            }
            if (!RULE_KEYS.contains(key)) {
                throw new DefinedException(new DefaultErrorCode("400", "默认规则键不在平台白名单内：" + key));
            }
        }
        return ruleConfig.trim();
    }

    /** 校验签名位：只允许白名单签名位。 */
    public String validateSignatures(String signatureConfig) {
        JsonNode root = readTree(signatureConfig, "签名位");
        if (!root.isObject()) {
            throw new DefinedException(new DefaultErrorCode("400", "签名位必须是对象"));
        }
        Iterator<String> names = root.fieldNames();
        while (names.hasNext()) {
            String key = names.next();
            if (!SIGNATURE_KEYS.contains(key)) {
                throw new DefinedException(new DefaultErrorCode("400", "签名位不在平台白名单内：" + key));
            }
        }
        return signatureConfig.trim();
    }

    private JsonNode readTree(String json, String label) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("400", label + "不是合法 JSON"));
        }
    }
}
