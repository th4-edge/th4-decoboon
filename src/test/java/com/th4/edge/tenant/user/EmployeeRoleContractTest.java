package com.th4.edge.tenant.user;

import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeRolePolicy;
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
    void financeDataScopeIsStoreAfterDec065() {
        // DEC-065 FIN-STORE-01：财务数据范围由「企业」收窄为「归属门店」；
        // 归属默认总店的财务由 DataScopeContext 升为总部财务（企业范围，FIN-STORE-03）。
        assertThat(EmployeeRole.Finance.getDataScopeType()).isEqualTo(DataScopeType.Store);
        assertThat(EmployeeRole.StoreManager.getDataScopeType()).isEqualTo(DataScopeType.Store);
        assertThat(EmployeeRole.Boss.getDataScopeType()).isEqualTo(DataScopeType.Company);
    }

    @Test
    void teamLeaderIsPersistedButNotAnApplicationRole() {
        assertThat(EmployeeRolePolicy.isApplicationRole(EmployeeRole.TeamLeader)).isFalse();
        assertThat(EmployeeRolePolicy.isApplicationRole(EmployeeRole.Boss)).isTrue();
        assertThat(EmployeeRolePolicy.isApplicationRole(EmployeeRole.StoreManager)).isTrue();
        assertThat(EmployeeRolePolicy.isApplicationRole(EmployeeRole.Marketer)).isTrue();
        assertThat(EmployeeRolePolicy.isApplicationRole(EmployeeRole.Designer)).isTrue();
        assertThat(EmployeeRolePolicy.isApplicationRole(EmployeeRole.Pm)).isTrue();
        assertThat(EmployeeRolePolicy.isApplicationRole(EmployeeRole.Finance)).isTrue();
    }
}
