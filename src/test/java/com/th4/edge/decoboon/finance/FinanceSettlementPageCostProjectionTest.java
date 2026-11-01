package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.finance.configure.SettlementStatus;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.finance.service.settlements.page.support.Page;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 结算列表必须使用项目成本的同一投影，不能用 transient 默认 true/0。 */
class FinanceSettlementPageCostProjectionTest {

    @Test
    void draftSettlementUsesBatchCostCompletenessProjection() {
        SettlementRepository repository = mock(SettlementRepository.class);
        FinanceAccessContext access = mock(FinanceAccessContext.class);
        ProjectCostContext costs = mock(ProjectCostContext.class);
        Employee employee = new Employee();
        Project project = new Project();
        project.setProjectId(42L);
        project.setName("成本不完整项目");
        Settlement settlement = new Settlement();
        settlement.setSettlementId(99L);
        settlement.setProject(project);
        settlement.setStatus(SettlementStatus.Draft);
        settlement.setTotalCost(new BigDecimal("2240.00"));

        when(access.requireReader(employee)).thenReturn(employee);
        when(access.visible(employee, Settlement.class)).thenReturn((root, query, builder) -> builder.conjunction());
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(settlement));
        when(costs.summarizeAll(any())).thenReturn(Map.of(42L,
                new ProjectCostContext.CostSummary(new BigDecimal("1760.00"), new BigDecimal("2240.00"), 1)));

        Page provider = new Page(repository, access, costs);
        ReflectionTestUtils.setField(provider, "employee", employee);
        ReflectionTestUtils.setField(provider, "current", 1);
        ReflectionTestUtils.setField(provider, "size", 20);

        List<Settlement> result = provider.call(new Settlement()).getContent();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCostComplete()).isFalse();
        assertThat(result.get(0).getCostIncompleteCount()).isEqualTo(1);
        assertThat(result.get(0).getEstimatedCost()).isEqualByComparingTo("1760.00");
        assertThat(result.get(0).getActualCost()).isEqualByComparingTo("2240.00");
        verify(costs, times(1)).summarizeAll(any());
    }
}
