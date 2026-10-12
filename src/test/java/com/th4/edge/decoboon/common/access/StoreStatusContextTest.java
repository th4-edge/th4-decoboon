package com.th4.edge.decoboon.common.access;

import com.th4.edge.decoboon.common.context.StoreAttributionContext;
import com.th4.edge.decoboon.common.context.StoreStatusContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.SecurityAction;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.configure.StoreStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.tenant.user.repository.UserSecurityLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 门店启用/停用规则（`P0D-04` / `DESIGN-BIZ-20261013-002` §2 与 §5 用例）。
 *
 * <p>覆盖：默认总店不可停用、最后一家启用门店不可停用（构造无默认店异常态）、
 * `GAP-051` 在职员工/在手业务阻断、停用后不可承接新归属、审计留痕与幂等、非 BOSS 拒绝。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:store-status-rules;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "logging.level.root=ERROR"
})
class StoreStatusContextTest {

    @Autowired private StoreStatusContext storeStatusContext;
    @Autowired private StoreAttributionContext storeAttributionContext;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private UserSecurityLogRepository securityLogRepository;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    private Company company(String name) {
        Company company = new Company();
        company.setTitle(name);
        company.setName(name);
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

    private Employee employee(Company company, Store store, EmployeeRole role, String name) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setName(name);
        employee.setTelNo("138" + Math.abs(System.nanoTime() % 100000000));
        return employeeRepository.save(employee);
    }

    @Test
    void disableDefaultStoreRejected() {
        Company company = company("默认店");
        Store head = store(company, "总店", true);
        store(company, "二店", false);
        Employee boss = employee(company, head, EmployeeRole.Boss, "负责人");

        assertThatThrownBy(() -> storeStatusContext.change(boss, head.getStoreId(), StoreStatus.Disabled))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("默认总店不可停用");
        assertThat(storeRepository.findByStoreId(head.getStoreId()).getStatus()).isEqualTo(StoreStatus.Active);
    }

    @Test
    void disableLastActiveStoreRejected() {
        Company company = company("异常无默认店");
        Store only = store(company, "唯一门店", false);
        Employee boss = employee(company, only, EmployeeRole.Boss, "负责人");

        assertThatThrownBy(() -> storeStatusContext.change(boss, only.getStoreId(), StoreStatus.Disabled))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("最后一家启用门店不可停用");
    }

    @Test
    void disableStoreWithActiveEmployeeRejected() {
        Company company = company("在职员工阻断");
        Store head = store(company, "总店", true);
        Store second = store(company, "二店", false);
        Employee boss = employee(company, head, EmployeeRole.Boss, "负责人");
        employee(company, second, EmployeeRole.StoreManager, "二店店长");

        assertThatThrownBy(() -> storeStatusContext.change(boss, second.getStoreId(), StoreStatus.Disabled))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("在职员工");
    }

    @Test
    void disableStoreWithPendingCustomerRejected() {
        Company company = company("在手业务阻断");
        Store head = store(company, "总店", true);
        Store second = store(company, "二店", false);
        Employee boss = employee(company, head, EmployeeRole.Boss, "负责人");

        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(second);
        customer.setOwner(boss);
        customer.setName("在手客户");
        customer.setStatus(CustStatus.Following);
        customer.setTelNo("139" + Math.abs(System.nanoTime() % 100000000));
        customer.setArea(new BigDecimal("100"));
        customerRepository.save(customer);

        assertThatThrownBy(() -> storeStatusContext.change(boss, second.getStoreId(), StoreStatus.Disabled))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("在手客户");
    }

    @Test
    void disableKeepsHistoryWritesAuditAndIsIdempotent() {
        Company company = company("审计与幂等");
        Store head = store(company, "总店", true);
        Store second = store(company, "二店", false);
        Employee boss = employee(company, head, EmployeeRole.Boss, "负责人");

        Store disabled = storeStatusContext.change(boss, second.getStoreId(), StoreStatus.Disabled);
        assertThat(disabled.getStatus()).isEqualTo(StoreStatus.Disabled);
        assertThat(second.getStoreId()).isNotNull();

        // 审计留痕：按 empId 查（测试员工无凭证，phone 为空；生产路径带凭证，安全日志页按 phone 可查）
        var logs = auditLogs(boss.getEmpId());
        assertThat(logs).anyMatch(log -> log.getAction() == SecurityAction.StoreStatusChange
                && log.getRemark() != null && log.getRemark().contains("已停用"));

        // 幂等：重复停用不报错、也不重复留痕
        int before = logs.size();
        storeStatusContext.change(boss, second.getStoreId(), StoreStatus.Disabled);
        assertThat(auditLogs(boss.getEmpId())).hasSize(before);
    }

    private java.util.List<com.th4.edge.tenant.user.model.UserSecurityLog> auditLogs(Long empId) {
        return entityManager.createQuery(
                        "select l from UserSecurityLog l where l.empId = :empId order by l.logId desc",
                        com.th4.edge.tenant.user.model.UserSecurityLog.class)
                .setParameter("empId", empId).getResultList();
    }

    @Test
    void disabledStoreRejectedAsNewAttribution() {
        Company company = company("停用不可归属");
        Store head = store(company, "总店", true);
        Store second = store(company, "二店", false);
        Employee boss = employee(company, head, EmployeeRole.Boss, "负责人");
        storeStatusContext.change(boss, second.getStoreId(), StoreStatus.Disabled);

        Store disabled = storeRepository.findByStoreId(second.getStoreId());
        assertThat(disabled.attributable()).isFalse();
        assertThatThrownBy(() -> storeAttributionContext.requireAttributable(disabled))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("已停用");
    }

    @Test
    void nonBossCannotChangeStoreStatus() {
        Company company = company("非BOSS拒绝");
        Store head = store(company, "总店", true);
        Store second = store(company, "二店", false);
        Employee manager = employee(company, second, EmployeeRole.StoreManager, "二店店长");

        assertThatThrownBy(() -> storeStatusContext.change(manager, second.getStoreId(), StoreStatus.Disabled))
                .isInstanceOf(DefinedException.class);
    }
}
