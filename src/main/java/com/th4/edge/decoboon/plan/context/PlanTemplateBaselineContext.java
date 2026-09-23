package com.th4.edge.decoboon.plan.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.plan.configure.PaymentTriggerType;
import com.th4.edge.decoboon.plan.configure.StageAcceptanceMode;
import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.ConstructionStageTemplate;
import com.th4.edge.decoboon.plan.model.PaymentPlanNodeTemplate;
import com.th4.edge.decoboon.plan.model.PaymentPlanTemplate;
import com.th4.edge.decoboon.plan.model.PlanTemplateCatalog;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 平台计划模板基线加载与查询。 */
@Component
public class PlanTemplateBaselineContext {

    private final ObjectMapper objectMapper;
    private volatile PlanTemplateCatalog catalog;

    public PlanTemplateBaselineContext(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        try (InputStream payment = new ClassPathResource("baselines/payment-plan-baselines.json").getInputStream();
             InputStream construction = new ClassPathResource("baselines/construction-plan-baselines.json").getInputStream()) {
            JsonNode paymentRoot = objectMapper.readTree(payment);
            JsonNode constructionRoot = objectMapper.readTree(construction);
            PlanTemplateCatalog loaded = new PlanTemplateCatalog();
            loaded.setPaymentPlanBaselineVersion(paymentRoot.path("version").asInt(1));
            loaded.setConstructionPlanBaselineVersion(constructionRoot.path("version").asInt(1));
            loaded.setPaymentPlans(paymentPlans(paymentRoot.path("templates"), loaded.getPaymentPlanBaselineVersion()));
            loaded.setConstructionPlans(constructionPlans(constructionRoot.path("templates"), loaded.getConstructionPlanBaselineVersion()));
            catalog = loaded;
        } catch (Exception ex) {
            throw new IllegalStateException("计划模板基线加载失败", ex);
        }
    }

    public PlanTemplateCatalog catalog() {
        if (catalog == null) load();
        return catalog;
    }

    public PaymentPlanTemplate paymentPlan(String code) {
        String target = code == null || code.isBlank() ? "STANDARD_30_40_30" : code;
        return catalog().getPaymentPlans().stream()
                .filter(item -> target.equals(item.getCode()))
                .findFirst()
                .orElseThrow(() -> notFound("付款计划模板不存在或已停用：" + target));
    }

    public ConstructionPlanTemplate constructionPlan(String code) {
        String target = code == null || code.isBlank() ? "RESIDENTIAL_STANDARD_8" : code;
        return catalog().getConstructionPlans().stream()
                .filter(item -> target.equals(item.getCode()))
                .findFirst()
                .orElseThrow(() -> notFound("施工计划模板不存在或已停用：" + target));
    }

    private List<PaymentPlanTemplate> paymentPlans(JsonNode nodes, int version) {
        List<PaymentPlanTemplate> result = new ArrayList<>();
        for (JsonNode node : nodes) {
            PaymentPlanTemplate template = new PaymentPlanTemplate();
            template.setCode(text(node, "code"));
            template.setName(text(node, "name"));
            template.setDescription(text(node, "description"));
            template.setSort(node.path("sort").asInt(0));
            template.setVersion(version);
            List<PaymentPlanNodeTemplate> items = new ArrayList<>();
            for (JsonNode item : node.path("nodes")) {
                PaymentPlanNodeTemplate planNode = new PaymentPlanNodeTemplate();
                planNode.setCode(text(item, "code"));
                planNode.setName(text(item, "name"));
                planNode.setRatio(decimal(item, "ratio"));
                planNode.setType(enumValue(ReceivableType.class, text(item, "type"), ReceivableType.Progress));
                planNode.setTriggerType(enumValue(PaymentTriggerType.class, text(item, "triggerType"), PaymentTriggerType.ContractSigned));
                planNode.setTriggerStageCode(textOrNull(item, "triggerStageCode"));
                planNode.setOffsetDays(item.path("offsetDays").asInt(0));
                planNode.setSort(item.path("sort").asInt(0));
                items.add(planNode);
            }
            template.setNodes(items);
            validatePaymentTemplate(template);
            result.add(template);
        }
        return result.stream().sorted(java.util.Comparator.comparing(PaymentPlanTemplate::getSort)).toList();
    }

    private List<ConstructionPlanTemplate> constructionPlans(JsonNode nodes, int version) {
        List<ConstructionPlanTemplate> result = new ArrayList<>();
        for (JsonNode node : nodes) {
            ConstructionPlanTemplate template = new ConstructionPlanTemplate();
            template.setCode(text(node, "code"));
            template.setName(text(node, "name"));
            template.setDescription(text(node, "description"));
            template.setTotalDays(node.path("totalDays").asInt(0));
            template.setSort(node.path("sort").asInt(0));
            template.setVersion(version);
            List<ConstructionStageTemplate> stages = new ArrayList<>();
            for (JsonNode item : node.path("stages")) {
                ConstructionStageTemplate stage = new ConstructionStageTemplate();
                stage.setCode(text(item, "code"));
                stage.setName(text(item, "name"));
                stage.setDefaultDays(Math.max(1, item.path("defaultDays").asInt(1)));
                stage.setAcceptanceMode(enumValue(StageAcceptanceMode.class, text(item, "acceptanceMode"), StageAcceptanceMode.None));
                stage.setSort(item.path("sort").asInt(0));
                stage.setDescription(text(item, "description"));
                stages.add(stage);
            }
            template.setStages(stages);
            validateConstructionTemplate(template);
            result.add(template);
        }
        return result.stream().sorted(java.util.Comparator.comparing(ConstructionPlanTemplate::getSort)).toList();
    }

    private void validatePaymentTemplate(PaymentPlanTemplate template) {
        if (template.getCode().isBlank() || template.getName().isBlank() || template.getNodes().isEmpty()) {
            throw invalid("付款计划基线缺少编码、名称或节点");
        }
        BigDecimal total = template.getNodes().stream().map(PaymentPlanNodeTemplate::getRatio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ONE) != 0) {
            throw invalid("付款计划基线比例合计必须为100%：" + template.getCode());
        }
    }

    private void validateConstructionTemplate(ConstructionPlanTemplate template) {
        if (template.getCode().isBlank() || template.getName().isBlank() || template.getStages().isEmpty()) {
            throw invalid("施工计划基线缺少编码、名称或阶段");
        }
        long duplicateCodes = template.getStages().stream().map(ConstructionStageTemplate::getCode).distinct().count();
        if (duplicateCodes != template.getStages().size()) {
            throw invalid("施工计划阶段编码不能重复：" + template.getCode());
        }
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("");
    }

    private String textOrNull(JsonNode node, String field) {
        String value = text(node, field);
        return value.isBlank() ? null : value;
    }

    private BigDecimal decimal(JsonNode node, String field) {
        return node.path(field).decimalValue();
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("500", message));
    }

    private DefinedException notFound(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }
}
