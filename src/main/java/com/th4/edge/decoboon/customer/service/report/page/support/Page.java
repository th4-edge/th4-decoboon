package com.th4.edge.decoboon.customer.service.report.page.support;

import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 只返回业主可见标记为 true 的动态。 */
@Note("业主项目动态分页")
@Properties(allow = {
        @AllowProperties(model = ProjectReport.class,
                properties = {"reportId", "project", "property", "stage", "type", "title", "content", "images",
                        "author", "authorRole", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "propertyNo", "community", "roomNo", "address"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status", "progress"}),
        // 工地相册按"朋友圈"展示需要落款人：只给姓名与角色，不含手机号等敏感字段
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/report/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.report.page.Page {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    private Long projectId;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    private final CustomerAccessContext accessContext;
    private final ProjectReportRepository reportRepository;

    public Page(CustomerAccessContext accessContext, ProjectReportRepository reportRepository) {
        this.accessContext = accessContext;
        this.reportRepository = reportRepository;
    }

    @Override
    public org.springframework.data.domain.Page<ProjectReport> call(ProjectReport request) {
        Credentials credential = accessContext.requireCredential(principal);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        if (projectId != null) {
            Project project = accessContext.requireProject(credential, projectId);
            return reportRepository
                    .findByCompany_ComIdAndProject_ProjectIdAndVisibleToCustomerTrueOrderByCreateTime_DateTimeDesc(
                            project.getCompany().getComId(), projectId, PageRequest.of(page, pageSize));
        }
        List<ProjectReport> all = new ArrayList<>();
        accessContext.activeAccesses(credential).forEach(access -> {
            Project project = access.getProject();
            all.addAll(reportRepository
                    .findByCompany_ComIdAndProject_ProjectIdAndVisibleToCustomerTrueOrderByCreateTime_DateTimeDesc(
                            project.getCompany().getComId(), project.getProjectId(), PageRequest.of(0, 200))
                    .getContent());
        });
        all.sort(Comparator.comparing(
                report -> report.getCreateTime() == null ? null : report.getCreateTime().getDateTime(),
                Comparator.nullsLast(Comparator.reverseOrder())));
        int from = Math.min(page * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return new org.springframework.data.domain.PageImpl<>(
                all.subList(from, to), PageRequest.of(page, pageSize), all.size());
    }
}
