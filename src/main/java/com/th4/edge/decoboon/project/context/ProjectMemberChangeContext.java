package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 项目成员变更记录。
 *
 * 收敛原本重复在 project/update 与 members/save 两处的成员调整动态逻辑，
 * 并落实「服务主体知情权」：业主有权知道是谁在服务，就同样有权知道**它变了**
 * （USER-DECISION-20261014-002）。
 *
 * 一条变更产生**两份文案**：
 * <ul>
 *   <li>{@code content} 内部口吻，含操作人与时间，供企业侧看；</li>
 *   <li>{@code customerSummary} 业主口吻，只写「岗位由 A 变更为 B（生效日）」，
 *       <b>不含内部操作人</b>，业主端只读这一份。</li>
 * </ul>
 */
@Note("项目成员变更记录")
@Component
public class ProjectMemberChangeContext {

    /** 一次成员变更：岗位 / 原负责人 / 新负责人。 */
    public record Change(String label, String beforeName, String afterName) {
    }

    private final ProjectReportRepository reportRepository;
    private final ProjectPermissionContext permissionContext;

    public ProjectMemberChangeContext(ProjectReportRepository reportRepository,
                                      ProjectPermissionContext permissionContext) {
        this.reportRepository = reportRepository;
        this.permissionContext = permissionContext;
    }

    /** 由「岗位 + 原员工 + 新员工」构造一次变更；无变化或新员工为空时返回 null。 */
    public Change change(String label, Employee before, Employee after) {
        if (after == null) {
            return null;
        }
        if (before != null && before.getEmpId() != null && before.getEmpId().equals(after.getEmpId())) {
            return null;
        }
        return new Change(label, before == null || before.getName() == null ? "未指定" : before.getName(),
                after.getName());
    }

    /** 写一条成员调整动态，立即生效、无接手确认；无有效变更时不落库。 */
    public void record(Project project, Employee operator, List<Change> changes) {
        List<Change> effective = changes == null
                ? List.of()
                : changes.stream().filter(Objects::nonNull).toList();
        if (effective.isEmpty() || operator == null) {
            return;
        }
        String internal = effective.stream()
                .map(change -> change.label() + "：" + change.beforeName() + " → " + change.afterName())
                .collect(Collectors.joining("；"));
        String ownerFacing = effective.stream()
                .map(change -> change.label() + "由 " + change.beforeName() + " 变更为 " + change.afterName())
                .collect(Collectors.joining("；"));

        ProjectReport report = new ProjectReport();
        report.setCompany(project.getCompany());
        report.setProject(project);
        report.setProperty(project.getProperty());
        permissionContext.apply(report, project);
        report.setType(ReportType.Notice);
        report.setTitle("项目成员调整");
        report.setContent(internal + "（操作人：" + operator.getName() + "，时间：" + LocalDateTime.now() + "，立即生效）");
        report.setCustomerSummary(ownerFacing + "（" + LocalDate.now() + " 生效）");
        report.setAuthor(operator);
        report.setAuthorRole(operator.getRole());
        report.setVisibleToCustomer(true);
        reportRepository.save(report);
    }
}
