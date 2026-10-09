package com.th4.edge.decoboon.content.service.task.page.support;

import com.th4.edge.decoboon.content.configure.ContentTaskStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.model.ContentTemplate;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.decoboon.content.repository.ContentTaskRepository;
import com.th4.edge.decoboon.content.repository.EmployeeContentRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;

/** 内容任务分页实现。 */
@Note("内容任务分页")
@Properties(allow = {
        @AllowProperties(model = ContentTask.class,
                properties = {"taskId", "title", "goal", "targetAudience", "platform", "materialIds",
                        "requiredClaims", "forbiddenClaims", "deadline", "assigneeScope", "assigneeEmpIds",
                        "status", "publishedAt", "closedAt", "createTime", "updateTime", "template",
                        "reviewer", "leadOwner", "createdBy", "store", "claimedByMe", "myContentStatus", "myContentId"}),
        @AllowProperties(model = ContentTemplate.class, properties = {"templateId", "name", "format", "durationSec", "clipMin", "clipMax", "status", "version"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("content/task/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.content.service.task.page.Page {
    @User private Employee employee;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private ContentTaskStatus status;
    @AutoInject private String keyword;

    private final ContentTaskRepository taskRepository;
    private final EmployeeContentRepository employeeContentRepository;
    private final ContentAccessContext accessContext;

    public Page(ContentTaskRepository taskRepository, EmployeeContentRepository employeeContentRepository,
                ContentAccessContext accessContext) {
        this.taskRepository = taskRepository;
        this.employeeContentRepository = employeeContentRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<ContentTask> call(ContentTask request) {
        Employee me = accessContext.requireEmployee(employee);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<ContentTask> source = taskRepository.findByCompany_ComIdOrderByTaskIdDesc(me.getCompany().getComId()).stream()
                .filter(task -> accessContext.isManager(me) || visibleToEmployee(task, me))
                .filter(task -> status == null || task.getStatus() == status)
                .filter(task -> kw.isBlank() || (task.getTitle() != null && task.getTitle().toLowerCase().contains(kw)))
                .sorted(Comparator.comparing(ContentTask::getTaskId, Comparator.nullsLast(Comparator.reverseOrder())))
                .peek(task -> applyMyStatus(task, me))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = Math.min(page * pageSize, source.size());
        int to = Math.min(from + pageSize, source.size());
        return new PageImpl<>(source.subList(from, to), PageRequest.of(page, pageSize), source.size());
    }

    private boolean visibleToEmployee(ContentTask task, Employee me) {
        if (task.getStatus() == ContentTaskStatus.Draft) return false;
        boolean assigned = task.getAssigneeEmpIds() == null || task.getAssigneeEmpIds().isBlank()
                || java.util.Arrays.stream(task.getAssigneeEmpIds().split(",")).anyMatch(id -> id.trim().equals(String.valueOf(me.getEmpId())));
        return assigned || employeeContentRepository.findByTask_TaskIdAndEmployee_EmpId(task.getTaskId(), me.getEmpId()).isPresent();
    }

    private void applyMyStatus(ContentTask task, Employee me) {
        var mine = employeeContentRepository.findByTask_TaskIdAndEmployee_EmpId(task.getTaskId(), me.getEmpId());
        task.setClaimedByMe(mine.isPresent());
        task.setMyContentStatus(mine.map(item -> item.getStatus().getValue()).orElse(null));
        task.setMyContentId(mine.map(item -> item.getContentId()).orElse(null));
        task.setMyContentId(mine.map(item -> item.getContentId()).orElse(null));
    }
}
