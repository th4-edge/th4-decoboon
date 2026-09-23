package com.th4.edge.decoboon.customer.service.acceptance.round.page.support;

import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.AcceptanceRound;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.AcceptanceRoundRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 业主可见的阶段验收轮次时间线实现。 */
@Note("业主验收轮次时间线")
@Properties(allow = {
        @AllowProperties(model = AcceptanceRound.class, properties = {
                "roundId", "roundNo", "result", "rectificationNote", "rectificationEvidenceUrls",
                "evidenceUrls", "issues", "conclusion", "submittedBy", "submittedAt",
                "confirmedAt", "rectificationOwner", "rectificationDueDate", "rectificationType",
                "rectificationIssueImages", "createTime"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Employee.class,
                properties = {"empId", "name", "role"})
})
@Service("customer/acceptance/round/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.acceptance.round.page.Page {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long acceptanceId;

    private final CustomerAccessContext accessContext;
    private final AcceptanceRepository acceptanceRepository;
    private final AcceptanceRoundRepository acceptanceRoundRepository;

    public Page(CustomerAccessContext accessContext,
                AcceptanceRepository acceptanceRepository,
                AcceptanceRoundRepository acceptanceRoundRepository) {
        this.accessContext = accessContext;
        this.acceptanceRepository = acceptanceRepository;
        this.acceptanceRoundRepository = acceptanceRoundRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcceptanceRound> call(Acceptance request) {
        Credentials credential = accessContext.requireCredential(principal);
        Acceptance acceptance = acceptanceRepository.findForCustomerAccess(
                        acceptanceId, credential.getCreId(), CustomerAccessStatus.Active)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "验收单不存在或无权访问")));
        return acceptanceRoundRepository.findByAcceptanceOrderByRoundNoDesc(acceptance);
    }
}
