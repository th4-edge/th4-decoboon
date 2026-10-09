package com.th4.edge.decoboon.content.service.task.claim.support;

import com.th4.edge.decoboon.content.configure.ContentTaskStatus;
import com.th4.edge.decoboon.content.configure.EmployeeContentStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.decoboon.content.repository.ContentTaskRepository;
import com.th4.edge.decoboon.content.repository.EmployeeContentRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** 员工领取内容任务实现。 */
@Note("员工领取内容任务")
@Properties(allow = {
        @AllowProperties(model = ContentTask.class, properties = {"taskId", "title", "status", "deadline", "claimedByMe", "myContentStatus", "myContentId"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("content/task/claim")
@Scope(ScopeTypes.PROTOTYPE)
public class Claim implements com.th4.edge.decoboon.content.service.task.claim.Claim {
    @User private Employee employee;
    @AutoInject @NotNull private Long taskId;
    private final ContentTaskRepository taskRepository;
    private final EmployeeContentRepository employeeContentRepository;
    private final ContentAccessContext accessContext;

    public Claim(ContentTaskRepository taskRepository, EmployeeContentRepository employeeContentRepository,
                 ContentAccessContext accessContext) {
        this.taskRepository = taskRepository;
        this.employeeContentRepository = employeeContentRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public ContentTask call(ContentTask request) {
        Employee me = accessContext.requireEmployee(employee);
        ContentTask task = accessContext.requireTask(taskId, me);
        accessContext.requireCanClaim(task, me);
        EmployeeContent mine = employeeContentRepository.findByTask_TaskIdAndEmployee_EmpId(taskId, me.getEmpId())
                .orElseGet(() -> {
                    EmployeeContent created = new EmployeeContent();
                    created.setCompany(me.getCompany());
                    created.setTask(task);
                    created.setEmployee(me);
                    created.setStatus(EmployeeContentStatus.NotStarted);
                    created.setStartedAt(LocalDateTime.now());
                    return employeeContentRepository.save(created);
                });
        if (task.getStatus() == ContentTaskStatus.Published) {
            task.setStatus(ContentTaskStatus.Claimed);
            taskRepository.save(task);
        }
        task.setClaimedByMe(true);
        task.setMyContentStatus(mine.getStatus().getValue());
        task.setMyContentId(mine.getContentId());
        return task;
    }
}
