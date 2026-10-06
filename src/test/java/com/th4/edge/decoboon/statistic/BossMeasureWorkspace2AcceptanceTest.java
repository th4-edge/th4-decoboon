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
import com.th4.edge.decoboon.statistic.model.BossMeasureItem;
import com.th4.edge.decoboon.statistic.model.BossMeasurePeriod;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/** 预约量房工作台 2.0 的独立验收探针。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:boss-measure-workspace-2-acceptance;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class BossMeasureWorkspace2AcceptanceTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private MeasureRepository measureRepository;
    @Autowired private RequirementRepository requirementRepository;
    @Autowired private com.th4.edge.decoboon.statistic.service.boss.measure.report.Report report;

    @Test
    void shouldDeriveSevenBusinessStatesWithoutIntroducingWorkflowStates() {
        Fixture f = fixture("状态派生");
        LocalDateTime now = LocalDateTime.now();

        Customer notScheduled = customer(f, CustStatus.Following, "未预约");
        property(notScheduled, null, PropertyStatus.PendingMeasure);

        Customer unassigned = customer(f, CustStatus.PendingMeasure, "待派单");
        Property unassignedProperty = property(unassigned, null, PropertyStatus.PendingMeasure);
        measure(f, unassignedProperty, unassigned, null, MeasureStatus.Scheduled, now.plusHours(2), null);

        Customer scheduled = customer(f, CustStatus.PendingMeasure, "待上门");
        Property scheduledProperty = property(scheduled, f.designer, PropertyStatus.Measuring);
        measure(f, scheduledProperty, scheduled, f.designer, MeasureStatus.Scheduled, now.plusHours(2), null);

        Customer overdue = customer(f, CustStatus.PendingMeasure, "预约逾期");
        Property overdueProperty = property(overdue, f.designer, PropertyStatus.Measuring);
        measure(f, overdueProperty, overdue, f.designer, MeasureStatus.Scheduled, now.minusHours(5), null);

        Customer pendingHandoff = customer(f, CustStatus.Quoted, "完成待交设计");
        Property pendingProperty = property(pendingHandoff, f.designer, PropertyStatus.Measured);
        measure(f, pendingProperty, pendingHandoff, f.designer, MeasureStatus.Completed,
                now.minusHours(3), now.minusHours(1));

        Customer handoffRisk = customer(f, CustStatus.Quoted, "待交设计风险");
        Property riskProperty = property(handoffRisk, f.designer, PropertyStatus.Measured);
        measure(f, riskProperty, handoffRisk, f.designer, MeasureStatus.Completed,
                now.minusHours(50), now.minusHours(49));

        Customer designStarted = customer(f, CustStatus.Quoted, "已交设计");
        Property designProperty = property(designStarted, f.designer, PropertyStatus.Measured);
        measure(f, designProperty, designStarted, f.designer, MeasureStatus.Completed,
                now.minusHours(20), now.minusHours(10));
        requirement(f, designStarted, designProperty);

        BossMeasureReport result = callAs(report, f.boss);

        Map<String, BossMeasureItem> byCustomer = result.getItems().stream()
                .collect(Collectors.toMap(BossMeasureItem::getCustomerName, Function.identity()));
        assertThat(byCustomer.keySet()).containsExactlyInAnyOrder(
                "未预约", "待派单", "待上门", "预约逾期", "完成待交设计", "待交设计风险", "已交设计");
        assertThat(byCustomer.get("未预约").getDerivedState()).isEqualTo("NotScheduled");
        assertThat(byCustomer.get("待派单").getDerivedState()).isEqualTo("Unassigned");
        assertThat(byCustomer.get("待上门").getDerivedState()).isEqualTo("Scheduled");
        assertThat(byCustomer.get("预约逾期").getDerivedState()).isEqualTo("Overdue");
        assertThat(byCustomer.get("完成待交设计").getDerivedState()).isEqualTo("CompletedPendingHandoff");
        assertThat(byCustomer.get("待交设计风险").getDerivedState()).isEqualTo("HandoffRisk");
        assertThat(byCustomer.get("已交设计").getDerivedState()).isEqualTo("DesignStarted");
        assertThat(result.getScheduledCount()).isEqualTo(6);
        assertThat(result.getCompletedCount()).isEqualTo(3);
        assertThat(result.getUnassignedCount()).isEqualTo(1);
        assertThat(result.getOverdue4hCount()).isEqualTo(1);
        assertThat(result.getHandoff48hCount()).isEqualTo(1);
        assertThat(result.getHandoffCount()).isEqualTo(1);
    }

    @Test
    void shouldCountTodayScheduledAppointmentsEvenWhenLaterOrAlreadyCompleted() {
        Fixture f = fixture("今日预约口径");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayEnd = now.toLocalDate().atTime(23, 59, 59);

        Customer laterToday = customer(f, CustStatus.PendingMeasure, "今日稍后");
        Property laterProperty = property(laterToday, f.designer, PropertyStatus.Measuring);
        measure(f, laterProperty, laterToday, f.designer, MeasureStatus.Scheduled, todayEnd, null);

        Customer completedToday = customer(f, CustStatus.Quoted, "今日已完成");
        Property completedProperty = property(completedToday, f.designer, PropertyStatus.Measured);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        measure(f, completedProperty, completedToday, f.designer, MeasureStatus.Completed,
                todayStart, todayStart);

        BossMeasureReport result = callAs(report, f.boss);
        BossMeasurePeriod today = result.getPeriods().stream()
                .filter(period -> "today".equals(period.getKey())).findFirst().orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(result.getTodayCount())
                    .as("todayCount 应包含今天已预约且已完成的量房")
                    .isEqualTo(2);
            softly.assertThat(today.getScheduledCount())
                    .as("今天的未来预约也必须属于今日预约")
                    .isEqualTo(2);
            softly.assertThat(today.getCompletedCount()).isEqualTo(1);
        });
    }

    @Test
    void shouldKeepSignedCustomerMeasureEventsInEnterpriseDateWindows() {
        Fixture f = fixture("签约客户事件");
        // 使用当天固定白天时点，避免凌晨执行时 now.minusHours(2) 落到前一天导致日期窗口 flake。
        LocalDateTime now = LocalDate.now().atTime(10, 0);

        Customer signed = customer(f, CustStatus.Signed, "已签约客户");
        Property property = property(signed, f.designer, PropertyStatus.Measured);
        measure(f, property, signed, f.designer, MeasureStatus.Completed, now.minusHours(2), now.minusMinutes(30));

        BossMeasureReport result = callAs(report, f.boss);

        assertThat(result.getItems()).extracting(BossMeasureItem::getCustomerName).contains("已签约客户");
        BossMeasurePeriod today = result.getPeriods().stream()
                .filter(period -> "today".equals(period.getKey())).findFirst().orElseThrow();
        assertThat(today.getScheduledCount()).as("全企业日期窗口不能按客户当前状态截断历史量房事实").isEqualTo(1);
        assertThat(today.getCompletedCount()).isEqualTo(1);
    }

    @Test
    void marketerShouldOnlySeeOwnCustomerMeasureFacts() {
        Fixture f = fixture("业务员数据范围");
        LocalDateTime now = LocalDateTime.now();

        Customer mine = customer(f, CustStatus.PendingMeasure, "我的客户");
        Property myProperty = property(mine, f.designer, PropertyStatus.Measuring);
        measure(f, myProperty, mine, f.designer, MeasureStatus.Scheduled, now.plusHours(2), null);

        Employee otherMarketer = employee(f.company, f.store, EmployeeRole.Marketer, "其他业务员");
        Customer other = customer(f.company, f.store, otherMarketer, CustomerSource.Referral,
                CustStatus.PendingMeasure, "他人客户");
        Property otherProperty = property(other, f.designer, PropertyStatus.Measuring);
        measure(f, otherProperty, other, f.designer, MeasureStatus.Scheduled, now.plusHours(2), null);

        BossMeasureReport result = callAs(report, f.marketer);
        assertThat(result.getItems()).extracting(BossMeasureItem::getCustomerName).containsExactly("我的客户");
    }

    private BossMeasureReport callAs(
            com.th4.edge.decoboon.statistic.service.boss.measure.report.Report reportTarget,
            Employee employee) {
        Object target = AopTestUtils.getTargetObject(reportTarget);
        ReflectionTestUtils.setField(target, "employee", employee);
        BossMeasureQuery query = new BossMeasureQuery();
        return ((com.th4.edge.decoboon.statistic.service.boss.measure.report.Report) target).call(query);
    }

    private Fixture fixture(String prefix) {
        Company company = company(prefix);
        Store store = store(company, prefix);
        Employee boss = employee(company, store, EmployeeRole.Boss, prefix + "老板");
        Employee marketer = employee(company, store, EmployeeRole.Marketer, prefix + "业务员");
        Employee designer = employee(company, store, EmployeeRole.Designer, prefix + "设计师");
        return new Fixture(company, store, boss, marketer, designer);
    }

    private Company company(String prefix) {
        Company company = new Company();
        company.setTitle(prefix + "装企");
        company.setName(prefix + "装企");
        company.setSize(Size.A);
        return companyRepository.save(company);
    }

    private Store store(Company company, String prefix) {
        Store store = new Store();
        store.setCompany(company);
        store.setName(prefix + "总店");
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

    private Customer customer(Fixture f, CustStatus status, String name) {
        return customer(f.company, f.store, f.marketer, CustomerSource.Video, status, name);
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
        customer.setCommunity(name + "小区");
        customer.setAddr(name + "地址");
        customer.setArea(new BigDecimal("100"));
        return customerRepository.save(customer);
    }

    private Property property(Customer customer, Employee designer, PropertyStatus status) {
        Property property = new Property();
        property.setCompany(customer.getCompany());
        property.setStore(customer.getStore());
        property.setTeam(customer.getTeam());
        property.setOwner(customer.getOwner());
        property.setSalesman(customer.getOwner());
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

    private Measure measure(Fixture f, Property property, Customer customer, Employee designer,
                            MeasureStatus status, LocalDateTime scheduledAt, LocalDateTime completedAt) {
        return measure(f, property, customer, designer, status, scheduledAt, completedAt, List.of());
    }

    private Measure measure(Fixture f, Property property, Customer customer, Employee designer,
                            MeasureStatus status, LocalDateTime scheduledAt, LocalDateTime completedAt,
                            List<String> photos) {
        Measure measure = new Measure();
        measure.setCompany(f.company);
        measure.setStore(f.store);
        measure.setOwner(f.marketer);
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

    private Requirement requirement(Fixture f, Customer customer, Property property) {
        Requirement requirement = new Requirement();
        requirement.setCompany(f.company);
        requirement.setStore(f.store);
        requirement.setOwner(f.designer);
        requirement.setCustomer(customer);
        requirement.setProperty(property);
        requirement.setTitle("测试诉求");
        return requirementRepository.save(requirement);
    }

    private record Fixture(Company company, Store store, Employee boss, Employee marketer, Employee designer) {
    }
}
