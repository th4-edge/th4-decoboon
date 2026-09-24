package com.th4.edge.decoboon.labor;

import com.th4.edge.decoboon.labor.configure.WorkerStatus;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.labor.repository.WorkerRepository;
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
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 工人档案详情验收：按主键单条读取（不受列表分页影响），并按角色裁剪敏感字段。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:worker-info;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "spring.main.banner-mode=off",
        "th4.edge.components.lock.enabled=false",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class WorkerInfoAcceptanceTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private com.th4.edge.decoboon.labor.service.worker.info.Info workerInfo;
    @Autowired private com.th4.edge.decoboon.labor.service.worker.update.Update workerUpdate;

    @Test
    void shouldReturnWorkerForOwnerAndMaskSensitiveFieldsForDesigner() {
        Employee boss = employee("负责人", EmployeeRole.Boss, null);
        Worker worker = worker(boss.getCompany());

        com.th4.edge.decoboon.labor.service.worker.info.support.Info target = target();
        ReflectionTestUtils.setField(target, "employee", boss);
        ReflectionTestUtils.setField(target, "workerId", worker.getWorkerId());
        Worker loaded = target.call(new Worker());
        assertThat(loaded.getName()).isEqualTo("张伟");
        assertThat(loaded.getIdCardNo()).isEqualTo("341222198803305970");

        Employee designer = employee("设计师", EmployeeRole.Designer, boss);
        ReflectionTestUtils.setField(target, "employee", designer);
        Worker masked = target.call(new Worker());
        assertThat(masked.getIdCardNo()).isEqualTo("341222********5970");
        assertThat(masked.getBankAccount()).isEqualTo("***********1266");
    }

    @Test
    void shouldKeepSensitiveFieldsWhenUpdateOmitsThem() {
        Employee pm = employee("项目经理", EmployeeRole.Pm, null);
        Worker worker = worker(pm.getCompany());
        com.th4.edge.decoboon.labor.service.worker.update.support.Update target = updateTarget();
        ReflectionTestUtils.setField(target, "employee", pm);
        ReflectionTestUtils.setField(target, "workerId", worker.getWorkerId());
        ReflectionTestUtils.setField(target, "name", "张伟（改）");

        Worker saved = target.call(new Worker());

        assertThat(saved.getName()).isEqualTo("张伟（改）");
        assertThat(saved.getIdCardNo()).isEqualTo("341222198803305970");
        assertThat(saved.getBankAccount()).isEqualTo("662245678901266");
    }

    @Test
    void shouldClearSensitiveFieldsWhenUpdateSendsBlank() {
        Employee pm = employee("项目经理", EmployeeRole.Pm, null);
        Worker worker = worker(pm.getCompany());
        com.th4.edge.decoboon.labor.service.worker.update.support.Update target = updateTarget();
        ReflectionTestUtils.setField(target, "employee", pm);
        ReflectionTestUtils.setField(target, "workerId", worker.getWorkerId());
        ReflectionTestUtils.setField(target, "idCardNo", "");
        ReflectionTestUtils.setField(target, "bankAccount", "");

        Worker saved = target.call(new Worker());

        assertThat(saved.getIdCardNo()).isNull();
        assertThat(saved.getBankAccount()).isNull();
    }

    @Test
    void shouldRejectCrossCompanyWorkerAccess() {
        Employee boss = employee("负责人", EmployeeRole.Boss, null);
        Employee stranger = employee("外部负责人", EmployeeRole.Boss, null);
        worker(stranger.getCompany());

        Worker other = workerRepository.findByCompany_ComIdOrderByWorkerIdDesc(stranger.getCompany().getComId()).get(0);
        com.th4.edge.decoboon.labor.service.worker.info.support.Info target = target();
        ReflectionTestUtils.setField(target, "employee", boss);
        ReflectionTestUtils.setField(target, "workerId", other.getWorkerId());

        assertThatThrownBy(() -> target.call(new Worker()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("工人档案不存在或无权访问");
    }

    private com.th4.edge.decoboon.labor.service.worker.info.support.Info target() {
        return AopTestUtils.getTargetObject(workerInfo);
    }

    private com.th4.edge.decoboon.labor.service.worker.update.support.Update updateTarget() {
        return AopTestUtils.getTargetObject(workerUpdate);
    }

    /** 传入已有员工表示同企业同门店（用于角色矩阵）；传空则新建企业。 */
    private Employee employee(String name, EmployeeRole role, Employee template) {
        long seq = System.nanoTime();
        Company company;
        Store store;
        if (template == null) {
            company = new Company();
            company.setTitle("工人详情测试企业" + seq + name);
            company.setName("工人详情测试企业");
            company.setSize(Size.A);
            company = companyRepository.save(company);
            store = new Store();
            store.setCompany(company);
            store.setName("测试门店");
            store.setDefaultStore(true);
            store = storeRepository.save(store);
        } else {
            company = template.getCompany();
            store = template.getStore();
        }

        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setName(name);
        employee.setTelNo("18" + String.format("%09d", Math.abs(seq % 1_000_000_000L)));
        employee.setJoinedAt(LocalDateTime.now());
        return employeeRepository.save(employee);
    }

    private Worker worker(Company company) {
        Worker worker = new Worker();
        worker.setCompany(company);
        worker.setName("张伟");
        worker.setIdCardNo("341222198803305970");
        worker.setTrade("木工");
        worker.setPhone("18660219910");
        worker.setBankAccount("662245678901266");
        worker.setStatus(WorkerStatus.Active);
        return workerRepository.save(worker);
    }
}
