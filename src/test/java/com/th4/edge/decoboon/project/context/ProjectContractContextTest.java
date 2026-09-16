package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 变更审批、客户确认和应收同步测试。 */
class ProjectContractContextTest {

    private ProjectContractRepository contractRepository;
    private ContractChangeRepository changeRepository;
    private ReceivableRepository receivableRepository;
    private ProjectRepository projectRepository;
    private ContractChangeFulfillmentContext fulfillmentContext;
    private ProjectContractContext context;

    @BeforeEach
    void setUp() {
        contractRepository = mock(ProjectContractRepository.class);
        changeRepository = mock(ContractChangeRepository.class);
        receivableRepository = mock(ReceivableRepository.class);
        projectRepository = mock(ProjectRepository.class);
        when(contractRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(changeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receivableRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receivableRepository.findByProjectOrderByDueDateAsc(any())).thenReturn(List.of());
        fulfillmentContext = mock(ContractChangeFulfillmentContext.class);
        context = new ProjectContractContext(
                contractRepository,
                changeRepository,
                mock(ContractLineRepository.class),
                receivableRepository,
                projectRepository,
                mock(QuoteLineRepository.class),
                mock(PropertyRepository.class),
                mock(PropertyContext.class),
                mock(FinanceAccessContext.class),
                new ProjectPermissionContext(),
                fulfillmentContext,
                mock(ProjectContext.class));
    }

    @Test
    void shouldApplyApprovedChangeAndCreateReceivableAfterCustomerConfirmation() {
        Company company = new Company();
        company.setComId(1L);
        Property property = new Property();
        Project project = new Project();
        project.setProjectId(10L);
        project.setCompany(company);
        project.setProperty(property);
        project.setContractAmount(new BigDecimal("100000"));
        ProjectContract contract = new ProjectContract();
        contract.setContractId(20L);
        contract.setCompany(company);
        contract.setProject(project);
        contract.setProperty(property);
        contract.setBaselineAmount(new BigDecimal("100000"));
        contract.setCurrentAmount(new BigDecimal("100000"));
        ContractChange change = new ContractChange();
        change.setChangeId(30L);
        change.setCompany(company);
        change.setProject(project);
        change.setContract(contract);
        change.setProperty(property);
        change.setStatus(ContractChangeStatus.Approved);
        change.setAmount(new BigDecimal("5000"));
        Employee operator = new Employee();
        operator.setEmpId(1L);

        ContractChange confirmed = context.confirmCustomer(change, operator);

        assertThat(confirmed.getStatus()).isEqualTo(ContractChangeStatus.CustomerConfirmed);
        assertThat(confirmed.getCustomerConfirmed()).isTrue();
        assertThat(contract.getCurrentAmount()).isEqualByComparingTo("105000");
        assertThat(project.getContractAmount()).isEqualByComparingTo("105000");
        verify(receivableRepository).save(any(Receivable.class));
        verify(fulfillmentContext).apply(change, operator);
    }

    @Test
    void shouldReturnConfirmedChangeWithoutDuplicatingSettlement() {
        Company company = new Company();
        company.setComId(1L);
        Project project = new Project();
        project.setProjectId(10L);
        project.setCompany(company);
        ProjectContract contract = new ProjectContract();
        contract.setContractId(20L);
        contract.setCompany(company);
        contract.setCurrentAmount(new BigDecimal("100000"));
        ContractChange change = new ContractChange();
        change.setChangeId(30L);
        change.setCompany(company);
        change.setProject(project);
        change.setContract(contract);
        change.setStatus(ContractChangeStatus.CustomerConfirmed);
        change.setCustomerConfirmed(true);
        change.setAmount(new BigDecimal("5000"));

        ContractChange confirmed = context.confirmCustomer(change, new Employee());

        assertThat(confirmed).isSameAs(change);
        assertThat(contract.getCurrentAmount()).isEqualByComparingTo("100000");
        verify(changeRepository, org.mockito.Mockito.never()).save(any());
        verify(fulfillmentContext, org.mockito.Mockito.never()).apply(any(), any());
    }

}
