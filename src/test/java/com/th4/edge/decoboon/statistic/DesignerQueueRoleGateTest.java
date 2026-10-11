package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.statistic.context.DesignerMeasureTaskContext;
import com.th4.edge.decoboon.statistic.model.DesignerMeasureQuery;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DEC-032 / BR-028（ANS-BIZ-20261011-002）：设计师岗位队列必须按 BOSS 内部能力超集放行。 */
class DesignerQueueRoleGateTest {

    @Test
    void measureQueueUsesInternalRoleGateForBoss() {
        TenantContext tenantContext = mock(TenantContext.class);
        DesignerMeasureTaskContext taskContext = mock(DesignerMeasureTaskContext.class);
        Company company = new Company();
        company.setComId(11L);
        Employee boss = new Employee();
        boss.setEmpId(33L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        DesignerMeasureQuery request = new DesignerMeasureQuery();

        when(tenantContext.requireInternalRole(boss, EmployeeRole.Designer)).thenReturn(boss);
        when(taskContext.list(boss, request)).thenReturn(List.of());

        var provider = new com.th4.edge.decoboon.statistic.service.designer.measure.list.support.List(
                tenantContext, taskContext);
        ReflectionTestUtils.setField(provider, "employee", boss);

        provider.call(request);

        verify(tenantContext).requireInternalRole(boss, EmployeeRole.Designer);
    }

    @Test
    void designerQueuesMustNotUseExactRoleGate() {
        for (String path : List.of(
                "src/main/java/com/th4/edge/decoboon/statistic/service/designer/measure/list/support/List.java",
                "src/main/java/com/th4/edge/decoboon/statistic/service/workbench/designer/queue/list/support/List.java",
                "src/main/java/com/th4/edge/decoboon/statistic/service/workbench/designer/report/support/Report.java")) {
            String source = read(path);
            assertThat(source).as(path).contains("requireInternalRole");
            assertThat(source).as(path).doesNotContain("requireRole(employee, EmployeeRole.Designer)");
        }
    }

    private String read(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
