package com.th4.edge.decoboon.property;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
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

/** 房屋级预约与完成时间生命周期测试。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:measure-schedule-fact;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class MeasureScheduleFactTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private MeasureRepository measureRepository;
    @Autowired private com.th4.edge.decoboon.property.service.measures.save.Save measureSave;

    @Test
    void shouldKeepScheduledAtAndMakeCompletionIdempotent() {
        Company company = new Company();
        company.setTitle("测试装企");
        company.setName("测试装企");
        company.setSize(Size.A);
        company = companyRepository.save(company);

        Store store = new Store();
        store.setCompany(company);
        store.setName("总店");
        store.setDefaultStore(true);
        store = storeRepository.save(store);

        Employee boss = new Employee();
        boss.setCompany(company);
        boss.setStore(store);
        boss.setRole(EmployeeRole.Boss);
        boss.setName("老板");
        boss.setTelNo("13800000000");
        boss = employeeRepository.save(boss);

        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(boss);
        customer.setDesigner(boss);
        customer.setSource(CustomerSource.Other);
        customer.setStatus(CustStatus.PendingMeasure);
        customer.setName("测试客户");
        customer.setTelNo("13900000000");
        customer.setCommunity("测试小区");
        customer.setAddr("测试地址");
        customer.setArea(new BigDecimal("100"));
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(boss);
        property.setSalesman(boss);
        property.setDesigner(boss);
        property.setCustomer(customer);
        property.setPropertyNo("P-001");
        property.setCommunity(customer.getCommunity());
        property.setBuilding("1栋");
        property.setRoomNo("101");
        property.setAddress(customer.getAddr());
        property.setArea(customer.getArea());
        property.setLayout("三室两厅");
        property.setStatus(PropertyStatus.PendingMeasure);
        property = propertyRepository.save(property);

        LocalDateTime scheduledAt = LocalDateTime.now().plusHours(2);
        com.th4.edge.decoboon.property.service.measures.save.Save saveTarget =
                (com.th4.edge.decoboon.property.service.measures.save.Save) AopTestUtils.getTargetObject(measureSave);
        setSaveFields(saveTarget, boss, property.getPropertyId(), MeasureStatus.Scheduled,
                scheduledAt, null, List.of());
        Measure scheduled = saveTarget.call(new Measure());
        assertThat(scheduled.getScheduledAt()).isEqualTo(scheduledAt);
        assertThat(scheduled.getCompletedAt()).isNull();

        LocalDateTime completedAt = scheduledAt.plusHours(3);
        setSaveFields(saveTarget, boss, property.getPropertyId(), MeasureStatus.Completed,
                scheduledAt, completedAt, List.of("site.jpg"));
        Measure completed = saveTarget.call(new Measure());
        assertThat(completed.getMeasureId()).isEqualTo(scheduled.getMeasureId());
        assertThat(completed.getScheduledAt()).isEqualTo(scheduledAt);
        assertThat(completed.getCompletedAt()).isEqualTo(completedAt);
        assertThat(completed.getMeasureTime()).isEqualTo(completedAt);

        setSaveFields(saveTarget, boss, property.getPropertyId(), MeasureStatus.Completed,
                scheduledAt, completedAt.plusHours(5), List.of("other.jpg"));
        Measure repeated = saveTarget.call(new Measure());
        assertThat(repeated.getScheduledAt()).isEqualTo(scheduledAt);
        assertThat(repeated.getCompletedAt()).isEqualTo(completedAt);
        assertThat(repeated.getSitePhotos()).containsExactly("site.jpg");
    }

    private void setSaveFields(Object target, Employee employee, Long propertyId, MeasureStatus status,
                               LocalDateTime scheduledAt, LocalDateTime completedAt, List<String> photos) {
        ReflectionTestUtils.setField(target, "employee", employee);
        ReflectionTestUtils.setField(target, "propertyId", propertyId);
        ReflectionTestUtils.setField(target, "status", status);
        ReflectionTestUtils.setField(target, "scheduledAt", scheduledAt);
        ReflectionTestUtils.setField(target, "completedAt", completedAt);
        ReflectionTestUtils.setField(target, "measureTime", completedAt == null ? scheduledAt : completedAt);
        ReflectionTestUtils.setField(target, "sitePhotos", new java.util.ArrayList<>(photos));
        ReflectionTestUtils.setField(target, "structureImages", new java.util.ArrayList<>());
        ReflectionTestUtils.setField(target, "attachments", new java.util.ArrayList<>());
    }
}
