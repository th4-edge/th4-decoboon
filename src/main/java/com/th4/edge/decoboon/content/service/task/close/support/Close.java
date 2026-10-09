package com.th4.edge.decoboon.content.service.task.close.support;

import com.th4.edge.decoboon.content.configure.ContentTaskStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.repository.ContentTaskRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** 内容任务关闭实现。 */
@Note("内容任务关闭")
@Properties(allow = @AllowProperties(model = ContentTask.class, properties = {"taskId", "status", "closedAt"}))
@Service("content/task/close")
@Scope(ScopeTypes.PROTOTYPE)
public class Close implements com.th4.edge.decoboon.content.service.task.close.Close {
    @User private Employee employee;
    @AutoInject @NotNull private Long taskId;
    private final ContentTaskRepository taskRepository;
    private final ContentAccessContext accessContext;

    public Close(ContentTaskRepository taskRepository, ContentAccessContext accessContext) {
        this.taskRepository = taskRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public ContentTask call(ContentTask request) {
        Employee me = accessContext.requireManager(employee);
        ContentTask task = taskRepository.findByTaskIdAndCompany_ComId(taskId, me.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "内容任务不存在或无权访问")));
        if (task.getStatus() == ContentTaskStatus.Closed) return task;
        task.setStatus(ContentTaskStatus.Closed);
        task.setClosedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }
}
