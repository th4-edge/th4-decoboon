package com.th4.edge.decoboon.blueprint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.blueprint.context.BlueprintRuntimeContext;
import com.th4.edge.decoboon.blueprint.model.BlueprintRuntime;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaterialBlueprintRuntimeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void runtimeResourceMustMatchDomainBlueprint() throws Exception {
        JsonNode published = objectMapper.readTree(Files.readString(Path.of("docs/product/domains/material/03-blueprint.json")));
        JsonNode runtime = objectMapper.readTree(Files.readString(Path.of("src/main/resources/blueprints/material-v1.json")));
        assertThat(runtime).isEqualTo(published);
    }

    @Test
    void bossCanLoadPublishedMaterialBlueprint() throws Exception {
        TenantContext tenantContext = mock(TenantContext.class);
        Employee boss = new Employee();
        boss.setRole(EmployeeRole.Boss);
        when(tenantContext.requireEmployee(boss)).thenReturn(boss);

        BlueprintRuntime runtime = new BlueprintRuntimeContext(tenantContext, objectMapper)
                .load(boss, "material.end-to-end");

        assertThat(runtime.getId()).isEqualTo("material.end-to-end");
        assertThat(runtime.getVersion()).isEqualTo("1.0.0");
        assertThat(runtime.getStatus()).isEqualTo("ready-for-implementation");
        assertThat(runtime.getSourceRefCount()).isGreaterThan(0);
        assertThat(runtime.getViewerRole()).isEqualTo("Boss");
        assertThat(runtime.isCostVisible()).isTrue();
        JsonNode json = objectMapper.readTree(runtime.getBlueprintJson());
        assertThat(json.path("stages")).hasSize(9);
        assertThat(json.path("objects")).hasSize(12);
    }

    @Test
    void designerCannotOpenFinanceBlueprintCenter() {
        TenantContext tenantContext = mock(TenantContext.class);
        Employee designer = new Employee();
        designer.setRole(EmployeeRole.Designer);
        when(tenantContext.requireEmployee(designer)).thenReturn(designer);

        assertThatThrownBy(() -> new BlueprintRuntimeContext(tenantContext, objectMapper)
                .load(designer, "material.end-to-end"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权");
    }
}
