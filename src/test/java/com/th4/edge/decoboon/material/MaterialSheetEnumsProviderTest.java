package com.th4.edge.decoboon.material;

import com.th4.edge.decoboon.material.context.MaterialSheetBaseline;
import com.th4.edge.decoboon.material.context.MaterialSheetTemplateContext;
import com.th4.edge.decoboon.material.model.MaterialSheetEnums;
import com.th4.edge.decoboon.material.service.sheet.enums.support.Enums;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 枚举 Provider 必须通过 TenantContext 加载公司，并把模板字段配置真正投影给 App。 */
class MaterialSheetEnumsProviderTest {

    @Test
    void tileEnumsShouldCarryTemplateFieldConfig() {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(2L);
        employee.setCompany(company);

        TenantContext tenantContext = mock(TenantContext.class);
        MaterialSheetTemplateContext templateContext = mock(MaterialSheetTemplateContext.class);
        when(tenantContext.requireEmployee(employee)).thenReturn(employee);
        when(templateContext.fieldsForIssue(1L, "Tile", "header")).thenReturn(List.of(
                new MaterialSheetTemplateContext.FieldRequirement("measureAt", "测量时间", true, true, 2)));
        when(templateContext.fieldsForIssue(1L, "Tile", "line")).thenReturn(List.of(
                new MaterialSheetTemplateContext.FieldRequirement("locationName", "安装位置", true, true, 1)));

        Enums provider = new Enums(new MaterialSheetBaseline(), templateContext, tenantContext);
        ReflectionTestUtils.setField(provider, "employee", employee);
        MaterialSheetEnums result = provider.call(employee);

        assertThat(result.getSheetTypes()).hasSize(1);
        MaterialSheetEnums.SheetType tile = result.getSheetTypes().get(0);
        assertThat(tile.code()).isEqualTo("Tile");
        assertThat(tile.headerFields()).extracting(MaterialSheetEnums.FieldConfig::key).contains("measureAt");
        assertThat(tile.headerFields().get(0).required()).isTrue();
        assertThat(tile.lineFields()).extracting(MaterialSheetEnums.FieldConfig::key).contains("locationName");
    }
}
