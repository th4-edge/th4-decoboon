package com.th4.edge.decoboon.finance.service.settlements.retentionrelease.support;

import com.th4.edge.decoboon.finance.configure.SettlementStatus;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
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

import java.time.LocalDate;

/** 质保到期释放。 */
@Note("质保金释放")
@Properties(allow = {
        @AllowProperties(model = Settlement.class,
        properties = {"settlementId", "project", "finalAmount", "retentionRate", "retentionAmount",
                "retentionReleaseAt", "retentionReleasedAt", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class, properties = {"projectId", "no", "name", "siteAddr", "status", "contractAmount"})
})
@Service("finance/settlement/retention-release")
@Scope(ScopeTypes.PROTOTYPE)
public class Release implements com.th4.edge.decoboon.finance.service.settlements.retentionrelease.Release {
    @User private Employee employee;
    @AutoInject @NotNull private Long settlementId;
    @AutoInject private LocalDate releasedAt;
    private final SettlementRepository settlementRepository;
    private final FinanceAccessContext accessContext;

    public Release(SettlementRepository settlementRepository, FinanceAccessContext accessContext) {
        this.settlementRepository = settlementRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Settlement call(Settlement request) {
        Employee current = accessContext.requireEditor(employee);
        Specification<Settlement> byId = (root, query, builder) ->
                builder.equal(root.get("settlementId"), settlementId);
        Settlement settlement = settlementRepository.findOne(
                        byId.and(accessContext.visible(current, Settlement.class)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "结算单不存在或无权访问")));
        if (settlement.getStatus() != SettlementStatus.RetentionPending) {
            throw new DefinedException(new DefaultErrorCode("400", "当前结算单没有待释放质保金"));
        }
        LocalDate release = releasedAt == null ? LocalDate.now() : releasedAt;
        if (settlement.getRetentionReleaseAt() != null && release.isBefore(settlement.getRetentionReleaseAt())) {
            throw new DefinedException(new DefaultErrorCode("400", "质保期尚未到期，不能提前释放"));
        }
        settlement.setStatus(SettlementStatus.RetentionReleased);
        settlement.setRetentionReleasedAt(release);
        return settlementRepository.save(settlement);
    }
}
