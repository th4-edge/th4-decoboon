package com.th4.edge.decoboon.material.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.material.model.MaterialAssistantCitation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 材料助手知识读取：只读发布蓝图与 50 个黄金问题，不复制第二套业务事实。 */
@Component
public class MaterialAssistantKnowledge {

    private static final String BLUEPRINT_RESOURCE = "blueprints/material-v1.json";
    private static final String GOLDEN_RESOURCE = "assistant/material-golden-questions.json";

    private final ObjectMapper objectMapper;
    private volatile JsonNode blueprint;
    private volatile List<GoldenQuestion> goldenQuestions;

    public MaterialAssistantKnowledge(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode blueprint() {
        JsonNode value = blueprint;
        if (value != null) return value;
        synchronized (this) {
            if (blueprint == null) blueprint = read(BLUEPRINT_RESOURCE);
            return blueprint;
        }
    }

    public String rule(String id) {
        return node("rules", id).map(item -> item.path("text").asText("")).orElse("");
    }

    public String objectMeaning(String id) {
        return node("objects", id).map(item -> item.path("meaning").asText("")).orElse("");
    }

    public String objectName(String id) {
        return node("objects", id).map(item -> item.path("name").asText("")).orElse(id);
    }

    public String checkLabel(String id) {
        return node("checks", id).map(item -> item.path("label").asText("")).orElse("");
    }

    public String checkCondition(String id) {
        return node("checks", id).map(item -> item.path("condition").asText("")).orElse("");
    }

    public String checkNextAction(String id) {
        return node("checks", id).map(item -> item.path("nextAction").asText("")).orElse("");
    }

    public String exception(String id) {
        return node("exceptions", id).map(item -> item.path("text").asText("")).orElse("");
    }

    public String gap(String id) {
        return node("gaps", id).map(item -> item.path("currentImplementation").asText("")).orElse("");
    }

    public String field(String object, String field) {
        Optional<JsonNode> found = stream("fields").stream()
                .filter(item -> object.equals(item.path("object").asText())
                        && field.equals(item.path("field").asText()))
                .findFirst();
        return found.map(item -> item.path("meaning").asText("")).orElse("");
    }

    public String fieldLabel(String object, String field) {
        Optional<JsonNode> found = stream("fields").stream()
                .filter(item -> object.equals(item.path("object").asText())
                        && field.equals(item.path("field").asText()))
                .findFirst();
        return found.map(item -> item.path("label").asText(field)).orElse(field);
    }

    public List<GoldenQuestion> goldenQuestions() {
        List<GoldenQuestion> value = goldenQuestions;
        if (value != null) return value;
        synchronized (this) {
            if (goldenQuestions == null) {
                JsonNode root = read(GOLDEN_RESOURCE);
                List<GoldenQuestion> rows = new ArrayList<>();
                if (root.isArray()) {
                    for (JsonNode item : root) {
                        List<String> refs = new ArrayList<>();
                        item.path("expectedRefs").forEach(ref -> refs.add(ref.asText()));
                        rows.add(new GoldenQuestion(
                                item.path("id").asText(),
                                item.path("question").asText(),
                                MaterialAssistantIntent.valueOf(item.path("expectedIntent").asText()),
                                List.copyOf(refs)));
                    }
                }
                goldenQuestions = List.copyOf(rows);
            }
            return goldenQuestions;
        }
    }

    public List<String> suggestedQuestions(MaterialAssistantIntent intent, int limit) {
        return goldenQuestions().stream()
                .filter(item -> item.intent() == intent)
                .map(GoldenQuestion::question)
                .limit(limit)
                .toList();
    }

    public MaterialAssistantCitation citation(String type, String id, String label, String detail) {
        MaterialAssistantCitation citation = new MaterialAssistantCitation();
        citation.setType(type);
        citation.setId(id);
        citation.setLabel(label);
        citation.setDetail(detail);
        return citation;
    }

    public MaterialAssistantCitation ruleCitation(String id) {
        return citation("RULE", id, id, rule(id));
    }

    public MaterialAssistantCitation objectCitation(String id) {
        return citation("OBJECT", id, objectName(id), objectMeaning(id));
    }

    public MaterialAssistantCitation checkCitation(String id) {
        return citation("CHECK", id, checkLabel(id), checkCondition(id) + "；下一步：" + checkNextAction(id));
    }

    public MaterialAssistantCitation exceptionCitation(String id) {
        return citation("EXCEPTION", id, id, exception(id));
    }

    public MaterialAssistantCitation fieldCitation(String object, String field) {
        return citation("FIELD", object + "." + field, fieldLabel(object, field), field(object, field));
    }

    public MaterialAssistantCitation gapCitation(String id) {
        return citation("GAP", id, id, gap(id));
    }

    public MaterialAssistantCitation sourceCitation(String id, String label, String detail) {
        return citation("SOURCE", id, label, detail);
    }

    private Optional<JsonNode> node(String collection, String id) {
        return stream(collection).stream().filter(item -> id.equals(item.path("id").asText())).findFirst();
    }

    private List<JsonNode> stream(String collection) {
        JsonNode array = blueprint().path(collection);
        List<JsonNode> rows = new ArrayList<>();
        if (array.isArray()) array.forEach(rows::add);
        return rows;
    }

    private JsonNode read(String resource) {
        try (InputStream input = new ClassPathResource(resource).getInputStream()) {
            return objectMapper.readTree(input.readAllBytes());
        } catch (IOException error) {
            throw new IllegalStateException("材料助手知识资源不可读：" + resource, error);
        }
    }

    public record GoldenQuestion(String id, String question, MaterialAssistantIntent intent, List<String> expectedRefs) {
    }
}
