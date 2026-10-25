package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteFinanceAggregateContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteDesignerAggregate;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DEC-036 / DEC-065：财务报价按设计师聚合（聚合 SQL 直接投影）与财务数据范围（总部/门店）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finance-quote-aggregate;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/finance-quote-aggregate-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class FinanceQuoteAggregateTest {

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private QuoteRepository quoteRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private DataScopeContext dataScopeContext;
    @Autowired
    private QuoteFinanceAggregateContext aggregateContext;

    private Fixture fixture;

    @BeforeEach
    void setUp() {
        fixture = createFixture();
        entityManager.flush();
    }

    @Test
    void aggregatesByDesignerWithCostCompleteRules() {
        DataScope head = dataScopeContext.resolve(fixture.headFinance());
        List<QuoteDesignerAggregate> rows = aggregateContext.aggregate(head, null);

        assertThat(rows).hasSize(2);
        QuoteDesignerAggregate designerA = rows.stream()
                .filter(r -> r.getDesignerName().equals("聚合测试设计师A"))
                .findFirst().orElseThrow();
        assertThat(designerA.getQuoteCount()).isEqualTo(2);
        assertThat(designerA.getQuoteAmount()).isEqualByComparingTo("150.00");
        assertThat(designerA.getProfitAmount()).isEqualByComparingTo("20.00");
        assertThat(designerA.getCostIncompleteCount()).isEqualTo(1);

        QuoteDesignerAggregate designerB = rows.stream()
                .filter(r -> r.getDesignerName().equals("聚合测试设计师B"))
                .findFirst().orElseThrow();
        assertThat(designerB.getQuoteCount()).isEqualTo(1);
        assertThat(designerB.getQuoteAmount()).isEqualByComparingTo("200.00");
        assertThat(designerB.getProfitAmount()).isEqualByComparingTo("50.00");
        assertThat(designerB.getCostIncompleteCount()).isEqualTo(0);
    }

    @Test
    void headFinanceSeesAllDesigners() {
        DataScope head = dataScopeContext.resolve(fixture.headFinance());
        assertThat(aggregateContext.aggregate(head, null)).hasSize(2);
    }

    @Test
    void storeFinanceSeesOnlyOwnStoreDesigners() {
        DataScope store = dataScopeContext.resolve(fixture.storeFinance());
        List<QuoteDesignerAggregate> rows = aggregateContext.aggregate(store, null);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getDesignerName()).isEqualTo("聚合测试设计师B");
        assertThat(rows.get(0).getQuoteCount()).isEqualTo(1);
    }

    private Fixture createFixture() {
        long seq = System.nanoTime();
        Company company = company(seq);
        Store storeA = store(company, "总店", true);
        Store storeB = store(company, "城北店", false);

        Employee designerA = employee(company, storeA, EmployeeRole.Designer, "聚合测试设计师A", seq, 1);
        Employee designerB = employee(company, storeB, EmployeeRole.Designer, "聚合测试设计师B", seq, 2);
        Employee headFinance = employee(company, storeA, EmployeeRole.Finance, "聚合测试总部财务", seq, 3);
        Employee storeFinance = employee(company, storeB, EmployeeRole.Finance, "聚合测试门店财务", seq, 4);

        quote(company, storeA, designerA, designerA, seq, 11, "100.00", "80.00", true, QuoteStatus.Accepted);
        quote(company, storeA, designerA, designerA, seq, 12, "50.00", "0.00", false, QuoteStatus.Draft);
        quote(company, storeB, designerB, designerB, seq, 13, "200.00", "150.00", true, QuoteStatus.Accepted);

        return new Fixture(company, storeA, storeB, designerA, designerB, headFinance, storeFinance);
    }

    private Company company(long seq) {
        Company company = new Company();
        company.setTitle("聚合测试企业" + Math.abs(seq % 1000));
        company.setName("聚合测试企业");
        company.setSize(Size.A);
        return companyRepository.save(company);
    }

    private Store store(Company company, String name, boolean defaultStore) {
        Store store = new Store();
        store.setCompany(company);
        store.setName(name);
        store.setDefaultStore(defaultStore);
        return storeRepository.save(store);
    }

    private Employee employee(Company company, Store store, EmployeeRole role, String name, long seq, int offset) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setName(name);
        employee.setTelNo("3" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        employee.setJoinedAt(LocalDateTime.now());
        return employeeRepository.save(employee);
    }

    private void quote(Company company, Store store, Employee operator, Employee createdBy, long seq, int offset,
                       String amount, String cost, boolean costComplete, QuoteStatus status) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(operator);
        customer.setName("聚合测试客户" + offset);
        customer.setTelNo("4" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        customer.setArea(new BigDecimal("120"));
        customer.setAddr("聚合测试地址");
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(operator);
        property.setCustomer(customer);
        property.setPropertyNo("PROP-AGG-" + offset + "-" + Math.abs(seq % 100000));
        property.setAddress("聚合测试小区 " + offset + " 栋 101");
        property.setArea(new BigDecimal("120"));
        property = propertyRepository.save(property);

        Quote quote = new Quote();
        quote.setNo("QUOTE-AGG-" + offset + "-" + Math.abs(seq % 100000));
        quote.setTitle("聚合测试报价");
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(createdBy);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(new BigDecimal("120"));
        quote.setSubtotal(amount == null ? BigDecimal.ZERO : new BigDecimal(amount));
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setAmount(amount == null ? BigDecimal.ZERO : new BigDecimal(amount));
        quote.setCost(cost == null ? BigDecimal.ZERO : new BigDecimal(cost));
        quote.setCostComplete(costComplete);
        quote.setDesignFee(BigDecimal.ZERO);
        quote.setManagementFee(BigDecimal.ZERO);
        quote.setStatus(status);
        quoteRepository.save(quote);
    }

    private record Fixture(Company company, Store storeA, Store storeB,
                           Employee designerA, Employee designerB,
                           Employee headFinance, Employee storeFinance) {
    }
}
