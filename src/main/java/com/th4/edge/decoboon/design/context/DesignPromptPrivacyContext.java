package com.th4.edge.decoboon.design.context;

import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.decoboon.design.model.DesignPromptSnapshot;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

/** AI设计Prompt按角色裁剪。 */
@Note("AI设计Prompt隐私上下文")
@Component
public class DesignPromptPrivacyContext {

    public void mask(DesignGenerationTask task, Employee employee) {
        if (task == null) {
            return;
        }
        mask(task.getPromptSnapshot(), employee);
    }

    public void mask(DesignPromptSnapshot prompt, Employee employee) {
        if (prompt == null) {
            return;
        }
        if (employee == null || employee.getRole() != EmployeeRole.Designer) {
            prompt.setTalkPrompt(null);
        }
    }
}
