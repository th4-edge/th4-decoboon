package com.th4.edge.decoboon;

import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** 员工选择器响应必须保持最小投影，避免嵌套企业时间对象导致 JSON 截断。 */
class EmployeePageSerializationContractTest {

    @Test
    void employeePageMustUseFieldsRequiredByRoleAndStorePickers() {
        Properties properties = com.th4.edge.tenant.user.service.user.employee.page.support.Page.class
                .getAnnotation(Properties.class);
        assertThat(properties).isNotNull();

        assertThat(fieldsOf(properties, Employee.class))
                .contains("empId", "name", "role", "store", "team", "status")
                .doesNotContain("createTime");
        assertThat(fieldsOf(properties, Company.class))
                .contains("comId", "title", "name", "color")
                .doesNotContain("setting", "createTime");
        assertThat(fieldsOf(properties, Store.class))
                .contains("storeId", "name", "defaultStore", "company")
                .doesNotContain("createTime");
        assertThat(fieldsOf(properties, Team.class))
                .contains("teamId", "name", "color", "store", "company")
                .doesNotContain("createTime");
    }

    private static java.util.List<String> fieldsOf(Properties properties, Class<?> model) {
        return Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(model))
                .findFirst()
                .map(item -> Arrays.asList(item.properties()))
                .orElseGet(java.util.List::of);
    }
}
