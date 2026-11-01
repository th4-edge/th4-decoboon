package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.context.DesignLifecycleContext;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.plan.context.ContractPlanContext;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 合同读模型必须以一条成本聚合替代全量合同行加载，并保持成本完整性原口径。 */
class ProjectContractEnrichCostAggregateTest {

    private final ProjectContractRepository contractRepository = mock(ProjectContractRepository.class);
    private final ContractChangeRepository changeRepository = mock(ContractChangeRepository.class);
    private final ContractLineRepository lineRepository = mock(ContractLineRepository.class);
    private final ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final QuoteLineRepository quoteLineRepository = mock(QuoteLineRepository.class);
    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final PropertyContext propertyContext = mock(PropertyContext.class);
    private final FinanceAccessContext financeAccessContext = mock(FinanceAccessContext.class);
    private final ProjectPermissionContext permissionContext = mock(ProjectPermissionContext.class);
    private final ContractChangeFulfillmentContext fulfillmentContext = mock(ContractChangeFulfillmentContext.class);
    private final ProjectContext projectContext = mock(ProjectContext.class);
    private final ContractPlanContext contractPlanContext = mock(ContractPlanContext.class);
    private final DesignLifecycleContext designLifecycleContext = mock(DesignLifecycleContext.class);
    private final ContractBusinessStateContext businessStateContext = mock(ContractBusinessStateContext.class);

    private ProjectContractContext context;
    private ProjectContract contract;

    @BeforeEach
    void setUp() {
        context = new ProjectContractContext(contractRepository, changeRepository, lineRepository,
                receivableRepository, projectRepository, customerRepository, quoteLineRepository,
                propertyRepository, propertyContext, financeAccessContext, permissionContext,
                fulfillmentContext, projectContext, contractPlanContext, designLifecycleContext,
                businessStateContext);
        contract = new ProjectContract();
        contract.setContractId(81L);
        when(changeRepository.findByContractOrderByChangeIdDesc(contract)).thenReturn(List.of());
    }

    @Test
    void completeCostSummaryMustUseSingleAggregate() {
        when(lineRepository.summarizeCostByContract(81L)).thenReturn(List.<Object[]>of(
                new Object[]{3L, new BigDecimal("286.50"), 0L}));

        ProjectContract result = context.enrich(contract);

        assertThat(result.getSignedCost()).isEqualByComparingTo("286.50");
        assertThat(result.getCostComplete()).isTrue();
        verify(lineRepository, never()).findByContractOrderBySortAsc(any(ProjectContract.class));
    }

    @Test
    void incompleteRequiredCostMustRemainIncomplete() {
        when(lineRepository.summarizeCostByContract(81L)).thenReturn(List.<Object[]>of(
                new Object[]{4L, new BigDecimal("320.00"), 1L}));

        ProjectContract result = context.enrich(contract);

        assertThat(result.getSignedCost()).isEqualByComparingTo("320.00");
        assertThat(result.getCostComplete()).isFalse();
        verify(lineRepository, never()).findByContractOrderBySortAsc(any(ProjectContract.class));
    }

    @Test
    void emptyContractMustNotBeReportedAsComplete() {
        when(lineRepository.summarizeCostByContract(81L)).thenReturn(List.<Object[]>of(
                new Object[]{0L, BigDecimal.ZERO, 0L}));

        ProjectContract result = context.enrich(contract);

        assertThat(result.getSignedCost()).isEqualByComparingTo("0.00");
        assertThat(result.getCostComplete()).isFalse();
        verify(lineRepository, never()).findByContractOrderBySortAsc(any(ProjectContract.class));
    }
}
