package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.finance.context.PartnerCreditContext;
import com.th4.edge.decoboon.finance.model.PartnerCredit;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PartnerCreditRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.tenant.user.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 供应商往来余额自动抵扣应付（退货余款延伸规则）。 */
class PartnerCreditContextTest {

    private PartnerCreditRepository creditRepository;
    private PartnerCreditContext context;
    private Company company;
    private Partner supplier;

    @BeforeEach
    void setUp() {
        creditRepository = mock(PartnerCreditRepository.class);
        var payableRepository = mock(PayableRepository.class);
        when(creditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payableRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        context = new PartnerCreditContext(creditRepository, payableRepository);

        company = new Company();
        company.setComId(1L);
        supplier = new Partner();
        supplier.setPartnerId(50L);
        supplier.setCompany(company);
    }

    @Test
    void shouldConsumeCreditsFifoAndReducePayable() {
        PartnerCredit first = credit(100L, "300");
        PartnerCredit second = credit(200L, "500");
        when(creditRepository.findByCompany_ComIdAndPartner_PartnerIdOrderByPartnerCreditIdDesc(1L, 50L))
                .thenReturn(List.of(second, first));

        BigDecimal used = context.applyTo(payable("1000", "0"));

        assertThat(used).isEqualByComparingTo("800");
        assertThat(first.getAmount()).isEqualByComparingTo("0");
        assertThat(second.getAmount()).isEqualByComparingTo("0");
    }

    @Test
    void shouldKeepRemainingCreditWhenPayableIsSmaller() {
        PartnerCredit first = credit(100L, "300");
        PartnerCredit second = credit(200L, "500");
        when(creditRepository.findByCompany_ComIdAndPartner_PartnerIdOrderByPartnerCreditIdDesc(1L, 50L))
                .thenReturn(List.of(second, first));

        BigDecimal used = context.applyTo(payable("200", "0"));

        assertThat(used).isEqualByComparingTo("200");
        // 按形成先后抵扣：最早一笔（id 100）先被使用 200，剩余 100
        assertThat(first.getAmount()).isEqualByComparingTo("100");
        assertThat(second.getAmount()).isEqualByComparingTo("500");
    }

    @Test
    void shouldSkipWhenPayableAlreadyPaid() {
        when(creditRepository.findByCompany_ComIdAndPartner_PartnerIdOrderByPartnerCreditIdDesc(1L, 50L))
                .thenReturn(List.of(credit(100L, "300")));

        BigDecimal used = context.applyTo(payable("1000", "1000"));

        assertThat(used).isEqualByComparingTo("0");
    }

    private PartnerCredit credit(Long id, String amount) {
        PartnerCredit credit = new PartnerCredit();
        credit.setPartnerCreditId(id);
        credit.setCompany(company);
        credit.setPartner(supplier);
        credit.setAmount(new BigDecimal(amount));
        return credit;
    }

    private Payable payable(String amount, String paid) {
        Payable payable = new Payable();
        payable.setPayableId(10L);
        payable.setCompany(company);
        payable.setPayeePartner(supplier);
        payable.setName("材料采购 · 测试供应商");
        payable.setAmount(new BigDecimal(amount));
        payable.setPaidAmount(new BigDecimal(paid));
        return payable;
    }
}
