package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerDormantReason;
import com.th4.edge.decoboon.customer.context.CustomerDormantContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 客户休眠量房门禁只应拦截未来预约。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:customer-dormant-measure-window;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class CustomerDormantMeasureWindowTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private MeasureRepository measureRepository;
    @Autowired private CustomerDormantContext dormantContext;

    @Test
    void shouldBlockFutureMeasureAndAllowPastMeasure() {
        Company company = new Company();
        company.setTitle("休眠量房窗口测试企业");
        company.setName("测试企业");
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
        boss.setName("测试负责人");
        boss.setTelNo("13800000001");
        boss = employeeRepository.save(boss);

        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(boss);
        customer.setStatus(CustStatus.PendingMeasure);
        customer.setName("测试客户");
        customer.setTelNo("13900000001");
        customer.setCommunity("测试小区");
        customer.setAddr("测试地址");
        customer.setArea(new BigDecimal("100"));
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(boss);
        property.setCustomer(customer);
        property.setPropertyNo("P-001");
        property.setAddress("测试地址");
        property.setStatus(PropertyStatus.PendingMeasure);
        property = propertyRepository.save(property);

        LocalDateTime future = LocalDateTime.now().plusDays(1);
        Measure measure = new Measure();
        measure.setCompany(company);
        measure.setStore(store);
        measure.setOwner(boss);
        measure.setCustomer(customer);
        measure.setProperty(property);
        measure.setStatus(MeasureStatus.Scheduled);
        measure.setScheduledAt(future);
        measure.setMeasureTime(future);
        measure = measureRepository.saveAndFlush(measure);

        Customer target = customer;
        Employee operator = boss;
        assertThatThrownBy(() -> dormantContext.setDormant(target, operator,
                CustomerDormantReason.PlanPostponed, "计划延期", CustStatus.PendingMeasure, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("未来量房预约");

        LocalDateTime past = LocalDateTime.now().minusHours(1);
        measure.setScheduledAt(past);
        measure.setMeasureTime(past);
        measureRepository.saveAndFlush(measure);

        Customer saved = dormantContext.setDormant(target, operator,
                CustomerDormantReason.PlanPostponed, "计划延期", CustStatus.PendingMeasure, null);
        assertThat(saved.getStatus()).isEqualTo(CustStatus.Dormant);
    }
}
