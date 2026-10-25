package com.th4.edge.decoboon.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.document.context.VoucherContext;
import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.document.repository.VoucherRecordRepository;
import com.th4.edge.decoboon.finance.configure.ExpensePaymentMode;
import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.ExpenseVoucherProjectionContext;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.ExpenseCategoryRepository;
import com.th4.edge.decoboon.finance.repository.ExpenseRepository;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** EXPENSE-D3：费用付款凭证来源快照、模板路由、重打幂等和费用详情回读。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:expense-voucher-d3;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/expense-voucher-d3-upload",
        "th4.edge.framework.file-signature-secret=expense-voucher-d3-secret",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class ExpenseVoucherLifecycleTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private ExpenseCategoryRepository categoryRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private PayableRepository payableRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private VoucherContext voucherContext;
    @Autowired private VoucherRecordRepository voucherRecordRepository;
    @Autowired private ExpenseVoucherProjectionContext voucherProjectionContext;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void expensePaymentVoucherKeepsSourceSnapshotAndReprintIdentity() throws Exception {
        Fixture fixture = fixture();

        VoucherRecord first = voucherContext.buildForPayment(
                fixture.boss(), fixture.payment().getPaymentId(), null);
        VoucherRecord reprint = voucherContext.buildForPayment(
                fixture.boss(), fixture.payment().getPaymentId(), null);

        assertThat(first.getSourceType()).isEqualTo(VoucherRecord.SOURCE_EXPENSE);
        assertThat(first.getSourceId()).isEqualTo(fixture.expense().getExpenseId());
        assertThat(first.getVoucherNo()).startsWith("PZ");
        assertThat(first.getTemplateVersion()).isEqualTo(2);
        assertThat(first.getSha256()).hasSize(64);
        assertThat(first.getPath()).isNotBlank();
        assertThat(reprint.getVoucherId()).isEqualTo(first.getVoucherId());
        assertThat(reprint.getVoucherNo()).isEqualTo(first.getVoucherNo());
        assertThat(reprint.getSha256()).isEqualTo(first.getSha256());
        assertThat(reprint.getPath()).isEqualTo(first.getPath());

        JsonNode snapshot = objectMapper.readTree(first.getSourceSnapshot());
        assertThat(snapshot.path("expenseId").asLong()).isEqualTo(fixture.expense().getExpenseId());
        assertThat(snapshot.path("expenseNo").asText()).isEqualTo(fixture.expense().getExpenseNo());
        assertThat(snapshot.path("expenseCategoryCode").asText()).isEqualTo("TRAVEL");
        assertThat(snapshot.path("approvalVersion").asInt()).isEqualTo(2);
        assertThat(snapshot.path("paymentId").asLong()).isEqualTo(fixture.payment().getPaymentId());
        assertThat(snapshot.path("paymentAmount").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(snapshot.path("templatePresetCode").asText()).isEqualTo("E-TRAVEL");
        assertThat(snapshot.path("templateVersion").asInt()).isEqualTo(2);
        assertThat(snapshot.path("fileSha256").asText()).isEqualTo(first.getSha256());

        Expense projection = voucherProjectionContext.apply(fixture.expense(), fixture.boss());
        assertThat(projection.getPayments()).hasSize(1);
        assertThat(projection.getVouchers()).hasSize(1);
        assertThat(projection.getVouchers().get(0).getPaymentId())
                .isEqualTo(fixture.payment().getPaymentId());
        assertThat(projection.getVouchers().get(0).getUrl()).isNotBlank();
    }

    @Test
    void expenseSourceMustResolveToSameCompanyAndSamePayableBeforeVoucherSideEffects() {
        Fixture fixture = fixture();
        Payable payable = fixture.expense().getPayable();
        payable.setSourceId(999999999999L);
        payableRepository.save(payable);

        assertThatThrownBy(() -> voucherContext.buildForPayment(
                fixture.boss(), fixture.payment().getPaymentId(), null))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("费用来源不存在或无权访问");
        assertThat(voucherRecordRepository.count()).isZero();
    }

    private Fixture fixture() {
        long seq = System.nanoTime();
        Company company = new Company();
        company.setTitle("费用凭证测试企业" + seq);
        company.setName("费用凭证测试企业");
        company.setSize(Size.A);
        company = companyRepository.save(company);

        Store store = new Store();
        store.setCompany(company);
        store.setName("费用凭证测试门店");
        store.setDefaultStore(true);
        store = storeRepository.save(store);

        Employee boss = new Employee();
        boss.setCompany(company);
        boss.setStore(store);
        boss.setRole(EmployeeRole.Boss);
        boss.setStatus(EmployeeStatus.Active);
        boss.setName("费用凭证测试负责人");
        boss.setTelNo("18" + String.format("%09d", Math.abs(seq % 1_000_000_000L)));
        boss.setJoinedAt(LocalDateTime.now());
        boss = employeeRepository.save(boss);

        ExpenseCategory category = new ExpenseCategory();
        category.setCompany(company);
        category.setCode("TRAVEL");
        category.setName("差旅费");
        category.setBuiltIn(true);
        category.setActive(true);
        category.setSort(10);
        category = categoryRepository.save(category);

        Expense expense = new Expense();
        expense.setCompany(company);
        expense.setStore(store);
        expense.setRequester(boss);
        expense.setOwner(boss);
        expense.setPayeeEmployee(boss);
        expense.setCategory(category);
        expense.setExpenseNo("BX-D3-" + seq);
        expense.setPaymentMode(ExpensePaymentMode.Reimbursement);
        expense.setAmount(new BigDecimal("300.00"));
        expense.setExpenseDate(LocalDate.now());
        expense.setDescription("EXPENSE-D3 凭证来源快照测试");
        expense.setStatus(ExpenseStatus.Approved);
        expense.setVersion(2);
        expense.setSubmittedAt(LocalDateTime.now().minusHours(2));
        expense.setApprovedBy(boss);
        expense.setApprovedAt(LocalDateTime.now().minusHours(1));
        expense.setSelfApproved(false);
        expense.setIdempotencyKey("d3-expense-" + seq);
        expense = expenseRepository.save(expense);

        Payable payable = new Payable();
        payable.setCompany(company);
        payable.setStore(store);
        payable.setOwner(boss);
        payable.setCreatedBy(boss);
        payable.setName("费用报销 · " + expense.getExpenseNo());
        payable.setType(PayableType.Expense);
        payable.setPayeeEmployee(boss);
        payable.setPayeeName(boss.getName());
        payable.setAmount(new BigDecimal("300.00"));
        payable.setPaidAmount(new BigDecimal("300.00"));
        payable.setEstimated(Boolean.FALSE);
        payable.setDueDate(LocalDate.now());
        payable.setStatus(PayableStatus.Paid);
        payable.setUrgent(Boolean.FALSE);
        payable.setSourceType("Expense");
        payable.setSourceId(expense.getExpenseId());
        payable.setSourceNo(expense.getExpenseNo());
        payable = payableRepository.save(payable);
        expense.setPayable(payable);
        expense = expenseRepository.save(expense);

        Payment payment = new Payment();
        payment.setCompany(company);
        payment.setStore(store);
        payment.setOwner(boss);
        payment.setCreatedBy(boss);
        payment.setPayable(payable);
        payment.setPayeeEmployee(boss);
        payment.setPayeeName(boss.getName());
        payment.setAmount(new BigDecimal("300.00"));
        payment.setAccount(FundAccount.CompanyBank);
        payment.setPaidAt(LocalDate.now());
        payment.setIdempotencyKey("d3-payment-" + seq);
        payment = paymentRepository.save(payment);

        Ledger ledger = new Ledger();
        ledger.setCompany(company);
        ledger.setStore(store);
        ledger.setOwner(boss);
        ledger.setCreatedBy(boss);
        ledger.setDirection(LedgerDirection.Expense);
        ledger.setSource(LedgerSource.Payment);
        ledger.setSourceId(payment.getPaymentId());
        ledger.setAccount(FundAccount.CompanyBank);
        ledger.setAmount(new BigDecimal("300.00"));
        ledger.setCategory("差旅费");
        ledger.setOccurredAt(LocalDate.now());
        ledger.setCounterparty(boss.getName());
        ledger.setSummary("费用付款");
        ledgerRepository.save(ledger);

        return new Fixture(boss, expense, payment);
    }

    private record Fixture(Employee boss, Expense expense, Payment payment) {
    }
}
