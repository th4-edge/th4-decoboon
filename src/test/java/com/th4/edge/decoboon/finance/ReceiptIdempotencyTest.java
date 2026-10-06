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
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 客户收款幂等回归测试（DEF-PROJ37-002）。
 *
 * <p>背景：余额校验曾排在幂等键查找之前，同一笔收款的重复提交在核销完成后被
 * 「收款金额不能超过应收余额」拦截，无法返回原收款记录。本测试锁定：重复提交返回原收款、
 * 不重复核销、不重复记账；同时保证新的超额收款事实仍被阻断。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:receipt-idempotency;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class ReceiptIdempotencyTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ReceivableRepository receivableRepository;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private com.th4.edge.decoboon.finance.service.receipts.save.Save receiptSave;

    @BeforeEach
    void unwrapProviderProxy() {
        receiptSave = AopTestUtils.getTargetObject(receiptSave);
    }

    @Test
    void shouldReturnOriginalReceiptWhenRepeatingSameCollection() {
        Fixture fixture = fixture("repeat-full", new BigDecimal("1000.00"));

        Receipt first = collect(fixture, new BigDecimal("1000.00"), null, "业主甲");
        Receipt repeat = collect(fixture, new BigDecimal("1000.00"), null, "业主甲");

        assertThat(first.getReceiptId()).isNotNull();
        assertThat(repeat.getReceiptId()).isEqualTo(first.getReceiptId());
        assertThat(receiptRepository.count()).isEqualTo(1);
        assertThat(ledgerRepository.count()).isEqualTo(1);

        Receivable receivable = receivableRepository.findById(fixture.receivable().getReceivableId()).orElseThrow();
        assertThat(receivable.getReceivedAmount()).isEqualByComparingTo("1000.00");
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.Received);
    }

    @Test
    void shouldReturnOriginalReceiptWhenClientIdempotencyKeyIsRepeated() {
        Fixture fixture = fixture("repeat-client-key", new BigDecimal("1000.00"));

        Receipt first = collect(fixture, new BigDecimal("400.00"), "client-key-0001", "业主乙");
        Receipt repeat = collect(fixture, new BigDecimal("400.00"), "client-key-0001", "业主乙");

        assertThat(repeat.getReceiptId()).isEqualTo(first.getReceiptId());
        assertThat(receiptRepository.count()).isEqualTo(1);
        assertThat(ledgerRepository.count()).isEqualTo(1);
        assertThat(receivableRepository.findById(fixture.receivable().getReceivableId()).orElseThrow()
                .getReceivedAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    void shouldAllowTwoRealCollectionsWhenSameBusinessUsesDifferentClientKeys() {
        Fixture fixture = fixture("repeat-real", new BigDecimal("1000.00"));

        Receipt first = collect(fixture, new BigDecimal("400.00"), "client-key-real-1", "业主甲");
        Receipt second = collect(fixture, new BigDecimal("400.00"), "client-key-real-2", "业主甲");

        assertThat(second.getReceiptId()).isNotEqualTo(first.getReceiptId());
        assertThat(receiptRepository.count()).isEqualTo(2);
        assertThat(ledgerRepository.count()).isEqualTo(2);
        assertThat(receivableRepository.findById(fixture.receivable().getReceivableId()).orElseThrow()
                .getReceivedAmount()).isEqualByComparingTo("800.00");
    }

    @Test
    void shouldRejectReusedClientKeyWhenBusinessFingerprintDiffers() {
        Fixture fixture = fixture("reuse-client-key", new BigDecimal("1000.00"));
        collect(fixture, new BigDecimal("300.00"), "client-key-conflict", "业主甲");

        assertDefined(409, "该幂等键已被其他收付款占用",
                () -> collect(fixture, new BigDecimal("400.00"), "client-key-conflict", "业主甲"));
        assertThat(receiptRepository.count()).isEqualTo(1);
        assertThat(ledgerRepository.count()).isEqualTo(1);
        assertThat(receivableRepository.findById(fixture.receivable().getReceivableId()).orElseThrow()
                .getReceivedAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    void shouldStillRejectOverPaymentForNewBusinessFact() {
        Fixture fixture = fixture("over-payment", new BigDecimal("1000.00"));
        collect(fixture, new BigDecimal("600.00"), null, "业主丙");

        // 不同付款人 = 新的业务事实（不同幂等键），应收余额只剩 400，应被阻断。
        assertDefined(400, "收款金额不能超过应收余额",
                () -> collect(fixture, new BigDecimal("600.00"), null, "业主丁"));
        assertThat(receiptRepository.count()).isEqualTo(1);
        assertThat(ledgerRepository.count()).isEqualTo(1);
        assertThat(receivableRepository.findById(fixture.receivable().getReceivableId()).orElseThrow()
                .getStatus()).isEqualTo(ReceivableStatus.Partial);
    }

    @Test
    void shouldRejectNonPositiveAmountWithoutWritingAnything() {
        Fixture fixture = fixture("non-positive", new BigDecimal("1000.00"));
        assertDefined(400, "收款金额必须大于 0",
                () -> collect(fixture, BigDecimal.ZERO, null, "业主戊"));
        assertThat(receiptRepository.count()).isZero();
        assertThat(ledgerRepository.count()).isZero();
    }

    private Receipt collect(Fixture fixture, BigDecimal amount, String idempotencyKey, String payerName) {
        ReflectionTestUtils.setField(receiptSave, "employee", fixture.finance());
        ReflectionTestUtils.setField(receiptSave, "projectId", fixture.project().getProjectId());
        ReflectionTestUtils.setField(receiptSave, "receivableId", fixture.receivable().getReceivableId());
        ReflectionTestUtils.setField(receiptSave, "amount", amount);
        ReflectionTestUtils.setField(receiptSave, "account", FundAccount.CompanyBank);
        ReflectionTestUtils.setField(receiptSave, "receivedAt", LocalDate.now());
        ReflectionTestUtils.setField(receiptSave, "payerName", payerName);
        ReflectionTestUtils.setField(receiptSave, "voucherUrl", null);
        ReflectionTestUtils.setField(receiptSave, "remark", "收款幂等回归测试");
        ReflectionTestUtils.setField(receiptSave, "idempotencyKey", idempotencyKey);
        return receiptSave.call(new Receipt());
    }

    private void assertDefined(int code, String messagePart,
                               org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOfSatisfying(DefinedException.class, error -> {
            assertThat(error.getErrorCode().getCode()).isEqualTo(String.valueOf(code));
            assertThat(error.getMessage()).contains(messagePart);
        });
    }

    private record Fixture(Company company, Store store, Employee finance, Customer customer,
                           Property property, Project project, Receivable receivable) {
    }

    private Fixture fixture(String prefix, BigDecimal receivableAmount) {
        String key = Long.toHexString(System.nanoTime());
        Company company = company("企" + key);
        Store store = store(company);
        Employee finance = employee(company, store, EmployeeRole.Finance);
        Customer customer = customer(company, store, finance, key);
        Property property = property(company, store, customer, finance, key);
        Quote quote = quote(company, store, customer, property, finance, key);
        Project project = project(company, store, customer, property, quote, finance, key);
        Receivable receivable = receivable(company, project, property, customer, finance);
        receivable.setAmount(receivableAmount);
        receivable = receivableRepository.save(receivable);
        return new Fixture(company, store, finance, customer, property, project, receivable);
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

    private Receivable receivable(Company company, Project project, Property property,
                                  Customer customer, Employee owner) {
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
        receivable.setAmount(BigDecimal.ZERO);
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setDueDate(LocalDate.now());
        receivable.setStatus(ReceivableStatus.Pending);
        receivable.setInvoiceRequired(false);
        receivable.setCreatedBy(owner);
        return receivableRepository.save(receivable);
    }
}
