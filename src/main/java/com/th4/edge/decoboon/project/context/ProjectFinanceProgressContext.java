package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目回款进度只读投影。
 *
 * <p>项目工地列表和详情只展示应收计划汇总与已收金额，供项目参与角色查看回款进度；
 * 不暴露应付、成本、毛利、利润等财务明细。金额完全来自 {@link Receivable}，不在前端反推。</p>
 */
@Note("项目回款进度只读投影")
@Component
public class ProjectFinanceProgressContext {

    private final ReceivableRepository receivableRepository;

    public ProjectFinanceProgressContext(ReceivableRepository receivableRepository) {
        this.receivableRepository = receivableRepository;
    }

    public void enrich(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }
        List<Long> projectIds = projects.stream()
                .map(Project::getProjectId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (projectIds.isEmpty()) {
            return;
        }

        Map<Long, Amounts> amountsByProject = new HashMap<>();
        for (Receivable receivable : receivableRepository.findByProject_ProjectIdInOrderByDueDateAsc(projectIds)) {
            if (receivable.getProject() == null || receivable.getProject().getProjectId() == null) {
                continue;
            }
            Long projectId = receivable.getProject().getProjectId();
            Amounts amounts = amountsByProject.getOrDefault(projectId, Amounts.ZERO);
            amountsByProject.put(projectId, new Amounts(
                    amounts.receivable().add(value(receivable.getAmount())),
                    amounts.received().add(value(receivable.getReceivedAmount()))));
        }

        for (Project project : projects) {
            Amounts amounts = amountsByProject.getOrDefault(project.getProjectId(), Amounts.ZERO);
            project.setReceivableAmount(amounts.receivable());
            project.setReceivedAmount(amounts.received());
        }
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private record Amounts(BigDecimal receivable, BigDecimal received) {
        private static final Amounts ZERO = new Amounts(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
