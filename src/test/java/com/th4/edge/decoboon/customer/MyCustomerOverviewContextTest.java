package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.PortraitDecisionRole;
import com.th4.edge.decoboon.customer.configure.PortraitHouseCondition;
import com.th4.edge.decoboon.customer.configure.PortraitPurpose;
import com.th4.edge.decoboon.customer.configure.PortraitStartWindow;
import com.th4.edge.decoboon.customer.configure.PortraitStyle;
import com.th4.edge.decoboon.customer.context.CustomerGradeContext;
import com.th4.edge.decoboon.customer.context.CustomerInsightContext;
import com.th4.edge.decoboon.customer.context.CustomerLossContext;
import com.th4.edge.decoboon.customer.context.CustomerTagContext;
import com.th4.edge.decoboon.customer.context.MyCustomerOverviewContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerOverviewReport;
import com.th4.edge.decoboon.customer.model.CustomerPortrait;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerPortraitRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyCustomerOverviewContextTest {

    @Test
    void reportComputesGradeAfterPortraitSave() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        CustomerPortraitRepository portraitRepository = mock(CustomerPortraitRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        ProjectContractRepository contractRepository = mock(ProjectContractRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        CustomerFollowRepository followRepository = mock(CustomerFollowRepository.class);
        CustomerTagContext tagContext = mock(CustomerTagContext.class);

        MyCustomerOverviewContext context = new MyCustomerOverviewContext(
                customerRepository,
                portraitRepository,
                quoteRepository,
                contractRepository,
                measureRepository,
                new CustomerInsightContext(),
                followRepository,
                tagContext,
                new CustomerGradeContext(),
                mock(CustomerLossContext.class));

        Company company = new Company();
        company.setComId(10L);

        Employee designer = new Employee();
        designer.setEmpId(105L);
        designer.setName("设计师");
        designer.setRole(EmployeeRole.Designer);
        designer.setCompany(company);

        Customer customer = new Customer();
        customer.setCustId(101L);
        customer.setCompany(company);
        customer.setName("画像客户");
        customer.setTelNo("13900000000");
        customer.setStatus(CustStatus.Following);
        customer.setDesigner(designer);
        customer.setBudget(new BigDecimal("200000"));
        customer.setLastFollowAt(LocalDateTime.now().minusDays(1));

        CustomerPortrait portrait = new CustomerPortrait();
        portrait.setCustomer(customer);
        portrait.setDecisionRole(PortraitDecisionRole.Self);
        portrait.setPurpose(PortraitPurpose.Improve);
        portrait.setHouseCondition(PortraitHouseCondition.Old);
        portrait.setStartWindow(PortraitStartWindow.WithinOneMonth);
        portrait.setStyle(PortraitStyle.Cream);

        when(customerRepository.countStatusForDesigner(10L, 105L))
                .thenReturn(List.<Object[]>of(new Object[]{CustStatus.Following, 1L}));
        when(customerRepository.findPageForDesigner(any(), any()))
                .thenReturn(new PageImpl<>(List.of(customer)));
        when(portraitRepository.findByCompany_ComIdAndCustomer_CustIdIn(anyLong(), anyCollection()))
                .thenReturn(List.of(portrait));
        when(quoteRepository.findByCompany_ComIdAndCustomer_CustIdInOrderByQuoteIdDesc(anyLong(), anyCollection()))
                .thenReturn(List.of());
        when(contractRepository.findByCompany_ComIdAndCustomer_CustIdInOrderByContractIdDesc(anyLong(), anyCollection()))
                .thenReturn(List.of());
        when(measureRepository.findByCompany_ComIdAndCustomer_CustIdInOrderByMeasureIdDesc(anyLong(), anyCollection()))
                .thenReturn(List.of());
        when(followRepository.countGroupByCustomer(anyLong(), anyCollection()))
                .thenReturn(List.of());
        when(tagContext.assembleBatch(anyLong(), anyCollection(), any(), any()))
                .thenReturn(Map.of());

        CustomerOverviewReport report = context.report(designer, 1, 20, null, null, "risk", null);

        assertThat(report.getCustomers()).hasSize(1);
        assertThat(report.getCustomers().get(0).getPortrait()).isEqualTo(portrait);
        assertThat(report.getCustomers().get(0).getGrade()).isNotNull();
        assertThat(report.getCustomers().get(0).getGrade().getScore()).isPositive();
    }
}
