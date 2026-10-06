package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
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

/** 收款并发同 key 只能成功一笔，且数据库企业内唯一约束真实存在。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:receipt-idempotency-concurrency;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class ReceiptIdempotencyConcurrencyTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ReceivableRepository receivableRepository;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private ObjectProvider<com.th4.edge.decoboon.finance.service.receipts.save.support.Save> receiptSaveProvider;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void shouldHaveDatabaseUniqueConstraintOnCompanyAndIdempotencyKey() {
        List<String> columns = jdbcTemplate.queryForList("""
                        select column_name
                          from information_schema.index_columns
                         where upper(index_name) like ?
                         order by ordinal_position
                        """, String.class, "UK_FIN_RECEIPT_COMPANY_IDEMPOTENCY%")
                .stream().map(value -> value.toLowerCase()).toList();

        assertThat(columns).containsExactly("com_id", "idempotency_key");
    }

    @Test
    void shouldSerializeConcurrentReceiptsWithTheSameClientKey() throws Exception {
        Fixture fixture = fixture("receipt-concurrent", new BigDecimal("1000.00"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> first = executor.submit(() -> attempt(fixture, ready, start));
            Future<Attempt> second = executor.submit(() -> attempt(fixture, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Attempt> attempts = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(attempts.stream().filter(item -> item.receipt() != null)).hasSize(1);
            Attempt failed = attempts.stream().filter(item -> item.error() != null).findFirst().orElseThrow();
            DefinedException conflict = (DefinedException) rootCause(failed.error());
            assertThat(conflict.getErrorCode().getCode()).isEqualTo("409");
            assertThat(conflict.getMessage()).contains("该幂等键已被其他收付款占用");

            assertThat(receiptRepository.findByCompany_ComIdOrderByReceivedAtDesc(fixture.company().getComId())).hasSize(1);
            assertThat(ledgerRepository.findByCompany_ComIdOrderByOccurredAtDescLedgerIdDesc(fixture.company().getComId())).hasSize(1);
            assertThat(receivableRepository.findById(fixture.receivable().getReceivableId()).orElseThrow().getReceivedAmount())
                    .isEqualByComparingTo("300.00");
        } finally {
            executor.shutdownNow();
        }
    }

    private Attempt attempt(Fixture fixture, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            return new Attempt(collect(fixture), null);
        } catch (Throwable error) {
            return new Attempt(null, error);
        }
    }

    private Receipt collect(Fixture fixture) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            com.th4.edge.decoboon.finance.service.receipts.save.support.Save provider = receiptSaveProvider.getObject();
            com.th4.edge.decoboon.finance.service.receipts.save.support.Save save = AopTestUtils.getTargetObject(provider);
            ReflectionTestUtils.setField(save, "employee", fixture.finance());
            ReflectionTestUtils.setField(save, "projectId", fixture.project().getProjectId());
            ReflectionTestUtils.setField(save, "receivableId", fixture.receivable().getReceivableId());
            ReflectionTestUtils.setField(save, "amount", new BigDecimal("300.00"));
            ReflectionTestUtils.setField(save, "account", FundAccount.CompanyBank);
            ReflectionTestUtils.setField(save, "receivedAt", LocalDate.now());
            ReflectionTestUtils.setField(save, "payerName", fixture.customer().getName());
            ReflectionTestUtils.setField(save, "voucherUrl", null);
            ReflectionTestUtils.setField(save, "remark", "收款并发幂等测试");
            ReflectionTestUtils.setField(save, "idempotencyKey", "receipt-key-concurrent");
            return save.call(new Receipt());
        }));
    }

    private Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private record Attempt(Receipt receipt, Throwable error) {
    }

    private record Fixture(Company company, Store store, Employee finance, Customer customer,
                           Property property, Project project, Receivable receivable) {
    }

    private Fixture fixture(String prefix, BigDecimal receivableAmount) {
        String key = prefix + "-" + Long.toHexString(System.nanoTime());
        return transactionTemplate.execute(status -> {
            Company company = company("企" + key);
            Store store = store(company);
            Employee finance = employee(company, store, EmployeeRole.Finance);
            Customer customer = customer(company, store, finance, key);
            Property property = property(company, store, customer, finance, key);
            Quote quote = quote(company, store, customer, property, finance, key);
            Project project = project(company, store, customer, property, quote, finance, key);
            Receivable receivable = receivable(company, project, property, customer, finance, receivableAmount);
            return new Fixture(company, store, finance, customer, property, project, receivable);
        });
    }

    private Receivable receivable(Company company, Project project, Property property,
                                  Customer customer, Employee owner, BigDecimal amount) {
        Receivable receivable = new Receivable();
        receivable.setCompany(company);
        receivable.setProject(project);
        receivable.setProperty(property);
        receivable.setCustomer(customer);
        receivable.setOwner(owner);
        receivable.setStore(project.getStore());
        receivable.setName("合同签约款");
        receivable.setStageName("合同签约");
        receivable.setType(ReceivableType.First);
        receivable.setAmount(amount);
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setDueDate(LocalDate.now());
        receivable.setStatus(ReceivableStatus.Pending);
        receivable.setInvoiceRequired(false);
        receivable.setCreatedBy(owner);
        return receivableRepository.save(receivable);
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
