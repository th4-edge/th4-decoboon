package com.th4.edge.decoboon.content.service.task.save.support;

import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.decoboon.content.configure.ContentPlatform;
import com.th4.edge.decoboon.content.configure.ContentTaskStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.model.ContentTemplate;
import com.th4.edge.decoboon.content.repository.ContentAssetRepository;
import com.th4.edge.decoboon.content.repository.ContentTaskRepository;
import com.th4.edge.decoboon.content.repository.ContentTemplateRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** 内容任务保存实现。 */
@Note("内容任务保存")
@Properties(allow = {
        @AllowProperties(model = ContentTask.class,
                properties = {"taskId", "title", "goal", "targetAudience", "platform", "materialIds",
                        "requiredClaims", "forbiddenClaims", "deadline", "assigneeScope", "assigneeEmpIds",
                        "status", "createTime", "updateTime", "template", "reviewer", "leadOwner", "createdBy"}),
        @AllowProperties(model = ContentTemplate.class, properties = {"templateId", "name", "format", "durationSec", "clipMin", "clipMax", "status", "version"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("content/task/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.content.service.task.save.Save {
    @User private Employee employee;
    @AutoInject private Long taskId;
    @AutoInject @NotEmpty @Length(max = 255) private String title;
    @AutoInject @NotEmpty @Length(max = 32) private String goal;
    @AutoInject @NotEmpty @Length(max = 500) private String targetAudience;
    @AutoInject private ContentPlatform platform;
    @AutoInject @NotNull private Long templateId;
    @AutoInject @NotEmpty private List<Long> materialIds;
    @AutoInject @NotEmpty private String requiredClaims;
    @AutoInject @NotEmpty private String forbiddenClaims;
    @AutoInject @NotNull private LocalDateTime deadline;
    @AutoInject private String assigneeScope;
    @AutoInject private List<Long> assigneeEmpIds;
    @AutoInject private Long reviewerId;
    @AutoInject private Long leadOwnerId;

    private final ContentTaskRepository taskRepository;
    private final ContentTemplateRepository templateRepository;
    private final ContentAssetRepository assetRepository;
    private final ContentAccessContext accessContext;
    private final EmployeeRepository employeeRepository;

    public Save(ContentTaskRepository taskRepository, ContentTemplateRepository templateRepository,
                ContentAssetRepository assetRepository, ContentAccessContext accessContext,
                EmployeeRepository employeeRepository) {
        this.taskRepository = taskRepository;
        this.templateRepository = templateRepository;
        this.assetRepository = assetRepository;
        this.accessContext = accessContext;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public ContentTask call(ContentTask request) {
        Employee me = accessContext.requireManager(employee);
        validateDeadline(deadline);
        ContentTemplate template = templateRepository.findByTemplateIdAndCompany_ComId(templateId, me.getCompany().getComId())
                .or(() -> templateRepository.findByTemplateIdAndCompanyIsNull(templateId))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "内容模板不存在或无权访问")));

        List<ContentAsset> assets = assetRepository.findAllById(materialIds);
        if (assets.size() != materialIds.stream().distinct().count()) {
            throw new DefinedException(new DefaultErrorCode("404", "部分素材不存在或无权访问"));
        }
        for (ContentAsset asset : assets) {
            if (!asset.getCompany().getComId().equals(me.getCompany().getComId())) {
                throw new DefinedException(new DefaultErrorCode("404", "部分素材不存在或无权访问"));
            }
            if (asset.getStatus() != ContentAssetStatus.PublicReady && asset.getStatus() != ContentAssetStatus.MarketingReady) {
                throw new DefinedException(new DefaultErrorCode("409", "任务素材必须完成公开发布授权"));
            }
        }

        ContentTask task = taskId == null ? new ContentTask() : taskRepository.findByTaskIdAndCompany_ComId(taskId, me.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "内容任务不存在或无权访问")));
        if (task.getTaskId() != null && task.getStatus() != ContentTaskStatus.Draft) {
            throw new DefinedException(new DefaultErrorCode("409", "只有草稿任务可以编辑"));
        }
        if (task.getTaskId() == null) {
            task.setCompany(me.getCompany());
            task.setStore(me.getStore());
            task.setCreatedBy(me);
            task.setStatus(ContentTaskStatus.Draft);
        }
        task.setTemplate(template);
        task.setTitle(title.trim());
        task.setGoal(goal.trim());
        task.setTargetAudience(targetAudience.trim());
        task.setPlatform(platform == null ? ContentPlatform.Douyin : platform);
        task.setMaterialIds(materialIds.stream().distinct().map(String::valueOf).collect(Collectors.joining(",")));
        task.setRequiredClaims(requiredClaims.trim());
        task.setForbiddenClaims(forbiddenClaims.trim());
        task.setDeadline(deadline);
        task.setAssigneeScope(assigneeScope == null || assigneeScope.isBlank() ? "Company" : assigneeScope.trim());
        task.setAssigneeEmpIds(assigneeEmpIds == null || assigneeEmpIds.isEmpty() ? null
                : assigneeEmpIds.stream().distinct().map(String::valueOf).collect(Collectors.joining(",")));
        task.setReviewer(reviewerId == null ? me : requireEmployee(me, reviewerId, "审核人"));
        task.setLeadOwner(leadOwnerId == null ? me : requireEmployee(me, leadOwnerId, "线索承接人"));
        return taskRepository.save(task);
    }

    private Employee requireEmployee(Employee me, Long empId, String label) {
        Employee target = employeeRepository.findByEmpIdAndCompany_ComId(empId, me.getCompany().getComId());
        if (target == null) {
            throw new DefinedException(new DefaultErrorCode("404", label + "不存在或不属于当前企业"));
        }
        return target;
    }

    private void validateDeadline(LocalDateTime value) {
        if (value.isBefore(LocalDateTime.now())) {
            throw new DefinedException(new DefaultErrorCode("400", "任务截止时间必须晚于当前时间"));
        }
    }
}
