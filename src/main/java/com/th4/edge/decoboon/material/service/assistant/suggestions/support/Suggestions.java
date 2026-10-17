package com.th4.edge.decoboon.material.service.assistant.suggestions.support;

import com.th4.edge.decoboon.material.assistant.MaterialAssistantIntent;
import com.th4.edge.decoboon.material.assistant.MaterialAssistantKnowledge;
import com.th4.edge.decoboon.material.model.MaterialAssistantSuggestionQuery;
import com.th4.edge.decoboon.material.model.MaterialAssistantSuggestions;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * material/assistant/suggestions 实现。
 *
 * <p>只从 MaterialAssistantKnowledge 的 50 个黄金问题中挑选建议，不生成新的业务事实；
 * 不读取项目/作业单金额，不产生写操作。上下文索引只用于选择问题分组。</p>
 */
@Note("材料业务助手建议问题")
@Properties(allow = {
        @AllowProperties(model = MaterialAssistantSuggestions.class,
                properties = {"contextType", "suggestions", "readOnly"})
})
@Service("material/assistant/suggestions")
@Scope(ScopeTypes.PROTOTYPE)
public class Suggestions implements com.th4.edge.decoboon.material.service.assistant.suggestions.Suggestions {

    private static final int MAX_SUGGESTIONS = 6;

    @User private Employee employee;
    @AutoInject private Long projectId;
    @AutoInject private Long sheetId;
    @AutoInject private String contextType;

    private final MaterialAssistantKnowledge knowledge;
    private final TenantContext tenantContext;

    public Suggestions(MaterialAssistantKnowledge knowledge, TenantContext tenantContext) {
        this.knowledge = knowledge;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialAssistantSuggestions call(MaterialAssistantSuggestionQuery request) {
        tenantContext.requireEmployee(employee);
        String type = normalizeType(contextType, projectId, sheetId);
        LinkedHashSet<String> questions = new LinkedHashSet<>();
        for (MaterialAssistantIntent intent : intents(type)) {
            questions.addAll(knowledge.suggestedQuestions(intent, 2));
            if (questions.size() >= MAX_SUGGESTIONS) break;
        }
        MaterialAssistantSuggestions result = new MaterialAssistantSuggestions();
        result.setContextType(type);
        result.setSuggestions(questions.stream().limit(MAX_SUGGESTIONS).toList());
        result.setReadOnly(true);
        return result;
    }

    private String normalizeType(String raw, Long projectId, Long sheetId) {
        if (raw != null && !raw.isBlank()) {
            String value = raw.trim().toLowerCase(Locale.ROOT);
            if (List.of("project", "sheet", "purchase", "settlement", "general").contains(value)) return value;
        }
        if (sheetId != null) return "sheet";
        if (projectId != null) return "project";
        return "general";
    }

    private List<MaterialAssistantIntent> intents(String type) {
        return switch (type) {
            case "sheet" -> List.of(
                    MaterialAssistantIntent.SHEET_STATUS,
                    MaterialAssistantIntent.PRICE_COST,
                    MaterialAssistantIntent.TIME_SEMANTICS);
            case "project" -> List.of(
                    MaterialAssistantIntent.PROJECT_HEALTH,
                    MaterialAssistantIntent.SELECTION_GATE,
                    MaterialAssistantIntent.CHANGE);
            case "purchase" -> List.of(
                    MaterialAssistantIntent.SHEET_VS_PURCHASE,
                    MaterialAssistantIntent.SUPPLIER,
                    MaterialAssistantIntent.PRICE_COST,
                    MaterialAssistantIntent.RECEIPT);
            case "settlement" -> List.of(
                    MaterialAssistantIntent.SETTLEMENT,
                    MaterialAssistantIntent.PRICE_COST,
                    MaterialAssistantIntent.RECEIPT);
            default -> List.of(
                    MaterialAssistantIntent.SHEET_VS_PURCHASE,
                    MaterialAssistantIntent.PRICE_COST,
                    MaterialAssistantIntent.PERMISSION);
        };
    }
}
