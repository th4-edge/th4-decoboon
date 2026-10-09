package com.th4.edge.decoboon.content.context;

import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.decoboon.content.repository.ContentAssetRepository;
import com.th4.edge.decoboon.content.repository.ContentAuthorizationRepository;
import com.th4.edge.decoboon.content.repository.ContentTaskRepository;
import com.th4.edge.decoboon.content.repository.EmployeeContentRepository;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * 内容素材与授权权限上下文。
 *
 * <p>P0 素材必须有项目归属。企业负责人、店长按企业/门店管理，其他员工只能访问本人参与项目和本人上传素材。</p>
 */
@Note("内容素材权限")
@Component
public class ContentAccessContext {

    private final TenantContext tenantContext;
    private final ProjectAccessContext projectAccessContext;
    private final ContentAssetRepository assetRepository;
    private final ContentAuthorizationRepository authorizationRepository;
    private final ContentTaskRepository taskRepository;
    private final EmployeeContentRepository employeeContentRepository;

    public ContentAccessContext(TenantContext tenantContext,
                                ProjectAccessContext projectAccessContext,
                                ContentAssetRepository assetRepository,
                                ContentAuthorizationRepository authorizationRepository,
                                ContentTaskRepository taskRepository,
                                EmployeeContentRepository employeeContentRepository) {
        this.tenantContext = tenantContext;
        this.projectAccessContext = projectAccessContext;
        this.assetRepository = assetRepository;
        this.authorizationRepository = authorizationRepository;
        this.taskRepository = taskRepository;
        this.employeeContentRepository = employeeContentRepository;
    }

    public Employee requireEmployee(Employee employee) {
        Employee current = tenantContext.requireEmployee(employee);
        if (current == null || current.getCompany() == null) {
            throw denied("当前账号未加入企业");
        }
        return current;
    }

    public boolean isManager(Employee employee) {
        return employee.getRole() == EmployeeRole.Boss || employee.getRole() == EmployeeRole.StoreManager;
    }

    public Employee requireManager(Employee employee) {
        Employee current = requireEmployee(employee);
        if (!isManager(current)) {
            throw denied("仅企业负责人或店长可以维护内容素材和授权");
        }
        return current;
    }

    public ContentAsset requireAsset(Long assetId, Employee employee) {
        Employee current = requireEmployee(employee);
        if (assetId == null) {
            throw notFound();
        }
        ContentAsset asset = assetRepository.findByAssetIdAndCompany_ComId(assetId, current.getCompany().getComId())
                .orElseThrow(ContentAccessContext::notFound);
        if (isManager(current)) {
            if (current.getRole() == EmployeeRole.StoreManager
                    && asset.getStore() != null
                    && current.getStore() != null
                    && !asset.getStore().getStoreId().equals(current.getStore().getStoreId())) {
                throw notFound();
            }
            return asset;
        }
        if (asset.getProject() == null) {
            throw notFound();
        }
        projectAccessContext.requireProject(asset.getProject().getProjectId(), current);
        return asset;
    }

    public void requireCanUpload(Employee employee, Long projectId) {
        Employee current = requireEmployee(employee);
        if (projectId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "现场素材必须关联项目"));
        }
        projectAccessContext.requireProject(projectId, current);
    }


    /** 公开状态必须存在有效公开发布授权。 */
    public void requirePublicAuthorization(ContentAsset asset) {
        boolean authorized = authorizationRepository.findByAsset_AssetIdOrderByAuthIdDesc(asset.getAssetId()).stream()
                .anyMatch(item -> item.getPurpose() == com.th4.edge.decoboon.content.configure.ContentAuthorizationPurpose.Public
                        && effective(item));
        if (!authorized) {
            throw new DefinedException(new DefaultErrorCode("409", "缺少有效的公开发布授权"));
        }
    }

    /** 宣传状态必须取得企业授权；涉及客户和上传员工时，必须分别取得对应授权。 */
    public void requireMarketingAuthorization(ContentAsset asset) {
        var authorizations = authorizationRepository.findByAsset_AssetIdOrderByAuthIdDesc(asset.getAssetId());
        boolean company = authorizations.stream().anyMatch(item ->
                item.getPurpose() == com.th4.edge.decoboon.content.configure.ContentAuthorizationPurpose.ZnDqMarketing
                        && item.getSubjectType() == com.th4.edge.decoboon.content.configure.ContentAuthorizationSubject.Company
                        && effective(item));
        boolean customer = asset.getCustomer() == null || authorizations.stream().anyMatch(item ->
                item.getPurpose() == com.th4.edge.decoboon.content.configure.ContentAuthorizationPurpose.ZnDqMarketing
                        && item.getSubjectType() == com.th4.edge.decoboon.content.configure.ContentAuthorizationSubject.Customer
                        && effective(item));
        boolean uploader = asset.getUploader() == null || authorizations.stream().anyMatch(item ->
                item.getPurpose() == com.th4.edge.decoboon.content.configure.ContentAuthorizationPurpose.ZnDqMarketing
                        && item.getSubjectType() == com.th4.edge.decoboon.content.configure.ContentAuthorizationSubject.Employee
                        && effective(item));
        if (!company || !customer || !uploader) {
            throw new DefinedException(new DefaultErrorCode("409", "装得清宣传授权不完整，需企业及涉及客户/员工授权"));
        }
    }

    private boolean effective(com.th4.edge.decoboon.content.model.ContentAuthorization item) {
        LocalDate today = LocalDate.now();
        return (item.getStatus() == com.th4.edge.decoboon.content.configure.ContentAuthorizationStatus.Authorized
                || item.getStatus() == com.th4.edge.decoboon.content.configure.ContentAuthorizationStatus.Expiring)
                && item.getValidFrom() != null && item.getValidTo() != null
                && !today.isBefore(item.getValidFrom()) && !today.isAfter(item.getValidTo());
    }

    public ContentTask requireTask(Long taskId, Employee employee) {
        Employee current = requireEmployee(employee);
        if (taskId == null) throw new DefinedException(new DefaultErrorCode("404", "内容任务不存在或无权访问"));
        ContentTask task = taskRepository.findByTaskIdAndCompany_ComId(taskId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "内容任务不存在或无权访问")));
        if (isManager(current)) return task;
        boolean assigned = task.getAssigneeEmpIds() == null || task.getAssigneeEmpIds().isBlank()
                || java.util.Arrays.stream(task.getAssigneeEmpIds().split(",")).anyMatch(id -> id.trim().equals(String.valueOf(current.getEmpId())));
        boolean claimed = employeeContentRepository.findByTask_TaskIdAndEmployee_EmpId(taskId, current.getEmpId()).isPresent();
        if (task.getStatus() == com.th4.edge.decoboon.content.configure.ContentTaskStatus.Draft || (!assigned && !claimed)) {
            throw new DefinedException(new DefaultErrorCode("404", "内容任务不存在或无权访问"));
        }
        return task;
    }

    public ContentTask requireTaskEditor(Employee employee) {
        Employee current = requireManager(employee);
        if (current.getCompany() == null) throw denied("当前账号未加入企业");
        return null;
    }

    public EmployeeContent requireEmployeeContent(Long contentId, Employee employee) {
        Employee current = requireEmployee(employee);
        if (contentId == null) throw new DefinedException(new DefaultErrorCode("404", "员工内容不存在或无权访问"));
        EmployeeContent content = employeeContentRepository.findByContentIdAndCompany_ComId(contentId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "员工内容不存在或无权访问")));
        if (!isManager(current) && !content.getEmployee().getEmpId().equals(current.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("404", "员工内容不存在或无权访问"));
        }
        return content;
    }

    public void requireCanClaim(ContentTask task, Employee employee) {
        Employee current = requireEmployee(employee);
        if (task.getStatus() != com.th4.edge.decoboon.content.configure.ContentTaskStatus.Published
                && task.getStatus() != com.th4.edge.decoboon.content.configure.ContentTaskStatus.Claimed) {
            throw new DefinedException(new DefaultErrorCode("409", "任务当前不可领取"));
        }
        if (task.getAssigneeEmpIds() != null && !task.getAssigneeEmpIds().isBlank()
                && java.util.Arrays.stream(task.getAssigneeEmpIds().split(","))
                .noneMatch(id -> id.trim().equals(String.valueOf(current.getEmpId())))) {
            throw denied("当前账号不在任务分配范围内");
        }
    }


    /** P0 素材状态变更只允许 BOSS/店长，后续内容运营能力再扩展。 */
    public Employee requireAssetReviewer(Employee employee) {
        return requireManager(employee);
    }

    public ContentAsset requirePublishable(ContentAsset asset) {
        if (asset.getStatus() != ContentAssetStatus.PublicReady && asset.getStatus() != ContentAssetStatus.MarketingReady) {
            throw new DefinedException(new DefaultErrorCode("409", "素材尚未完成公开授权或审核"));
        }
        return asset;
    }

    private static DefinedException denied(String message) {
        return new DefinedException(new DefaultErrorCode("403", message));
    }

    private static DefinedException notFound() {
        return new DefinedException(new DefaultErrorCode("404", "内容素材不存在或无权访问"));
    }
}
