package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.customer.repository.CustomerAccessRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.configure.SessionType;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.stereotype.Component;

import java.util.List;

/** 业主身份与项目显式授权上下文。 */
@Component
public class CustomerAccessContext {

    private final CustomerAccessRepository accessRepository;
    private final UserIdentityContext userContext;

    public CustomerAccessContext(CustomerAccessRepository accessRepository,
                                 UserIdentityContext userContext) {
        this.accessRepository = accessRepository;
        this.userContext = userContext;
    }

    /** 业主接口只接受微信 Credentials 会话，不把员工 token 降级或提升为业主身份。 */
    public Credentials requireCredential(User<?> principal) {
        if (!(principal instanceof Credentials credentials)) {
            throw new DefinedException(new DefaultErrorCode("403", "当前会话不是业主身份"));
        }
        return userContext.requireUser(credentials.getCreId());
    }

    public List<CustomerAccess> activeAccesses(Credentials credentials) {
        return accessRepository.findByCredential_CreIdAndStatusOrderByAccessIdAsc(
                credentials.getCreId(), CustomerAccessStatus.Active);
    }

    public CustomerAccess requireAccess(Credentials credentials, Long projectId) {
        if (projectId == null) {
            throw new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问"));
        }
        return accessRepository.findByCredential_CreIdAndProject_ProjectIdAndStatus(
                        credentials.getCreId(), projectId, CustomerAccessStatus.Active)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "项目不存在或无权访问")));
    }

    public Project requireProject(Credentials credentials, Long projectId) {
        return requireAccess(credentials, projectId).getProject();
    }

    public void attachSession(Credentials credentials) {
        var employee = userContext.activeEmployee(credentials);
        if (employee != null) {
            credentials.setEmployee(employee);
            credentials.setSessionType(SessionType.Employee);
            credentials.setCustomerAccesses(activeAccesses(credentials));
            return;
        }
        List<CustomerAccess> accesses = activeAccesses(credentials);
        if (!accesses.isEmpty()) {
            credentials.setSessionType(SessionType.Customer);
            credentials.setCustomerAccesses(accesses);
            return;
        }
        credentials.setSessionType(SessionType.User);
        credentials.setCustomerAccesses(List.of());
    }
}
