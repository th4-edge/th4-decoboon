package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.context.ProjectFinanceProgressContext;
import com.th4.edge.decoboon.project.model.Project;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 项目工地回款进度只读投影测试。 */
class ProjectFinanceProgressContextTest {

    private final ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
    private final ProjectFinanceProgressContext context = new ProjectFinanceProgressContext(receivableRepository);

    @Test
    void shouldAggregateReceivableAndReceivedAmountsByProject() {
        Project first = project(1L);
        Project second = project(2L);
        when(receivableRepository.findByProject_ProjectIdInOrderByDueDateAsc(List.of(1L, 2L)))
                .thenReturn(List.of(
                        receivable(first, "100.10", "30.05"),
                        receivable(first, "50.00", "20.00"),
                        receivable(second, "80.00", "80.00")));

        context.enrich(List.of(first, second));

        assertThat(first.getReceivableAmount()).isEqualByComparingTo("150.10");
        assertThat(first.getReceivedAmount()).isEqualByComparingTo("50.05");
        assertThat(second.getReceivableAmount()).isEqualByComparingTo("80.00");
        assertThat(second.getReceivedAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void shouldKeepZeroWhenNoReceivablePlanExists() {
        Project project = project(3L);
        when(receivableRepository.findByProject_ProjectIdInOrderByDueDateAsc(List.of(3L)))
                .thenReturn(List.of());

        context.enrich(List.of(project));

        assertThat(project.getReceivableAmount()).isEqualByComparingTo("0");
        assertThat(project.getReceivedAmount()).isEqualByComparingTo("0");
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setProjectId(id);
        return project;
    }

    private Receivable receivable(Project project, String amount, String received) {
        Receivable receivable = new Receivable();
        receivable.setProject(project);
        receivable.setAmount(new BigDecimal(amount));
        receivable.setReceivedAmount(new BigDecimal(received));
        return receivable;
    }
}
