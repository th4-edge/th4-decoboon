package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.statistic.model.BossMeasureQuery;
import com.th4.edge.decoboon.statistic.model.BossMeasureReport;
import com.th4.edge.tenant.user.configure.EmployeeRole;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 量房预约转化经营聚合独立测试。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:boss-measure-report;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class BossMeasureReportContextTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private MeasureRepository measureRepository;
    @Autowired private RequirementRepository requirementRepository;
    @Autowired private com.th4.edge.decoboon.statistic.service.boss.measure.report.Report report;

    @Test
    void shouldAggregateAppointmentConversionByMarketerAndSource() {
        Company company = company();
        Store store = store(company);
        Employee boss = employee(company, store, EmployeeRole.Boss, "老板");
        Employee marketerA = employee(company, store, EmployeeRole.Marketer, "业务甲");
        Employee marketerB = employee(company, store, EmployeeRole.Marketer, "业务乙");
        Employee designer = employee(company, store, EmployeeRole.Designer, "设计师");

        Customer notScheduled = customer(company, store, marketerA, CustomerSource.Video, CustStatus.Following, "未预约客户");
        Customer scheduled = customer(company, store, marketerA, CustomerSource.Video, CustStatus.PendingMeasure, "已预约客户");
        Customer overdue = customer(company, store, marketerB, CustomerSource.Referral, CustStatus.PendingMeasure, "逾期客户");
        Customer handoffRisk = customer(company, store, marketerA, CustomerSource.Video, CustStatus.Quoted, "待交设计客户");
        Customer designStarted = customer(company, store, marketerB, CustomerSource.Referral, CustStatus.Quoted, "已交设计客户");

        property(notScheduled, marketerA, null, PropertyStatus.PendingMeasure);
        Property scheduledProperty = property(scheduled, marketerA, designer, PropertyStatus.Measuring);
        Property overdueProperty = property(overdue, marketerB, null, PropertyStatus.PendingMeasure);
        Property handoffProperty = property(handoffRisk, marketerA, designer, PropertyStatus.Measured);
        Property designProperty = property(designStarted, marketerB, designer, PropertyStatus.Measured);

        LocalDateTime now = LocalDateTime.now();
        measure(scheduledProperty, scheduled, marketerA, designer, MeasureStatus.Scheduled,
                now.plusHours(2), null, List.of());
        measure(overdueProperty, overdue, marketerB, null, MeasureStatus.Scheduled,
                now.minusHours(5), null, List.of());
        measure(handoffProperty, handoffRisk, marketerA, designer, MeasureStatus.Completed,
                now.minusHours(80), now.minusHours(72), List.of("site-1.jpg"));
        measure(designProperty, designStarted, marketerB, designer, MeasureStatus.Completed,
                now.minusHours(20), now.minusHours(10), List.of("site-2.jpg"));
        requirement(company, store, designStarted, designProperty, designer);

        Object target = AopTestUtils.getTargetObject(report);
        ReflectionTestUtils.setField(target, "employee", boss);
        BossMeasureReport result = ((com.th4.edge.decoboon.statistic.service.boss.measure.report.Report) target)
                .call(new BossMeasureQuery());

        assertThat(result.getItemCount()).isEqualTo(5);
        assertThat(result.getScheduledCount()).isEqualTo(4);
        assertThat(result.getCompletedCount()).isEqualTo(2);
        assertThat(result.getOverdue4hCount()).isEqualTo(1);
        assertThat(result.getUnassignedCount()).isEqualTo(1);
        assertThat(result.getHandoff48hCount()).isEqualTo(1);
        assertThat(result.getHandoffCount()).isEqualTo(1);
        assertThat(result.getAppointmentRate()).isEqualByComparingTo("80.0");
        assertThat(result.getCompletionRate()).isEqualByComparingTo("50.0");
        assertThat(result.getOverdueRate()).isEqualByComparingTo("25.0");
        assertThat(result.getHandoffRate()).isEqualByComparingTo("50.0");
        assertThat(result.getMarketerGroups()).hasSize(2);
        assertThat(result.getSourceGroups()).hasSize(2);
        assertThat(result.getDesignerGroups()).hasSize(1);
        assertThat(result.getItems()).filteredOn(item -> item.getCompletedAt() == null
                        && item.getScheduledAt() != null
                        && item.getScheduledAt().isBefore(LocalDateTime.now().minusHours(4)))
                .singleElement().satisfies(item -> {
                    assertThat(item.getMarketerName()).isEqualTo("业务乙");
                    assertThat(item.getSourceName()).isEqualTo("转介绍");
                });
    }

    private Company company() {
        Company company = new Company();
        company.setTitle("测试装企");
        company.setName("测试装企");
        company.setSize(Size.A);
        return companyRepository.save(company);
    }

    private Store store(Company company) {
        Store store = new Store();
        store.setCompany(company);
        store.setName("总店");
        store.setDefaultStore(true);
        return storeRepository.save(store);
    }

    private Employee employee(Company company, Store store, EmployeeRole role, String name) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setName(name);
        employee.setTelNo("1" + Math.abs(System.nanoTime() % 1_000_000_000L));
        return employeeRepository.save(employee);
    }

    private Customer customer(Company company, Store store, Employee owner,
                              CustomerSource source, CustStatus status, String name) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(owner);
        customer.setSource(source);
        customer.setStatus(status);
        customer.setName(name);
        customer.setTelNo("2" + Math.abs(System.nanoTime() % 1_000_000_000L));
        customer.setCommunity("测试小区");
        customer.setAddr("测试地址");
        customer.setArea(new BigDecimal("100"));
        return customerRepository.save(customer);
    }

    private Property property(Customer customer, Employee owner, Employee designer, PropertyStatus status) {
        Property property = new Property();
        property.setCompany(customer.getCompany());
        property.setStore(customer.getStore());
        property.setTeam(customer.getTeam());
        property.setOwner(owner);
        property.setSalesman(owner);
        property.setDesigner(designer);
        property.setCustomer(customer);
        property.setPropertyNo("P-" + System.nanoTime());
        property.setCommunity(customer.getCommunity());
        property.setBuilding("1栋");
        property.setRoomNo("101");
        property.setAddress(customer.getAddr());
        property.setArea(customer.getArea());
        property.setLayout("三室两厅");
        property.setStatus(status);
        return propertyRepository.save(property);
    }

    private Measure measure(Property property, Customer customer, Employee owner, Employee designer,
                            MeasureStatus status, LocalDateTime scheduledAt, LocalDateTime completedAt,
                            List<String> photos) {
        Measure measure = new Measure();
        measure.setCompany(property.getCompany());
        measure.setStore(property.getStore());
        measure.setTeam(property.getTeam());
        measure.setOwner(owner);
        measure.setCustomer(customer);
        measure.setProperty(property);
        measure.setDesigner(designer);
        measure.setExecutor(designer);
        measure.setStatus(status);
        measure.setScheduledAt(scheduledAt);
        measure.setCompletedAt(completedAt);
        measure.setMeasureTime(completedAt == null ? scheduledAt : completedAt);
        measure.setSitePhotos(photos);
        return measureRepository.save(measure);
    }

    private Requirement requirement(Company company, Store store, Customer customer,
                                    Property property, Employee owner) {
        Requirement requirement = new Requirement();
        requirement.setCompany(company);
        requirement.setStore(store);
        requirement.setOwner(owner);
        requirement.setCustomer(customer);
        requirement.setProperty(property);
        requirement.setTitle("测试诉求");
        return requirementRepository.save(requirement);
    }
}
