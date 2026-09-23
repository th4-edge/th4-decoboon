package com.th4.edge.decoboon.project.service.acceptances.round.page.support;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.AcceptanceRound;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.AcceptanceRoundRepository;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 阶段验收轮次时间线。
 *
 * <p>append-only：提交验收/复验追加一行，业主确认或驳回回填本轮结论，历史轮次不可修改。</p>
 */
@Note("阶段验收轮次时间线")
@Properties(allow = {
        @AllowProperties(model = AcceptanceRound.class, properties = {
                "roundId", "roundNo", "result", "rectificationNote", "rectificationEvidenceUrls",
                "evidenceUrls", "issues", "conclusion", "submittedBy", "submittedAt",
                "confirmedAt", "rectificationOwner", "rectificationDueDate", "rectificationType",
                "rectificationIssueImages", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/acceptance/round/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.acceptances.round.page.Page {

    @AutoInject @NotNull private Long acceptanceId;
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
    public List<AcceptanceRound> call(Acceptance request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<Acceptance> spec = (root, query, builder) -> builder.equal(root.get("acceptanceId"), acceptanceId);
        Acceptance acceptance = acceptanceRepository.findOne(spec.and(
                        accessContext.visibleChild(employee, Acceptance.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "验收单不存在或无权访问")));
        accessContext.requireVisible(acceptance.getProject(), current);
        return acceptanceRoundRepository.findByAcceptanceOrderByRoundNoDesc(acceptance);
    }
}
