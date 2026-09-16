package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.statistic.model.EmployeePerformance;
import com.th4.edge.decoboon.statistic.service.performance.report.support.Report;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** 人员绩效响应白名单回归：员工摘要不得带出门店/团队循环归属图。 */
class PerformanceReportSerializationContractTest {

    @Test
    void performanceEmployeeSummaryMustNotContainStoreOrTeam() {
        Properties properties = Report.class.getAnnotation(Properties.class);
        assertThat(properties).isNotNull();
        AllowProperties employeeAllow = Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(Employee.class))
                .findFirst()
                .orElseThrow();

        assertThat(Arrays.asList(employeeAllow.properties())).contains("empId", "name", "role");
        assertThat(Arrays.asList(employeeAllow.properties())).doesNotContain("store", "team");
        assertThat(Arrays.stream(properties.allow()).map(AllowProperties::model))
                .contains(EmployeePerformance.class);
    }
}
