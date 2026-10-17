package com.th4.edge.decoboon.material.service.assistant.ask.support;

import com.th4.edge.decoboon.material.assistant.MaterialAssistantAuditContext;
import com.th4.edge.decoboon.material.assistant.MaterialAssistantContext;
import com.th4.edge.decoboon.material.model.MaterialAssistantAnswer;
import com.th4.edge.decoboon.material.model.MaterialAssistantCitation;
import com.th4.edge.decoboon.material.model.MaterialAssistantQuery;
import com.th4.edge.decoboon.material.model.MaterialAssistantToolCall;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 材料业务助手只读问答实现。 */
@Note("材料业务助手只读问答")
@Properties(allow = {
        @AllowProperties(model = MaterialAssistantAnswer.class,
                properties = {"question", "intent", "answerType", "answer", "confidence", "readOnly",
                        "citations", "toolCalls", "nextActions", "suggestedQuestions", "auditId", "auditStatus"}),
        @AllowProperties(model = MaterialAssistantCitation.class,
                properties = {"type", "id", "label", "detail"}),
        @AllowProperties(model = MaterialAssistantToolCall.class,
                properties = {"name", "provider", "status", "scope", "readOnly"})
})
@Service("material/assistant/ask")
@Scope(ScopeTypes.PROTOTYPE)
public class Ask implements com.th4.edge.decoboon.material.service.assistant.ask.Ask {

    @User
    private Employee employee;

    @AutoInject
    private String question;
    @AutoInject
    private Long projectId;
    @AutoInject
    private Long sheetId;
    @AutoInject
    private String requestId;
    @AutoInject
    private String idempotencyKey;

    private final MaterialAssistantContext assistantContext;
    private final MaterialAssistantAuditContext auditContext;

    public Ask(MaterialAssistantContext assistantContext, MaterialAssistantAuditContext auditContext) {
        this.assistantContext = assistantContext;
        this.auditContext = auditContext;
    }

    @Override
    public MaterialAssistantAnswer call(MaterialAssistantQuery request) {
        LocalDateTime startedAt = LocalDateTime.now();
        MaterialAssistantAnswer answer = assistantContext.ask(employee, question, projectId, sheetId);
        auditContext.record(employee, question, answer, requestId, idempotencyKey,
                projectId, sheetId, startedAt, LocalDateTime.now());
        return answer;
    }
}
