package com.th4.edge.decoboon.project.service.acceptances.rectify.page.support;

import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.AcceptanceRectifyItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.AcceptanceRoundRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 整改中阶段清单。
 *
 * <p>只返回当前处于驳回（整改中）的阶段验收，并给出轮次、整改责任人、截止日期与逾期天数；
 * 整改循环不改变金额、成本与账务，本清单为只读质量事实。</p>
 */
@Note("整改中阶段清单")
@Description("按数据范围列出当前处于整改中的阶段验收，含轮次、整改责任人与逾期天数")
@Service("project/acceptance/rectify/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.acceptances.rectify.page.Page {

    @User private Employee employee;
    private final AcceptanceRepository acceptanceRepository;
    private final AcceptanceRoundRepository acceptanceRoundRepository;
    private final ProjectAccessContext accessContext;

    public Page(AcceptanceRepository acceptanceRepository,
                AcceptanceRoundRepository acceptanceRoundRepository,
                ProjectAccessContext accessContext) {
        this.acceptanceRepository = acceptanceRepository;
        this.acceptanceRoundRepository = acceptanceRoundRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcceptanceRectifyItem> call(Acceptance request) {
        accessContext.requireEmployee(employee);
        List<Acceptance> acceptances = acceptanceRepository.findAll(
                accessContext.visibleChild(employee, Acceptance.class, "project")
                        .and((root, query, builder) -> builder.equal(root.get("status"), AcceptStatus.Rejected)));
        List<AcceptanceRectifyItem> items = new ArrayList<>();
        LocalDate today = LocalDate.now();
        // 数据范围已在 visibleChild 的查询条件中强制命中，此处只做投影。
        for (Acceptance acceptance : acceptances) {
            items.add(toItem(acceptance, today));
        }
        return items;
    }

    private AcceptanceRectifyItem toItem(Acceptance acceptance, LocalDate today) {
        AcceptanceRectifyItem item = new AcceptanceRectifyItem();
        Project project = acceptance.getProject();
        ProjectStage stage = acceptance.getStage();
        item.setAcceptanceId(acceptance.getAcceptanceId());
        item.setProjectId(project == null ? null : project.getProjectId());
        item.setProjectNo(project == null ? null : project.getNo());
        item.setProjectName(project == null ? null : project.getName());
        item.setStageId(stage == null ? null : stage.getStageId());
        item.setStageName(stage == null ? null : stage.getName());
        item.setRoundNo((int) acceptanceRoundRepository.countByAcceptance(acceptance));
        item.setConclusion(acceptance.getConclusion());
        item.setIssues(acceptance.getIssues() == null ? null : new ArrayList<>(acceptance.getIssues()));
        item.setRectificationOwnerName(
                acceptance.getRectificationOwner() == null ? null : acceptance.getRectificationOwner().getName());
        item.setRectificationType(acceptance.getRectificationType() == null
                ? null : acceptance.getRectificationType().name());
        item.setRectificationTypeName(acceptance.getRectificationType() == null
                ? null : acceptance.getRectificationType().getName());
        item.setRectificationIssueImageCount(acceptance.getRectificationIssueImages() == null
                ? 0 : acceptance.getRectificationIssueImages().size());
        item.setRectificationDueDate(acceptance.getRectificationDueDate());
        item.setConfirmedAt(acceptance.getConfirmedAt());
        int overdue = 0;
        if (acceptance.getRectificationDueDate() != null && acceptance.getRectificationDueDate().isBefore(today)) {
            overdue = (int) ChronoUnit.DAYS.between(acceptance.getRectificationDueDate(), today);
        }
        item.setOverdueDays(overdue);
        item.setNextAction(overdue > 0 ? "整改已逾期，尽快完成并提交复验" : "完成整改后提交复验");
        return item;
    }
}
