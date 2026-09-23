package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.configure.BusinessEventType;
import com.th4.edge.decoboon.statistic.context.BusinessEventContext;
import com.th4.edge.decoboon.statistic.model.BusinessEvent;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessEventContextTest {

    @Test
    void shouldBuildRecentEventsFromRealDomainFacts() {
        TenantContext tenant = mock(TenantContext.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        CustomerFollowRepository follows = mock(CustomerFollowRepository.class);
        QuoteRepository quotes = mock(QuoteRepository.class);
        ProjectContractRepository contracts = mock(ProjectContractRepository.class);
        ReceiptRepository receipts = mock(ReceiptRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        AcceptanceRepository acceptances = mock(AcceptanceRepository.class);
        ContractChangeRepository changes = mock(ContractChangeRepository.class);
        ProjectReportRepository reports = mock(ProjectReportRepository.class);
        BusinessEventContext context = new BusinessEventContext(tenant, customers, follows, quotes, contracts,
                receipts, payments, acceptances, changes, reports);

        Company company = new Company();
        company.setComId(10L);
        Employee boss = new Employee();
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        Project project = new Project();
        project.setProjectId(20L);
        project.setName("测试项目");

        Quote quote = new Quote();
        quote.setQuoteId(30L);
        quote.setNo("Q-001");
        quote.setTitle("测试报价");
        quote.setAmount(new BigDecimal("10000"));
        quote.setSubmittedAt(LocalDateTime.of(2026, 9, 18, 10, 0));
        Receipt receipt = new Receipt();
        receipt.setReceiptId(40L);
        receipt.setProject(project);
        receipt.setAmount(new BigDecimal("3000"));
        receipt.setPayerName("刘先生");
        receipt.setReceivedAt(LocalDate.of(2026, 9, 20));

        when(tenant.requireBoss(boss)).thenReturn(boss);
        when(customers.findTop200ByCompany_ComIdOrderByCustIdDesc(10L)).thenReturn(List.of());
        when(follows.findTop200ByCompany_ComIdOrderByFollowIdDesc(10L)).thenReturn(List.of());
        when(quotes.findTop200ByCompany_ComIdOrderByQuoteIdDesc(10L)).thenReturn(List.of(quote));
        when(contracts.findTop200ByCompany_ComIdOrderByContractIdDesc(10L)).thenReturn(List.of());
        when(receipts.findTop200ByCompany_ComIdOrderByReceivedAtDesc(10L)).thenReturn(List.of(receipt));
        when(payments.findTop200ByCompany_ComIdOrderByPaidAtDesc(10L)).thenReturn(List.of());
        when(acceptances.findTop200ByCompany_ComIdOrderByAcceptanceIdDesc(10L)).thenReturn(List.of());
        when(changes.findTop200ByCompany_ComIdOrderByChangeIdDesc(10L)).thenReturn(List.of());
        when(reports.findByCompany_ComIdOrderByCreateTime_DateTimeDesc(eq(10L), any(Pageable.class)))
                .thenReturn(Page.empty());

        List<BusinessEvent> events = context.events(boss, LocalDate.of(2026, 9, 20), 10);

        assertThat(events).extracting(BusinessEvent::getType)
                .containsExactly(BusinessEventType.ReceiptReceived, BusinessEventType.QuoteSubmitted);
        assertThat(events.get(0).getProjectName()).isEqualTo("测试项目");
        assertThat(events.get(0).getAmount()).isEqualByComparingTo("3000");
        assertThat(context.events(boss, LocalDate.of(2026, 9, 20), 1)).hasSize(1);
    }
}
