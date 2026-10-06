package com.th4.edge.decoboon.document;

import com.th4.edge.decoboon.document.context.VoucherContext;
import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.document.repository.VoucherRecordRepository;
import com.th4.edge.decoboon.document.repository.VoucherRevisionRepository;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.labor.repository.WorkerRepository;
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
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 凭证生成验收：落库、金额大写、合并判定与重打幂等。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:voucher-build;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "spring.main.banner-mode=off",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/voucher-acceptance-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class VoucherBuildAcceptanceTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private PayableRepository payableRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private VoucherRecordRepository voucherRecordRepository;
    @Autowired private VoucherRevisionRepository voucherRevisionRepository;
    @Autowired private org.springframework.beans.factory.ObjectProvider<com.th4.edge.decoboon.finance.service.payments.update.support.Update> paymentWorkerUpdateProvider;
    @Autowired private VoucherContext voucherContext;
    @Autowired private com.th4.edge.decoboon.document.service.voucher.page.Page voucherPage;

    @Test
    void shouldBuildVoucherAndReturnSameRecordOnReprint() {
        Employee boss = boss();
        Ledger first = ledger(boss, "梦圆小区木工班组", "工资", "40.00");
        Ledger second = ledger(boss, "梦圆小区木工班组", "工资", "60.00");

        VoucherRecord record = voucherContext.build(boss, List.of(first.getLedgerId(), second.getLedgerId()));

        assertThat(record.getVoucherNo()).startsWith("PZ");
        assertThat(record.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(record.getAmountWords()).isEqualTo("壹佰元整");
        assertThat(record.getPartyName()).isEqualTo("梦圆小区木工班组");
        assertThat(record.getItems()).hasSize(2);
        assertThat(record.getFileName()).endsWith(".pdf");
        assertThat(record.getUrl()).isNotBlank();
        assertThat(record.getSize()).isGreaterThan(0L);

        VoucherRecord reprinted = voucherContext.build(boss, List.of(first.getLedgerId(), second.getLedgerId()));

        assertThat(reprinted.getVoucherId()).isEqualTo(record.getVoucherId());
        assertThat(reprinted.getVoucherNo()).isEqualTo(record.getVoucherNo());
        assertThat(voucherRecordRepository.findByCompany_ComIdOrderByVoucherIdDesc(boss.getCompany().getComId()))
                .hasSize(1);
    }

    @Test
    void shouldFilterVoucherPageByKeyword() {
        Employee boss = boss();
        Ledger mine = ledger(boss, "梦圆小区木工班组", "工资", "40.00");
        Ledger other = ledger(boss, "另一家劳务班组", "工资", "50.00");
        voucherContext.build(boss, List.of(mine.getLedgerId()));
        voucherContext.build(boss, List.of(other.getLedgerId()));

        com.th4.edge.decoboon.document.service.voucher.page.support.Page target =
                AopTestUtils.getTargetObject(voucherPage);
        ReflectionTestUtils.setField(target, "employee", boss);

        ReflectionTestUtils.setField(target, "keyword", "梦圆");
        var matched = target.call(new VoucherRecord());
        assertThat(matched.getContent()).hasSize(1);
        assertThat(matched.getContent().get(0).getPartyName()).isEqualTo("梦圆小区木工班组");

        ReflectionTestUtils.setField(target, "keyword", "不存在的人");
        assertThat(target.call(new VoucherRecord()).getContent()).isEmpty();
    }

    @Test
    void shouldFilterVoucherPageByType() {
        Employee boss = boss();
        Ledger expense = ledger(boss, "梦圆小区木工班组", "工资", "40.00");
        voucherContext.build(boss, List.of(expense.getLedgerId()));

        com.th4.edge.decoboon.document.service.voucher.page.support.Page target =
                AopTestUtils.getTargetObject(voucherPage);
        ReflectionTestUtils.setField(target, "employee", boss);
        ReflectionTestUtils.setField(target, "keyword", null);

        ReflectionTestUtils.setField(target, "type", "Payment");
        assertThat(target.call(new VoucherRecord()).getContent()).hasSize(1);

        ReflectionTestUtils.setField(target, "type", "Receipt");
        assertThat(target.call(new VoucherRecord()).getContent()).isEmpty();
    }

    @Test
    void shouldRejectMergingRecordsWithDifferentCounterparty() {
        Employee boss = boss();
        Ledger first = ledger(boss, "梦圆小区木工班组", "工资", "40.00");
        Ledger other = ledger(boss, "另一家劳务班组", "工资", "50.00");

        assertThatThrownBy(() -> voucherContext.build(boss, List.of(first.getLedgerId(), other.getLedgerId())))
                .hasMessageContaining("仅可合并");
    }

    @Test
    void shouldRejectZeroAmountVoucher() {
        Employee boss = boss();
        Ledger zero = ledger(boss, "零元班组", "工资", "0.00");

        assertThatThrownBy(() -> voucherContext.build(boss, List.of(zero.getLedgerId())))
                .hasMessageContaining("凭证金额不能为 0");
    }

    @Test
    void shouldRebuildPaymentVoucherAfterWorkerBackfillWithoutChangingFinancialFacts() {
        Employee boss = boss();
        Payable payable = salaryPayable(boss);
        Payment payment = payment(boss, payable, null);
        Ledger ledger = paymentLedger(boss, payment, payable.getAmount());

        VoucherRecord before = voucherContext.build(boss, List.of(ledger.getLedgerId()));
        assertThat(before.getWorker()).isNull();
        String voucherNo = before.getVoucherNo();
        String path = before.getPath();
        String sha = before.getSha256();
        BigDecimal paymentAmount = payment.getAmount();
        LocalDate paidAt = payment.getPaidAt();
        FundAccount account = payment.getAccount();
        BigDecimal payablePaid = payable.getPaidAmount();

        Worker worker = worker(boss);
        com.th4.edge.decoboon.finance.service.payments.update.support.Update provider =
                AopTestUtils.getTargetObject(paymentWorkerUpdateProvider.getObject());
        ReflectionTestUtils.setField(provider, "employee", boss);
        ReflectionTestUtils.setField(provider, "paymentId", payment.getPaymentId());
        ReflectionTestUtils.setField(provider, "workerId", worker.getWorkerId());
        Payment updatedPayment = provider.call(new Payment());
        VoucherRecord after = voucherRecordRepository.findById(before.getVoucherId()).orElseThrow();

        assertThat(updatedPayment.getWorker().getWorkerId()).isEqualTo(worker.getWorkerId());
        assertThat(after).isNotNull();
        assertThat(after.getVoucherId()).isEqualTo(before.getVoucherId());
        assertThat(after.getVoucherNo()).isEqualTo(voucherNo);
        assertThat(after.getPath()).isEqualTo(path);
        assertThat(after.getWorker().getWorkerId()).isEqualTo(worker.getWorkerId());
        assertThat(after.getPartyName()).isEqualTo(worker.getName());
        assertThat(after.getSha256()).isNotEqualTo(sha);
        assertThat(after.getTotalAmount()).isEqualByComparingTo(paymentAmount);
        assertThat(voucherRevisionRepository.findByCompany_ComIdAndVoucher_VoucherIdOrderByRevisionIdDesc(
                boss.getCompany().getComId(), before.getVoucherId())).singleElement().satisfies(revision -> {
            assertThat(revision.getBeforeWorker()).isNull();
            assertThat(revision.getAfterWorker().getWorkerId()).isEqualTo(worker.getWorkerId());
            assertThat(revision.getOperator().getEmpId()).isEqualTo(boss.getEmpId());
            assertThat(revision.getChangeType()).isEqualTo("WorkerBackfill");
            assertThat(revision.getChangedAt()).isNotNull();
        });
        assertThat(payment.getAmount()).isEqualByComparingTo(paymentAmount);
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
        assertThat(payment.getAccount()).isEqualTo(account);
        assertThat(payable.getPaidAmount()).isEqualByComparingTo(payablePaid);
        assertThat(ledgerRepository.findById(ledger.getLedgerId()).orElseThrow().getAmount())
                .isEqualByComparingTo(paymentAmount);
    }

    private Payable salaryPayable(Employee owner) {
        long seq = System.nanoTime();
        Customer customer = new Customer();
        customer.setCompany(owner.getCompany());
        customer.setStore(owner.getStore());
        customer.setOwner(owner);
        customer.setName("凭证测试客户");
        customer.setTelNo("18" + String.format("%09d", Math.abs(seq % 1_000_000_000L)));
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(owner.getCompany());
        property.setStore(owner.getStore());
        property.setOwner(owner);
        property.setCustomer(customer);
        property.setPropertyNo("PROP-" + seq);
        property.setAddress("凭证测试小区 1 栋 101");
        property.setArea(new BigDecimal("100"));
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Payable payable = new Payable();
        payable.setCompany(owner.getCompany());
        payable.setStore(owner.getStore());
        payable.setOwner(owner);
        payable.setProject(null);
        payable.setProperty(property);
        payable.setCreatedBy(owner);
        payable.setName("工资补录凭证测试");
        payable.setType(PayableType.Salary);
        payable.setPayeeName("张师傅");
        payable.setAmount(new BigDecimal("600.00"));
        payable.setPaidAmount(new BigDecimal("600.00"));
        payable.setDueDate(LocalDate.now());
        return payableRepository.save(payable);
    }

    private Payment payment(Employee owner, Payable payable, Worker worker) {
        Payment payment = new Payment();
        payment.setCompany(owner.getCompany());
        payment.setStore(owner.getStore());
        payment.setOwner(owner);
        payment.setCreatedBy(owner);
        payment.setPayable(payable);
        payment.setWorker(worker);
        payment.setPayeeName("张师傅");
        payment.setAmount(new BigDecimal("600.00"));
        payment.setAccount(FundAccount.CompanyBank);
        payment.setPaidAt(LocalDate.now());
        return paymentRepository.save(payment);
    }

    private Ledger paymentLedger(Employee owner, Payment payment, BigDecimal amount) {
        Ledger ledger = new Ledger();
        ledger.setCompany(owner.getCompany());
        ledger.setStore(owner.getStore());
        ledger.setOwner(owner);
        ledger.setCreatedBy(owner);
        ledger.setDirection(LedgerDirection.Expense);
        ledger.setSource(LedgerSource.Payment);
        ledger.setSourceId(payment.getPaymentId());
        ledger.setAccount(FundAccount.CompanyBank);
        ledger.setAmount(amount);
        ledger.setCategory("工资");
        ledger.setOccurredAt(LocalDate.now());
        ledger.setCounterparty("张师傅");
        ledger.setSummary("工资发放");
        return ledgerRepository.save(ledger);
    }

    private Worker worker(Employee owner) {
        Worker worker = new Worker();
        worker.setCompany(owner.getCompany());
        worker.setName("张师傅");
        worker.setIdCardNo("340101199001011234");
        worker.setTrade("水电工");
        worker.setPhone("13900000000");
        worker.setBankName("测试银行");
        worker.setBankAccount("6222000000000000");
        return workerRepository.save(worker);
    }

    private Employee boss() {
        long seq = System.nanoTime();
        Company company = new Company();
        company.setTitle("凭证测试企业" + seq);
        company.setName("凭证测试企业");
        company.setSize(Size.A);
        company = companyRepository.save(company);

        Store store = new Store();
        store.setCompany(company);
        store.setName("凭证测试门店");
        store.setDefaultStore(true);
        store = storeRepository.save(store);

        Employee boss = new Employee();
        boss.setCompany(company);
        boss.setStore(store);
        boss.setRole(EmployeeRole.Boss);
        boss.setStatus(EmployeeStatus.Active);
        boss.setName("凭证测试负责人");
        boss.setTelNo("18" + String.format("%09d", Math.abs(seq % 1_000_000_000L)));
        boss.setJoinedAt(LocalDateTime.now());
        return employeeRepository.save(boss);
    }

    private Ledger ledger(Employee owner, String counterparty, String category, String amount) {
        Ledger ledger = new Ledger();
        ledger.setCompany(owner.getCompany());
        ledger.setStore(owner.getStore());
        ledger.setOwner(owner);
        ledger.setCreatedBy(owner);
        ledger.setDirection(LedgerDirection.Expense);
        ledger.setSource(LedgerSource.Manual);
        ledger.setAccount(FundAccount.CompanyBank);
        ledger.setAmount(new BigDecimal(amount));
        ledger.setCategory(category);
        ledger.setOccurredAt(LocalDate.now());
        ledger.setCounterparty(counterparty);
        ledger.setSummary(counterparty + " 工资发放");
        return ledgerRepository.save(ledger);
    }
}
