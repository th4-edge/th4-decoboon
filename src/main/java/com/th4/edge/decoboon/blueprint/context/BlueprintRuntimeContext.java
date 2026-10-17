package com.th4.edge.decoboon.blueprint.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.blueprint.model.BlueprintRuntime;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * 业务蓝图运行态读取器。
 *
 * <p>运行时资源是领域包蓝图的发布副本；测试校验两份 JSON 同源。Provider 只返回已发布、已核对的内容，
 * 不在运行时推测业务规则。</p>
 */
@Component
public class BlueprintRuntimeContext {

    private static final String MATERIAL_BLUEPRINT_ID = "material.end-to-end";
    private static final String MATERIAL_BLUEPRINT_RESOURCE = "blueprints/material-v1.json";
    private static final Set<EmployeeRole> READER_ROLES =
            Set.of(EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Finance);

    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;
    private volatile String cachedJson;

    public BlueprintRuntimeContext(TenantContext tenantContext, ObjectMapper objectMapper) {
        this.tenantContext = tenantContext;
        this.objectMapper = objectMapper;
    }

    public BlueprintRuntime load(Employee employee, String blueprintId) {
        Employee current = tenantContext.requireEmployee(employee);
        if (!READER_ROLES.contains(current.getRole())) {
            throw new DefinedException(new DefaultErrorCode("403", "当前角色无权查看材料业务蓝图"));
        }
        String id = blueprintId == null || blueprintId.isBlank() ? MATERIAL_BLUEPRINT_ID : blueprintId.trim();
        if (!MATERIAL_BLUEPRINT_ID.equals(id)) {
            throw new DefinedException(new DefaultErrorCode("404", "业务蓝图不存在"));
        }
        try {
            JsonNode root = objectMapper.readTree(resourceJson());
            BlueprintRuntime runtime = new BlueprintRuntime();
            runtime.setId(text(root, "id"));
            runtime.setTitle(text(root, "title"));
            runtime.setVersion(text(root, "version"));
            runtime.setStatus(text(root, "status"));
            runtime.setLastReviewedCommit(text(root, "lastReviewedCommit"));
            runtime.setSourceRefCount(root.path("sourceRefs").isArray() ? root.path("sourceRefs").size() : 0);
            runtime.setViewerRole(current.getRole().name());
            runtime.setCostVisible(READER_ROLES.contains(current.getRole()));
            runtime.setBlueprintJson(objectMapper.writeValueAsString(root));
            return runtime;
        } catch (IOException error) {
            throw new DefinedException(new DefaultErrorCode("500", "业务蓝图暂不可确认"));
        }
    }

    private String resourceJson() throws IOException {
        String json = cachedJson;
        if (json != null) {
            return json;
        }
        ClassPathResource resource = new ClassPathResource(MATERIAL_BLUEPRINT_RESOURCE);
        try (InputStream input = resource.getInputStream()) {
            json = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        cachedJson = json;
        return json;
    }

    private String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
