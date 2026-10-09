package com.th4.edge.decoboon.content.service.task.info.support;

import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.model.ContentTemplate;
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

/** 内容任务详情实现。 */
@Note("内容任务详情")
@Properties(allow = {
        @AllowProperties(model = ContentTask.class,
                properties = {"taskId", "title", "goal", "targetAudience", "platform", "materialIds",
                        "requiredClaims", "forbiddenClaims", "deadline", "assigneeScope", "assigneeEmpIds",
                        "status", "publishedAt", "closedAt", "createTime", "updateTime", "template",
                        "reviewer", "leadOwner", "createdBy", "store", "claimedByMe", "myContentStatus", "myContentId"}),
        @AllowProperties(model = ContentTemplate.class, properties = {"templateId", "name", "format", "durationSec", "clipMin", "clipMax", "scriptSchema", "allowedAssetTypes", "status", "version"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("content/task/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.content.service.task.info.Info {
    @User private Employee employee;
    @AutoInject @NotNull private Long taskId;
    private final ContentAccessContext accessContext;
    private final EmployeeContentRepository employeeContentRepository;

    public Info(ContentAccessContext accessContext, EmployeeContentRepository employeeContentRepository) {
        this.accessContext = accessContext;
        this.employeeContentRepository = employeeContentRepository;
    }

    @Override
    public ContentTask call(ContentTask request) {
        Employee me = accessContext.requireEmployee(employee);
        ContentTask task = accessContext.requireTask(taskId, me);
        var mine = employeeContentRepository.findByTask_TaskIdAndEmployee_EmpId(task.getTaskId(), me.getEmpId());
        task.setClaimedByMe(mine.isPresent());
        task.setMyContentStatus(mine.map(item -> item.getStatus().getValue()).orElse(null));
        task.setMyContentId(mine.map(item -> item.getContentId()).orElse(null));
        task.setMyContentId(mine.map(item -> item.getContentId()).orElse(null));
        return task;
    }
}
