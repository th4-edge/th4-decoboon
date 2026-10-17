package com.th4.edge.decoboon.material.assistant;

import com.th4.edge.decoboon.ai.configure.AiUsageStatus;
import com.th4.edge.decoboon.ai.context.AiUsageContext;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.material.model.MaterialAssistantAnswer;
import com.th4.edge.tenant.user.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/** 材料助手调用审计：复用既有 AI 调用底账，不保存问题正文或回答正文。 */
@Component
public class MaterialAssistantAuditContext {

    private static final Logger LOG = LoggerFactory.getLogger(MaterialAssistantAuditContext.class);

    private final AiUsageContext usageContext;

    public MaterialAssistantAuditContext(AiUsageContext usageContext) {
        this.usageContext = usageContext;
    }

    public void record(Employee employee, String question, MaterialAssistantAnswer answer,
                       String requestId, String idempotencyKey,
                       Long projectId, Long sheetId,
                       LocalDateTime startedAt, LocalDateTime completedAt) {
        try {
            AiUsageCall call = new AiUsageCall();
            call.setRequestId(trim(requestId));
            call.setIdempotencyKey(trim(idempotencyKey).isBlank()
                    ? "material-assistant:" + safeEmpId(employee) + ":" + UUID.randomUUID()
                    : "material-assistant:" + trim(idempotencyKey));
            call.setBusinessModule("material");
            call.setCapabilityCode("material.assistant.ask");
            call.setSceneCode("material.readonly-explanation");
            call.setBusinessType(sheetId != null ? "MaterialOrderSheet" : projectId != null ? "Project" : null);
            call.setBusinessId(sheetId != null ? sheetId : projectId);
            call.setProviderCode("baseline");
            call.setModelCode("rule-material-v1");
            call.setModelVersion("1.0.0");
            call.setCapabilityVersion("B4.1");
            call.setRateCardVersion("shadow-direct-v1");
            call.setInputUnits((long) (question == null ? 0 : question.length()));
            call.setOutputUnits((long) (answer.getAnswer() == null ? 0 : answer.getAnswer().length()));
            call.setStatus(AiUsageStatus.Succeeded);
            call.setStartedAt(startedAt);
            call.setCompletedAt(completedAt);
            call.setEstimated(Boolean.TRUE);
            AiUsageCall saved = usageContext.record(employee, call);
            answer.setAuditId(saved.getCallId());
            answer.setAuditStatus("RECORDED");
        } catch (RuntimeException error) {
            LOG.warn("material assistant audit unavailable empId={} intent={}",
                    employee == null ? null : employee.getEmpId(), answer.getIntent(), error);
            answer.setAuditStatus("UNAVAILABLE");
        }
    }

    private String safeEmpId(Employee employee) {
        return employee == null || employee.getEmpId() == null ? "unknown" : String.valueOf(employee.getEmpId());
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
