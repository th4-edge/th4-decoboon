package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.context.DesignLifecycleContext;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.plan.context.ContractPlanContext;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 变更审批、客户确认和应收同步测试。 */
class ProjectContractContextTest {

    private ProjectContractRepository contractRepository;
    private ContractChangeRepository changeRepository;
    private ReceivableRepository receivableRepository;
    private ContractLineRepository contractLineRepository;
    private ProjectRepository projectRepository;
    private CustomerRepository customerRepository;
    private PropertyRepository propertyRepository;
    private ContractChangeFulfillmentContext fulfillmentContext;
    private DesignLifecycleContext designLifecycleContext;
    private ProjectContractContext context;

    @BeforeEach
    void setUp() {
        contractRepository = mock(ProjectContractRepository.class);
        changeRepository = mock(ContractChangeRepository.class);
        receivableRepository = mock(ReceivableRepository.class);
        contractLineRepository = mock(ContractLineRepository.class);
        projectRepository = mock(ProjectRepository.class);
        customerRepository = mock(CustomerRepository.class);
        propertyRepository = mock(PropertyRepository.class);
        when(contractRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(changeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receivableRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receivableRepository.findByProjectOrderByDueDateAsc(any())).thenReturn(List.of());
        fulfillmentContext = mock(ContractChangeFulfillmentContext.class);
        designLifecycleContext = mock(DesignLifecycleContext.class);
        context = new ProjectContractContext(
                contractRepository,
                changeRepository,
                contractLineRepository,
                receivableRepository,
                projectRepository,
                customerRepository,
                mock(QuoteLineRepository.class),
                propertyRepository,
                mock(PropertyContext.class),
                mock(FinanceAccessContext.class),
                new ProjectPermissionContext(),
                fulfillmentContext,
                mock(ProjectContext.class),
                mock(ContractPlanContext.class),
                designLifecycleContext);
    }

    @Test
    void quoteWithoutPropertyMustNotCreatePropertyOrContract() {
        Company company = new Company();
        company.setComId(1L);
        Customer customer = new Customer();
        customer.setCustId(11L);
        customer.setCompany(company);
        Quote quote = new Quote();
        quote.setQuoteId(12L);
        quote.setCompany(company);
        quote.setCustomer(customer);
        when(contractRepository.findByQuote_QuoteId(12L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> context.createFromQuote(
                quote, new Employee(), null, null, null, "无房屋不能建合同"))
                .hasMessageContaining("报价缺少客户房屋");

        verify(propertyRepository, never()).save(any(Property.class));
        verify(contractRepository, never()).save(any(ProjectContract.class));
    }

    @Test
    void customerStatusMustFollowSignedAndEffectiveContractSteps() {
        Company company = new Company();
        company.setComId(1L);
        Customer customer = new Customer();
        customer.setCustId(11L);
        customer.setCompany(company);
        customer.setStatus(CustStatus.Quoted);
        Quote quote = new Quote();
        quote.setQuoteId(12L);
        quote.setCompany(company);
        quote.setCustomer(customer);
        ProjectContract contract = new ProjectContract();
        contract.setContractId(20L);
        contract.setCompany(company);
        contract.setQuote(quote);
        contract.setCustomer(customer);
        contract.setBaselineAmount(new BigDecimal("100000.00"));
        contract.setCurrentAmount(new BigDecimal("100000.00"));

        context.markSigned(contract);

        assertThat(contract.getStatus().name()).isEqualTo("Signed");
        assertThat(customer.getStatus()).isEqualTo(CustStatus.Signed);

        context.markEffective(contract);

        assertThat(contract.getStatus().name()).isEqualTo("Effective");
        assertThat(customer.getStatus()).isEqualTo(CustStatus.Serving);
    }


    @Test
    void signingMustFreezeCurrentCatalogCostIntoContractLines() {
        Company company = new Company();
        company.setComId(1L);
        com.th4.edge.decoboon.catalog.model.CatalogItem catalogItem =
                new com.th4.edge.decoboon.catalog.model.CatalogItem();
        catalogItem.setCostPrice(new BigDecimal("88.00"));
        com.th4.edge.decoboon.quote.model.QuoteLine quoteLine = new com.th4.edge.decoboon.quote.model.QuoteLine();
        quoteLine.setCatalogItem(catalogItem);
        com.th4.edge.decoboon.project.model.ContractLine contractLine =
                new com.th4.edge.decoboon.project.model.ContractLine();
        contractLine.setSourceQuoteLine(quoteLine);
        contractLine.setType(com.th4.edge.decoboon.catalog.configure.ItemType.Material);
        contractLine.setQuantity(new BigDecimal("2"));
        contractLine.setPricingMode(com.th4.edge.decoboon.catalog.configure.PricingMethod.MaterialOnly);
        ProjectContract contract = new ProjectContract();
        contract.setContractId(30L);
        contract.setStatus(com.th4.edge.decoboon.project.configure.ContractStatus.PendingSign);
        contract.setBaselineAmount(new BigDecimal("100000.00"));
        contract.setCurrentAmount(new BigDecimal("100000.00"));
        com.th4.edge.decoboon.quote.model.Quote quote = new com.th4.edge.decoboon.quote.model.Quote();
        com.th4.edge.decoboon.customer.model.Customer customer = new com.th4.edge.decoboon.customer.model.Customer();
        customer.setCustId(40L);
        quote.setCustomer(customer);
        contract.setQuote(quote);
        when(contractLineRepository.findByContractOrderBySortAsc(contract)).thenReturn(List.of(contractLine));

        ProjectContract signed = context.markSigned(contract, java.time.LocalDate.of(2026, 9, 18));

        assertThat(signed.getStatus()).isEqualTo(com.th4.edge.decoboon.project.configure.ContractStatus.Signed);
        assertThat(contractLine.getCost()).isEqualByComparingTo("176.00");
    }

    @Test
    void signingMustKeepQuoteCostSnapshotWhenContractLineCostIsMissing() {
        Company company = new Company();
        company.setComId(1L);
        com.th4.edge.decoboon.catalog.model.CatalogItem catalogItem =
                new com.th4.edge.decoboon.catalog.model.CatalogItem();
        catalogItem.setCostPrice(new BigDecimal("88.00"));
        com.th4.edge.decoboon.quote.model.QuoteLine quoteLine = new com.th4.edge.decoboon.quote.model.QuoteLine();
        quoteLine.setCatalogItem(catalogItem);
        quoteLine.setCost(new BigDecimal("32000.00"));
        com.th4.edge.decoboon.project.model.ContractLine contractLine =
                new com.th4.edge.decoboon.project.model.ContractLine();
        contractLine.setSourceQuoteLine(quoteLine);
        contractLine.setType(com.th4.edge.decoboon.catalog.configure.ItemType.Material);
        contractLine.setCost(BigDecimal.ZERO);
        contractLine.setQuantity(new BigDecimal("2"));
        contractLine.setPricingMode(com.th4.edge.decoboon.catalog.configure.PricingMethod.MaterialOnly);
        ProjectContract contract = new ProjectContract();
        contract.setContractId(31L);
        contract.setStatus(com.th4.edge.decoboon.project.configure.ContractStatus.PendingSign);
        contract.setBaselineAmount(new BigDecimal("100000.00"));
        contract.setCurrentAmount(new BigDecimal("100000.00"));
        com.th4.edge.decoboon.quote.model.Quote quote = new com.th4.edge.decoboon.quote.model.Quote();
        com.th4.edge.decoboon.customer.model.Customer customer = new com.th4.edge.decoboon.customer.model.Customer();
        customer.setCustId(41L);
        quote.setCustomer(customer);
        contract.setQuote(quote);
        when(contractLineRepository.findByContractOrderBySortAsc(contract)).thenReturn(List.of(contractLine));

        ProjectContract signed = context.markSigned(contract, java.time.LocalDate.of(2026, 9, 18));

        assertThat(signed.getStatus()).isEqualTo(com.th4.edge.decoboon.project.configure.ContractStatus.Signed);
        assertThat(contractLine.getCost()).isEqualByComparingTo("32000.00");
    }

    @Test
    void finalSignedAmountMustOverrideQuoteBaselineAndKeepSignedSnapshot() {
        Company company = new Company();
        company.setComId(1L);
        Customer customer = new Customer();
        customer.setCustId(42L);
        customer.setCompany(company);
        Quote quote = new Quote();
        quote.setCustomer(customer);
        ProjectContract contract = new ProjectContract();
        contract.setContractId(32L);
        contract.setCompany(company);
        contract.setQuote(quote);
        contract.setBaselineAmount(new BigDecimal("100000.00"));
        contract.setCurrentAmount(new BigDecimal("100000.00"));
        contract.setStatus(com.th4.edge.decoboon.project.configure.ContractStatus.PendingSign);
        when(contractLineRepository.findByContractOrderBySortAsc(contract)).thenReturn(List.of());

        ProjectContract signed = context.markSigned(
                contract, java.time.LocalDate.of(2026, 9, 18), new BigDecimal("95000.00"));

        assertThat(signed.getBaselineAmount()).isEqualByComparingTo("100000.00");
        assertThat(signed.getSignedAmount()).isEqualByComparingTo("95000.00");
        assertThat(signed.getCurrentAmount()).isEqualByComparingTo("95000.00");
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
        contract.setSignedAmount(new BigDecimal("100000"));
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
        assertThat(contract.getSignedAmount()).isEqualByComparingTo("100000");
        assertThat(project.getContractAmount()).isEqualByComparingTo("105000");
        verify(receivableRepository).save(any(Receivable.class));
        verify(fulfillmentContext).apply(change, operator);
    }

    @Test
    void shouldActivateChangeWhenApproved() {
        Company company = new Company();
        company.setComId(1L);
        Project project = new Project();
        project.setProjectId(10L);
        project.setCompany(company);
        project.setProperty(new Property());
        project.setContractAmount(new BigDecimal("100000"));
        ProjectContract contract = new ProjectContract();
        contract.setContractId(20L);
        contract.setCompany(company);
        contract.setProject(project);
        contract.setProperty(project.getProperty());
        contract.setBaselineAmount(new BigDecimal("100000"));
        contract.setSignedAmount(new BigDecimal("100000"));
        contract.setCurrentAmount(new BigDecimal("100000"));
        ContractChange change = new ContractChange();
        change.setChangeId(30L);
        change.setCompany(company);
        change.setProject(project);
        change.setContract(contract);
        change.setProperty(project.getProperty());
        change.setStatus(ContractChangeStatus.Pending);
        change.setCustomerConfirmed(true);
        change.setCustomerConfirmedAt(java.time.LocalDateTime.now().minusDays(1));
        change.setAmount(new BigDecimal("5000"));
        Employee operator = new Employee();
        operator.setEmpId(1L);

        ContractChange effective = context.approve(change, operator, true, "同意变更");

        assertThat(effective.getStatus()).isEqualTo(ContractChangeStatus.CustomerConfirmed);
        assertThat(effective.getApprovedBy()).isSameAs(operator);
        assertThat(effective.getCustomerConfirmedAt()).isBefore(java.time.LocalDateTime.now());
        assertThat(contract.getCurrentAmount()).isEqualByComparingTo("105000");
        assertThat(project.getContractAmount()).isEqualByComparingTo("105000");
        verify(receivableRepository).save(any(Receivable.class));
        verify(fulfillmentContext).apply(change, operator);
    }

    @Test
    void shouldRejectApprovalWithoutCustomerSignOff() {
        Company company = new Company();
        company.setComId(1L);
        Project project = new Project();
        project.setProjectId(10L);
        project.setCompany(company);
        ProjectContract contract = new ProjectContract();
        contract.setContractId(20L);
        contract.setCompany(company);
        contract.setProject(project);
        contract.setCurrentAmount(new BigDecimal("100000"));
        ContractChange change = new ContractChange();
        change.setChangeId(30L);
        change.setCompany(company);
        change.setProject(project);
        change.setContract(contract);
        change.setStatus(ContractChangeStatus.Pending);
        change.setCustomerConfirmed(false);
        change.setAmount(new BigDecimal("5000"));

        assertThatThrownBy(() -> context.approve(change, new Employee(), true, "同意"))
                .hasMessageContaining("缺少客户线下确认");
        verify(fulfillmentContext, never()).apply(any(), any());
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
