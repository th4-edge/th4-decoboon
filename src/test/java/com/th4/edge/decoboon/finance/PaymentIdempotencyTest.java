package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.PayableSettlementRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 付款幂等、并发唯一约束与多应付核销回归测试。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment-idempotency;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
class PaymentIdempotencyTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private PayableRepository payableRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PayableSettlementRepository payableSettlementRepository;
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private ObjectProvider<com.th4.edge.decoboon.finance.service.payments.save.support.Save> paymentSaveProvider;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void shouldReturnOriginalPaymentWhenSameClientKeyIsRepeated() {
        Fixture fixture = fixture("payment-repeat", new BigDecimal("1000.00"));

        Payment first = collect(fixture, new BigDecimal("300.00"), "payment-key-1", null);
        Payment repeat = collect(fixture, new BigDecimal("300.00"), "payment-key-1", null);

        assertThat(repeat.getPaymentId()).isEqualTo(first.getPaymentId());
        assertThat(repeat.getIdempotencyKey()).isEqualTo("payment-key-1");
        assertThat(repeat.getIdempotencyFingerprint()).isNotBlank();
        assertThat(paymentRepository.findByCompany_ComIdOrderByPaidAtDesc(fixture.company().getComId())).hasSize(1);
        assertThat(ledgerRepository.findByCompany_ComIdOrderByOccurredAtDescLedgerIdDesc(fixture.company().getComId())).hasSize(1);
        assertThat(payableRepository.findById(fixture.payable().getPayableId()).orElseThrow().getPaidAmount())
                .isEqualByComparingTo("300.00");
    }

    @Test
    void shouldAllowTwoRealPaymentsWhenSameBusinessUsesDifferentClientKeys() {
        Fixture fixture = fixture("payment-real", new BigDecimal("1000.00"));

        Payment first = collect(fixture, new BigDecimal("300.00"), "payment-key-real-1", null);
        Payment second = collect(fixture, new BigDecimal("300.00"), "payment-key-real-2", null);

        assertThat(second.getPaymentId()).isNotEqualTo(first.getPaymentId());
        assertThat(paymentRepository.findByCompany_ComIdOrderByPaidAtDesc(fixture.company().getComId())).hasSize(2);
        assertThat(ledgerRepository.findByCompany_ComIdOrderByOccurredAtDescLedgerIdDesc(fixture.company().getComId())).hasSize(2);
        assertThat(payableRepository.findById(fixture.payable().getPayableId()).orElseThrow().getPaidAmount())
                .isEqualByComparingTo("600.00");
    }

    @Test
    void shouldAllowSameClientKeyAcrossDifferentCompanies() {
        Fixture firstCompany = fixture("payment-company-a", new BigDecimal("1000.00"));
        Fixture secondCompany = fixture("payment-company-b", new BigDecimal("1000.00"));

        Payment first = collect(firstCompany, new BigDecimal("300.00"), "cross-company-key", null);
        Payment second = collect(secondCompany, new BigDecimal("300.00"), "cross-company-key", null);

        assertThat(second.getPaymentId()).isNotEqualTo(first.getPaymentId());
        assertThat(paymentRepository.findByCompany_ComIdOrderByPaidAtDesc(firstCompany.company().getComId())).hasSize(1);
        assertThat(paymentRepository.findByCompany_ComIdOrderByPaidAtDesc(secondCompany.company().getComId())).hasSize(1);
    }

    @Test
    void shouldRejectReusedClientKeyWhenBusinessFingerprintDiffers() {
        Fixture fixture = fixture("payment-conflict", new BigDecimal("1000.00"));
        collect(fixture, new BigDecimal("300.00"), "payment-key-conflict", null);

        assertDefined(409, "该幂等键已被其他收付款占用",
                () -> collect(fixture, new BigDecimal("400.00"), "payment-key-conflict", null));
        assertThat(paymentRepository.findByCompany_ComIdOrderByPaidAtDesc(fixture.company().getComId())).hasSize(1);
        assertThat(ledgerRepository.findByCompany_ComIdOrderByOccurredAtDescLedgerIdDesc(fixture.company().getComId())).hasSize(1);
        assertThat(payableRepository.findById(fixture.payable().getPayableId()).orElseThrow().getPaidAmount())
                .isEqualByComparingTo("300.00");
    }

    @Test
    void shouldReturnOriginalPaymentAfterPayableBalanceChanged() {
        Fixture fixture = fixture("payment-balance", new BigDecimal("1000.00"));

        Payment first = collect(fixture, new BigDecimal("1000.00"), "payment-key-full", null);
        Payment repeat = collect(fixture, new BigDecimal("1000.00"), "payment-key-full", null);

        assertThat(repeat.getPaymentId()).isEqualTo(first.getPaymentId());
        assertThat(paymentRepository.findByCompany_ComIdOrderByPaidAtDesc(fixture.company().getComId())).hasSize(1);
        assertThat(ledgerRepository.findByCompany_ComIdOrderByOccurredAtDescLedgerIdDesc(fixture.company().getComId())).hasSize(1);
        assertThat(payableRepository.findById(fixture.payable().getPayableId()).orElseThrow().getPaidAmount())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldNotDuplicateMultiPayableSettlementsOnRetry() {
        Fixture fixture = fixture("payment-multi", new BigDecimal("300.00"));
        Payable second = addPayable(fixture, new BigDecimal("300.00"), LocalDate.now().plusDays(10));

        Payment first = collect(fixture, new BigDecimal("500.00"), "payment-key-multi",
                List.of(fixture.payable().getPayableId(), second.getPayableId()));
        Payment repeat = collect(fixture, new BigDecimal("500.00"), "payment-key-multi",
                List.of(fixture.payable().getPayableId(), second.getPayableId()));

        assertThat(repeat.getPaymentId()).isEqualTo(first.getPaymentId());
        assertThat(paymentRepository.findByCompany_ComIdOrderByPaidAtDesc(fixture.company().getComId())).hasSize(1);
        assertThat(payableSettlementRepository.findByPayment_PaymentId(first.getPaymentId())).hasSize(2);
        assertThat(ledgerRepository.findByCompany_ComIdOrderByOccurredAtDescLedgerIdDesc(fixture.company().getComId())).hasSize(1);
        assertThat(payableRepository.findById(fixture.payable().getPayableId()).orElseThrow().getPaidAmount())
                .isEqualByComparingTo("300.00");
        assertThat(payableRepository.findById(second.getPayableId()).orElseThrow().getPaidAmount())
                .isEqualByComparingTo("200.00");
    }

    @Test
    void shouldHaveDatabaseUniqueConstraintOnCompanyAndIdempotencyKey() {
        List<String> columns = jdbcTemplate.queryForList("""
                        select column_name
                          from information_schema.index_columns
                         where upper(index_name) like ?
                         order by ordinal_position
                        """, String.class, "UK_FIN_PAYMENT_COMPANY_IDEMPOTENCY%")
                .stream().map(value -> value.toLowerCase()).toList();

        assertThat(columns).containsExactly("com_id", "idempotency_key");
    }

    @Test
    void shouldSerializeConcurrentPaymentsWithTheSameClientKey() throws Exception {
        Fixture fixture = fixture("payment-concurrent", new BigDecimal("1000.00"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> first = executor.submit(() -> attempt(fixture, ready, start));
            Future<Attempt> second = executor.submit(() -> attempt(fixture, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Attempt> attempts = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            List<Attempt> succeeded = attempts.stream().filter(item -> item.payment() != null).toList();
            // 分布式锁可能让后到请求等待并返回同一笔付款，也可能由唯一索引把输家转换为 409；
            // 两种调度都必须收敛到同一业务事实，测试不得依赖线程先后顺序。
            assertThat(succeeded).isNotEmpty();
            assertThat(succeeded).extracting(item -> item.payment().getPaymentId())
                    .containsOnly(succeeded.get(0).payment().getPaymentId());
            List<Attempt> failed = attempts.stream().filter(item -> item.error() != null).toList();
            if (!failed.isEmpty()) {
                assertThat(failed).hasSize(1);
                DefinedException conflict = (DefinedException) rootCause(failed.get(0).error());
                assertThat(conflict.getErrorCode().getCode()).isEqualTo("409");
                assertThat(conflict.getMessage()).contains("该幂等键已被其他收付款占用");
            }
            assertThat(paymentRepository.findByCompany_ComIdOrderByPaidAtDesc(fixture.company().getComId())).hasSize(1);
            assertThat(ledgerRepository.findByCompany_ComIdOrderByOccurredAtDescLedgerIdDesc(fixture.company().getComId())).hasSize(1);
            assertThat(payableRepository.findById(fixture.payable().getPayableId()).orElseThrow().getPaidAmount())
                    .isEqualByComparingTo("300.00");
        } finally {
            executor.shutdownNow();
        }
    }

    private Attempt attempt(Fixture fixture, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            return new Attempt(collect(fixture, new BigDecimal("300.00"), "payment-key-concurrent", null), null);
        } catch (Throwable error) {
            return new Attempt(null, error);
        }
    }

    private Payment collect(Fixture fixture, BigDecimal amount, String idempotencyKey, List<Long> payableIds) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            com.th4.edge.decoboon.finance.service.payments.save.support.Save provider =
                    paymentSaveProvider.getObject();
            com.th4.edge.decoboon.finance.service.payments.save.support.Save save =
                    AopTestUtils.getTargetObject(provider);
            ReflectionTestUtils.setField(save, "employee", fixture.finance());
            ReflectionTestUtils.setField(save, "payableId",
                    payableIds == null ? fixture.payable().getPayableId() : null);
            ReflectionTestUtils.setField(save, "payableIds", payableIds);
            ReflectionTestUtils.setField(save, "amount", amount);
            ReflectionTestUtils.setField(save, "account", FundAccount.CompanyBank);
            ReflectionTestUtils.setField(save, "paidAt", LocalDate.now());
            ReflectionTestUtils.setField(save, "payeeName", fixture.payable().getPayeeName());
            ReflectionTestUtils.setField(save, "workerId", null);
            ReflectionTestUtils.setField(save, "transactionNo", null);
            ReflectionTestUtils.setField(save, "voucherUrl", null);
            ReflectionTestUtils.setField(save, "remark", "付款幂等回归测试");
            ReflectionTestUtils.setField(save, "idempotencyKey", idempotencyKey);
            return save.call(new Payment());
        }));
    }

    private void assertDefined(int code, String messagePart, org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOfSatisfying(DefinedException.class, error -> {
            assertThat(error.getErrorCode().getCode()).isEqualTo(String.valueOf(code));
            assertThat(error.getMessage()).contains(messagePart);
        });
    }

    private Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private record Attempt(Payment payment, Throwable error) {
    }

    private record Fixture(Company company, Store store, Employee finance, Customer customer,
                           Property property, Project project, Payable payable) {
    }

    private Fixture fixture(String prefix, BigDecimal payableAmount) {
        String key = prefix + "-" + Long.toHexString(System.nanoTime());
        return transactionTemplate.execute(status -> {
            Company company = company("企" + key);
            Store store = store(company);
            Employee finance = employee(company, store, EmployeeRole.Finance);
            Customer customer = customer(company, store, finance, key);
            Property property = property(company, store, customer, finance, key);
            Quote quote = quote(company, store, customer, property, finance, key);
            Project project = project(company, store, customer, property, quote, finance, key);
            Payable payable = payable(company, store, property, project, finance, key, payableAmount, LocalDate.now());
            return new Fixture(company, store, finance, customer, property, project, payable);
        });
    }

    private Payable addPayable(Fixture fixture, BigDecimal amount, LocalDate dueDate) {
        return transactionTemplate.execute(status -> payable(fixture.company(), fixture.store(), fixture.property(),
                fixture.project(), fixture.finance(), "second-" + System.nanoTime(), amount, dueDate));
    }

    private Payable payable(Company company, Store store, Property property, Project project,
                            Employee owner, String key, BigDecimal amount, LocalDate dueDate) {
        Payable payable = new Payable();
        payable.setCompany(company);
        payable.setStore(store);
        payable.setOwner(owner);
        payable.setProject(project);
        payable.setProperty(property);
        payable.setName("应付-" + key);
        payable.setType(PayableType.Material);
        payable.setPayeeName("供应商-" + key);
        payable.setAmount(amount);
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setDueDate(dueDate);
        payable.setStatus(PayableStatus.Pending);
        payable.setUrgent(false);
        payable.setCreatedBy(owner);
        return payableRepository.save(payable);
    }

    private Company company(String name) {
        Company company = new Company();
        company.setTitle(name);
        company.setName("C" + Long.toHexString(System.nanoTime()));
        company.setSize(Size.A);
        return companyRepository.save(company);
    }

    private Store store(Company company) {
        Store store = new Store();
        store.setCompany(company);
        store.setName("S" + Long.toHexString(System.nanoTime()));
        store.setDefaultStore(true);
        return storeRepository.save(store);
    }

    private Employee employee(Company company, Store store, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setName("E" + Long.toHexString(System.nanoTime()));
        employee.setTelNo("1" + Math.abs(System.nanoTime() % 1_000_000_000L));
        return employeeRepository.save(employee);
    }

    private Customer customer(Company company, Store store, Employee owner, String key) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(owner);
        customer.setName("客" + key);
        customer.setTelNo("2" + Math.abs(System.nanoTime() % 1_000_000_000L));
        customer.setCommunity("小" + key);
        customer.setAddr("地" + key);
        customer.setArea(new BigDecimal("100"));
        return customerRepository.save(customer);
    }

    private Property property(Company company, Store store, Customer customer, Employee owner, String key) {
        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(owner);
        property.setCustomer(customer);
        property.setPropertyNo("P-" + key);
        property.setCommunity(customer.getCommunity());
        property.setBuilding("1");
        property.setRoomNo("101");
        property.setAddress(customer.getAddr());
        property.setArea(new BigDecimal("100"));
        property.setLayout("3室2厅");
        property.setStatus(PropertyStatus.Serving);
        return propertyRepository.save(property);
    }

    private Quote quote(Company company, Store store, Customer customer, Property property,
                        Employee owner, String key) {
        Quote quote = new Quote();
        quote.setNo("Q-" + key);
        quote.setTitle("报" + key);
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(owner);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(property.getArea());
        quote.setSubtotal(new BigDecimal("1000.00"));
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setAmount(new BigDecimal("1000.00"));
        quote.setCost(new BigDecimal("800.00"));
        quote.setDesignFee(BigDecimal.ZERO);
        quote.setManagementFee(BigDecimal.ZERO);
        quote.setStatus(QuoteStatus.Accepted);
        return quoteRepository.save(quote);
    }

    private Project project(Company company, Store store, Customer customer, Property property,
                            Quote quote, Employee finance, String key) {
        Project project = new Project();
        project.setNo("PJ-" + key);
        project.setName("项" + key);
        project.setCompany(company);
        project.setStore(store);
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setOwner(customer.getOwner());
        project.setSupport(finance);
        project.setSiteAddr(property.getAddress());
        project.setArea(property.getArea());
        project.setContractAmount(new BigDecimal("1000.00"));
        project.setCost(new BigDecimal("800.00"));
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(30);
        project.setStartDate(LocalDate.now());
        project.setPlanEndDate(LocalDate.now().plusDays(60));
        return projectRepository.save(project);
    }
}
