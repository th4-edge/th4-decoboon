package com.th4.edge.decoboon.material.assistant;

import com.th4.edge.decoboon.ai.context.AiUsageContext;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.material.model.MaterialAssistantAnswer;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaterialAssistantAuditContextTest {

    private final AiUsageContext usageContext = mock(AiUsageContext.class);

    @Test
    void auditMustRecordCapabilityWithoutPromptOrAnswerText() {
        MaterialAssistantAuditContext audit = new MaterialAssistantAuditContext(usageContext);
        Employee employee = new Employee();
        employee.setEmpId(9L);
        MaterialAssistantAnswer answer = new MaterialAssistantAnswer();
        answer.setIntent("PRICE_COST");
        answer.setAnswer("成本未完整时利润为 null。");
        AiUsageCall saved = new AiUsageCall();
        saved.setCallId(88L);
        when(usageContext.record(eq(employee), org.mockito.ArgumentMatchers.any(AiUsageCall.class))).thenReturn(saved);

        audit.record(employee, "为什么利润是空的？", answer, "req-1", "idem-1", 7L, null,
                LocalDateTime.now().minusSeconds(1), LocalDateTime.now());

        ArgumentCaptor<AiUsageCall> captor = ArgumentCaptor.forClass(AiUsageCall.class);
        verify(usageContext).record(eq(employee), captor.capture());
        AiUsageCall call = captor.getValue();
        assertThat(call.getBusinessModule()).isEqualTo("material");
        assertThat(call.getCapabilityCode()).isEqualTo("material.assistant.ask");
        assertThat(call.getBusinessType()).isEqualTo("Project");
        assertThat(call.getBusinessId()).isEqualTo(7L);
        assertThat(call.getProviderCode()).isEqualTo("baseline");
        assertThat(call.getModelCode()).isEqualTo("rule-material-v1");
        assertThat(call.getIdempotencyKey()).isEqualTo("material-assistant:idem-1");
        assertThat(answer.getAuditId()).isEqualTo(88L);
        assertThat(answer.getAuditStatus()).isEqualTo("RECORDED");
    }
}
