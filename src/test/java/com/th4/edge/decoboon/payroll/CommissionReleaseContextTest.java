package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.payroll.configure.CommissionStatus;
import com.th4.edge.decoboon.payroll.context.CommissionReleaseContext;
import com.th4.edge.decoboon.payroll.context.PayrollCalculationContext;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionReleaseRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CommissionReleaseContextTest {

    @Test
    void shouldCreateOneReleaseForCumulativeReceiptRatio() {
        CommissionItemRepository commissionRepository = mock(CommissionItemRepository.class);
        CommissionReleaseRepository releaseRepository = mock(CommissionReleaseRepository.class);
        ReceiptRepository receiptRepository = mock(ReceiptRepository.class);
        CommissionReleaseContext context = new CommissionReleaseContext(
                commissionRepository, releaseRepository, receiptRepository, new PayrollCalculationContext());

        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(2L);
        employee.setCompany(company);
        Project project = new Project();
        project.setProjectId(3L);

        Receipt receipt = new Receipt();
        receipt.setReceiptId(10L);
        receipt.setCompany(company);
        receipt.setProject(project);
        receipt.setAmount(new BigDecimal("100000.00"));
        receipt.setReceivedAt(LocalDate.now());

        CommissionItem commission = new CommissionItem();
        commission.setCommissionId(20L);
        commission.setCompany(company);
        commission.setEmployee(employee);
        commission.setProject(project);
        commission.setContractAmount(new BigDecimal("200000.00"));
        commission.setCommissionAmount(new BigDecimal("4000.00"));
        commission.setReleasedAmount(BigDecimal.ZERO);
        commission.setStatus(CommissionStatus.Accrued);

        when(receiptRepository.findByCompany_ComIdAndProject_ProjectIdOrderByReceivedAtDesc(1L, 3L))
                .thenReturn(List.of(receipt));
        when(commissionRepository.findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(1L, 3L))
                .thenReturn(List.of(commission));
        when(releaseRepository.findByCompany_ComIdAndCommissionItem_CommissionIdAndReceipt_ReceiptId(1L, 20L, 10L))
                .thenReturn(Optional.empty());

        context.releaseForReceipt(employee, receipt);

        ArgumentCaptor<com.th4.edge.decoboon.payroll.model.CommissionRelease> captor =
                ArgumentCaptor.forClass(com.th4.edge.decoboon.payroll.model.CommissionRelease.class);
        verify(releaseRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("2000.00");
        assertThat(commission.getReleasedAmount()).isEqualByComparingTo("2000.00");
        assertThat(commission.getStatus()).isEqualTo(CommissionStatus.Released);
    }
}
