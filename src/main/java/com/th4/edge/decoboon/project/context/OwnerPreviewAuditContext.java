package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.project.configure.OwnerPreviewResult;
import com.th4.edge.decoboon.project.configure.OwnerPreviewSource;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectOwnerPreviewLog;
import com.th4.edge.decoboon.project.repository.ProjectOwnerPreviewLogRepository;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 业主视角预览最小审计写入。 */
@Component
public class OwnerPreviewAuditContext {

    private final ProjectOwnerPreviewLogRepository repository;

    public OwnerPreviewAuditContext(ProjectOwnerPreviewLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Employee employee, Project project) {
        record(employee, project, OwnerPreviewResult.Success);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Employee employee, Long requestedProjectId, OwnerPreviewResult result) {
        record(employee, null, requestedProjectId, result);
    }

    private void record(Employee employee, Project project, OwnerPreviewResult result) {
        record(employee, project, project == null ? null : project.getProjectId(), result);
    }

    private void record(Employee employee, Project project, Long requestedProjectId, OwnerPreviewResult result) {
        if (employee == null || employee.getCompany() == null || employee.getCompany().getComId() == null) {
            throw new IllegalStateException("业主视角预览审计缺少有效员工企业");
        }
        if (requestedProjectId == null) {
            throw new IllegalArgumentException("业主视角预览审计缺少项目索引");
        }
        ProjectOwnerPreviewLog log = new ProjectOwnerPreviewLog();
        log.setCompany(employee.getCompany());
        log.setProject(project);
        log.setRequestedProjectId(requestedProjectId);
        log.setEmployee(employee);
        log.setEmployeeName(employee.getName());
        log.setEmployeeRole(employee.getRole());
        log.setSource(OwnerPreviewSource.App);
        log.setResult(result);
        log.setOpenedAt(LocalDateTime.now());
        repository.save(log);
    }
}
