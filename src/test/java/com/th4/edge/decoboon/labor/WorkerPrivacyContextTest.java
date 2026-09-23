package com.th4.edge.decoboon.labor;

import com.th4.edge.decoboon.labor.context.WorkerPrivacyContext;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 工人档案敏感字段裁剪：完整身份证号与银行账号仅企业负责人、财务与维护人可见。 */
class WorkerPrivacyContextTest {

    private static final String ID_CARD = "341222198803305970";
    private static final String BANK = "662245678901266";

    private final WorkerPrivacyContext context = new WorkerPrivacyContext(null);

    @Test
    void shouldReturnFullSensitiveValuesForBossFinanceAndMaintainer() {
        for (EmployeeRole role : new EmployeeRole[]{EmployeeRole.Boss, EmployeeRole.Finance, EmployeeRole.Pm}) {
            Worker worker = worker();
            context.mask(worker, employee(role));
            assertThat(worker.getIdCardNo()).as(role.name()).isEqualTo(ID_CARD);
            assertThat(worker.getBankAccount()).as(role.name()).isEqualTo(BANK);
        }
    }

    @Test
    void shouldMaskSensitiveValuesForStoreManagerAndDesigner() {
        for (EmployeeRole role : new EmployeeRole[]{EmployeeRole.StoreManager, EmployeeRole.Designer}) {
            Worker worker = worker();
            context.mask(worker, employee(role));
            assertThat(worker.getIdCardNo()).as(role.name()).isEqualTo("341222********5970");
            assertThat(worker.getBankAccount()).as(role.name()).isEqualTo("***********1266");
            assertThat(worker.getName()).isEqualTo("张伟");
        }
    }

    @Test
    void shouldKeepNullValuesUntouched() {
        Worker worker = worker();
        worker.setIdCardNo(null);
        worker.setBankAccount(null);
        context.mask(worker, employee(EmployeeRole.Designer));
        assertThat(worker.getIdCardNo()).isNull();
        assertThat(worker.getBankAccount()).isNull();
    }

    private Worker worker() {
        Worker worker = new Worker();
        worker.setName("张伟");
        worker.setIdCardNo(ID_CARD);
        worker.setBankAccount(BANK);
        return worker;
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setRole(role);
        return employee;
    }
}
