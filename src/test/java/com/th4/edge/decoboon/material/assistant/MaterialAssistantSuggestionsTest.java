package com.th4.edge.decoboon.material.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.material.model.MaterialAssistantSuggestions;
import com.th4.edge.decoboon.material.service.assistant.suggestions.support.Suggestions;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaterialAssistantSuggestionsTest {

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final Employee employee = new Employee();
    private final MaterialAssistantKnowledge knowledge = new MaterialAssistantKnowledge(new ObjectMapper());

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(1L);
        employee.setEmpId(1L);
        employee.setCompany(company);
        when(tenantContext.requireEmployee(employee)).thenReturn(employee);
    }

    @Test
    void sheetContextUsesSheetStatusPriceAndTimeQuestions() {
        MaterialAssistantSuggestions result = suggestions("sheet", 7L, 9L);

        assertThat(result.isReadOnly()).isTrue();
        assertThat(result.getContextType()).isEqualTo("sheet");
        assertThat(result.getSuggestions()).isNotEmpty().hasSizeLessThanOrEqualTo(6);
        assertThat(result.getSuggestions()).contains("这张作业单为什么不能提交请购？");
        assertThat(result.getSuggestions()).anyMatch(q -> q.contains("未定价"));
        assertThat(result.getSuggestions()).anyMatch(q -> q.contains("测量") || q.contains("到场"));
    }

    @Test
    void settlementContextUsesSettlementAndCostQuestions() {
        MaterialAssistantSuggestions result = suggestions("settlement", null, null);

        assertThat(result.getContextType()).isEqualTo("settlement");
        assertThat(result.getSuggestions()).anyMatch(q -> q.contains("结算"));
        assertThat(result.getSuggestions()).anyMatch(q -> q.contains("应付") || q.contains("定价"));
    }

    @Test
    void noContextFallsBackToGeneralAndDoesNotDuplicate() {
        MaterialAssistantSuggestions result = suggestions("", null, null);

        assertThat(result.getContextType()).isEqualTo("general");
        assertThat(result.getSuggestions()).doesNotHaveDuplicates();
        assertThat(result.getSuggestions()).anyMatch(q -> q.contains("作业单") || q.contains("采购单"));
    }

    @Test
    void invalidContextTypeFallsBackToSheetWhenSheetIdPresent() {
        MaterialAssistantSuggestions result = suggestions("unknown-type", 7L, 9L);

        assertThat(result.getContextType()).isEqualTo("sheet");
    }

    private MaterialAssistantSuggestions suggestions(String contextType, Long projectId, Long sheetId) {
        Suggestions provider = new Suggestions(knowledge, tenantContext);
        set(provider, "employee", employee);
        set(provider, "projectId", projectId);
        set(provider, "sheetId", sheetId);
        set(provider, "contextType", contextType);
        return provider.call(null);
    }

    private void set(Object target, String field, Object value) {
        try {
            var declared = target.getClass().getDeclaredField(field);
            declared.setAccessible(true);
            declared.set(target, value);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }
}
