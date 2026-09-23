package com.th4.edge.tenant.user;

import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** 产品员工角色固定为七类，禁止重新引入履约专员等重复岗位。 */
class EmployeeRoleContractTest {

    @Test
    void employeeRolesAreCanonicalSeven() {
        assertThat(EmployeeRole.values())
                .extracting(Enum::name)
                .containsExactly("Boss", "StoreManager", "Designer", "Marketer", "Pm", "Finance", "TeamLeader");
        assertThat(EmployeeRole.Boss.getName()).isEqualTo("企业负责人");
        assertThat(EmployeeRole.StoreManager.getName()).isEqualTo("店长");
        assertThat(EmployeeRole.Marketer.getName()).isEqualTo("业务员");
        assertThat(Arrays.stream(EmployeeRole.values()).map(Enum::name))
                .doesNotContain("ProjectSupport");
    }

    @Test
    void financeKeepsCompanyDataScopeAfterRoleMerge() {
        assertThat(EmployeeRole.Finance.getDataScopeType()).isEqualTo(DataScopeType.Company);
    }
}
