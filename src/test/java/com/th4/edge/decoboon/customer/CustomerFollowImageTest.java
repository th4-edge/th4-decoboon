package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.decoboon.common.file.context.FileAuthorizationContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.FollowWay;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.exception.DefinedException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 客户跟进图片持久化、批量回读和私有文件授权。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:customer-follow-image;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class CustomerFollowImageTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private CustomerFollowRepository followRepository;
    @Autowired private FileAuthorizationContext fileAuthorizationContext;

    @Test
    void shouldPersistImagesBatchLoadAndAuthorizeVisibleCustomer() {
        Company company = company("客户跟进图片测试企业");
        Store store = store(company);
        Employee boss = employee(company, store, EmployeeRole.Boss, "测试负责人");
        Customer customer = customer(company, store, boss, "图片跟进客户");
        String first = imagePath(company.getComId(), "first");
        String second = imagePath(company.getComId(), "second");

        CustomerFollow follow = new CustomerFollow();
        follow.setCompany(company);
        follow.setStore(store);
        follow.setCustomer(customer);
        follow.setEmp(boss);
        follow.setWay(FollowWay.Phone);
        follow.setContent("客户确认周六到店");
        follow.setRequestId("req-image-1");
        follow.setImages(List.of(first, second));
        follow = followRepository.saveAndFlush(follow);

        List<CustomerFollow> loaded = followRepository.findWithImagesByFollowIds(List.of(follow.getFollowId()));
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getImages()).containsExactly(first, second);

        var page = followRepository.findPageForDisplay(customer.getCustId(), PageRequest.of(0, 20));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getFollowId()).isEqualTo(follow.getFollowId());
        assertThat(followRepository.findByCompany_ComIdAndRequestId(company.getComId(), "req-image-1")).isPresent();
        assertThat(followRepository.existsByImagePath(first)).isTrue();

        FileAuthorizationContext.Authorization authorization = fileAuthorizationContext.authorize(
                boss, FileScope.CustomerFollow, follow.getFollowId(), Set.of(first, second));
        assertThat(authorization.projectId()).isNull();
        assertThat(authorization.paths()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void shouldRejectCrossCompanyFollowImageAuthorization() {
        Company companyA = company("图片授权企业A");
        Store storeA = store(companyA);
        Employee bossA = employee(companyA, storeA, EmployeeRole.Boss, "A负责人");
        Customer customerA = customer(companyA, storeA, bossA, "A客户");
        String path = imagePath(companyA.getComId(), "private");

        CustomerFollow follow = new CustomerFollow();
        follow.setCompany(companyA);
        follow.setStore(storeA);
        follow.setCustomer(customerA);
        follow.setEmp(bossA);
        follow.setWay(FollowWay.Phone);
        follow.setContent("A企业跟进");
        follow.setRequestId("req-image-a");
        follow.setImages(List.of(path));
        follow = followRepository.saveAndFlush(follow);

        Company companyB = company("图片授权企业B");
        Store storeB = store(companyB);
        Employee bossB = employee(companyB, storeB, EmployeeRole.Boss, "B负责人");
        Long followId = follow.getFollowId();

        assertThatThrownBy(() -> fileAuthorizationContext.authorize(
                bossB, FileScope.CustomerFollow, followId, Set.of(path)))
                .isInstanceOf(DefinedException.class);
    }

    private Company company(String title) {
        Company company = new Company();
        company.setTitle(title);
        company.setName(title);
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
        employee.setStatus(EmployeeStatus.Active);
        employee.setName(name);
        employee.setTelNo("1380000" + String.format("%04d", employeeRepository.count() + 1));
        return employeeRepository.save(employee);
    }

    private Customer customer(Company company, Store store, Employee owner, String name) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(owner);
        customer.setStatus(CustStatus.Following);
        customer.setName(name);
        customer.setTelNo("1390000" + String.format("%04d", customerRepository.count() + 1));
        return customerRepository.save(customer);
    }

    private String imagePath(Long companyId, String suffix) {
        return "companies/" + companyId + "/files/customerfollow/2026/10/" + suffix + ".jpg";
    }
}
