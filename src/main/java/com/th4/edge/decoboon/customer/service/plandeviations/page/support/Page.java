package com.th4.edge.decoboon.customer.service.plandeviations.page.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.project.configure.PlanDeviationStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectPlanDeviation;
import com.th4.edge.decoboon.project.repository.ProjectPlanDeviationRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业主计划偏差分页实现。
 *
 * <p>DEC-051：偏差对象对业主可见，但只暴露已确认结论，内部待确认的“工期延误”不对业主展示；
 * 白名单只包含日期、偏差天数、原因分类、顺延方式、状态、确认人与时间。</p>
 */
@Note("业主计划偏差分页")
@Properties(allow = {
        @AllowProperties(model = ProjectPlanDeviation.class,
                properties = {"deviationId", "project", "baseEndDate", "expectedEndDate", "deviationDays",
                        "reasonType", "scheduleStrategy", "status", "confirmedBy", "confirmedAt",
                        "newBaseEndDate", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/plan-deviation/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.plandeviations.page.Page {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long projectId;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    private final CustomerAccessContext accessContext;
    private final ProjectPlanDeviationRepository deviationRepository;

    public Page(CustomerAccessContext accessContext, ProjectPlanDeviationRepository deviationRepository) {
        this.accessContext = accessContext;
        this.deviationRepository = deviationRepository;
    }

    @Override
    public org.springframework.data.domain.Page<ProjectPlanDeviation> call(ProjectPlanDeviation request) {
        Credentials credential = accessContext.requireCredential(principal);
        Project project = accessContext.requireProject(credential, projectId);
        List<ProjectPlanDeviation> all = deviationRepository
                .findByProject_ProjectIdOrderByDeviationIdDesc(project.getProjectId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getVisibleToCustomer()))
                .filter(item -> item.getStatus() == PlanDeviationStatus.Confirmed
                        || item.getStatus() == PlanDeviationStatus.RoutedToChange)
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = Math.min(page * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return new PageImpl<>(all.subList(from, to), PageRequest.of(page, pageSize), all.size());
    }
}
