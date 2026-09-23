package com.th4.edge.decoboon.document.service.frequentproject.list.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.document.repository.VoucherRecordRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 常用项目实现。
 *
 * <p>口径：近 90 天内该企业（并按当前员工数据范围）出凭证最多的项目，最多返回 8 个；
 * 90 天内没有凭证时回退到"最近出过凭证的项目"，仍然没有则返回空列表——
 * 不编造常用项目，前端在空列表时隐藏该分组。</p>
 */
@Note("常用项目")
@Properties(allow = {
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "status", "siteAddr", "customer", "designer"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("document/frequent-project/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.document.service.frequentproject.list.List {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 12;
    private static final int WINDOW_DAYS = 90;

    @User
    private Employee employee;
    @AutoInject
    private Integer size;

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final VoucherRecordRepository recordRepository;
    private final ProjectRepository projectRepository;

    public List(TenantContext tenantContext,
                DataScopeContext dataScopeContext,
                VoucherRecordRepository recordRepository,
                ProjectRepository projectRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.recordRepository = recordRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Project> call(Project request) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss, EmployeeRole.StoreManager,
                EmployeeRole.Finance, EmployeeRole.Pm, EmployeeRole.Designer);
        int limit = size == null || size < 1 ? DEFAULT_LIMIT : Math.min(size, MAX_LIMIT);

        Map<Long, Long> counts = countByProject(current, LocalDateTime.now().minusDays(WINDOW_DAYS));
        if (counts.isEmpty()) {
            counts = countByProject(current, null);
        }
        if (counts.isEmpty()) return java.util.List.of();

        java.util.List<Long> orderedIds = counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
        Map<Long, Project> byId = projectRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(Project::getProjectId, Function.identity(), (left, right) -> left));
        return orderedIds.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private Map<Long, Long> countByProject(Employee current, LocalDateTime since) {
        Specification<VoucherRecord> visible = current.getRole() == EmployeeRole.Boss
                || current.getRole() == EmployeeRole.Finance
                ? (root, query, builder) -> builder.equal(root.get("company").get("comId"),
                        current.getCompany().getComId())
                : DataScopeSpecification.visible(VoucherRecord.class, dataScopeContext.resolve(current));
        if (since != null) {
            visible = visible.and((root, query, builder) -> builder.greaterThan(root.get("generatedAt"), since));
        }
        Map<Long, Long> counts = new LinkedHashMap<>();
        recordRepository.findAll(visible).stream()
                .filter(record -> record.getProject() != null && record.getProject().getProjectId() != null)
                .forEach(record -> counts.merge(record.getProject().getProjectId(), 1L, Long::sum));
        return counts;
    }
}
