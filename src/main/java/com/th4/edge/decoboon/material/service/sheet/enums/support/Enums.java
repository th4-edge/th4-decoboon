package com.th4.edge.decoboon.material.service.sheet.enums.support;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.material.configure.MaterialSheetStatus;
import com.th4.edge.decoboon.material.context.MaterialSheetBaseline;
import com.th4.edge.decoboon.material.context.MaterialSheetTemplateContext;
import com.th4.edge.decoboon.material.model.MaterialSheetEnums;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;

/**
 * material/sheet/enums 实现。
 *
 * <p>品类来自平台行业基线（只读），避免前端硬编码；任何登录员工可读（建单需要）。</p>
 */
@Note("主材下单枚举")
@Properties(allow = {
        @AllowProperties(model = MaterialSheetEnums.class, properties = {"sheetTypes", "statuses", "units"}),
        @AllowProperties(model = MaterialSheetEnums.SheetType.class,
                properties = {"code", "name", "headerFields", "lineFields"}),
        @AllowProperties(model = MaterialSheetEnums.FieldConfig.class,
                properties = {"key", "label", "visible", "required", "order"}),
        @AllowProperties(model = MaterialSheetEnums.Option.class, properties = {"value", "name"})
})
@Service("material/sheet/enums")
@Scope(ScopeTypes.PROTOTYPE)
public class Enums implements com.th4.edge.decoboon.material.service.sheet.enums.Enums {

    @User private Employee employee;

    private final MaterialSheetBaseline baseline;
    private final MaterialSheetTemplateContext templateContext;
    private final TenantContext tenantContext;

    public Enums(MaterialSheetBaseline baseline, MaterialSheetTemplateContext templateContext,
                 TenantContext tenantContext) {
        this.baseline = baseline;
        this.templateContext = templateContext;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialSheetEnums call(Employee request) {
        MaterialSheetEnums result = new MaterialSheetEnums();
        Employee current = tenantContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        result.setSheetTypes(baseline.presets().stream()
                .map(preset -> {
                    return new MaterialSheetEnums.SheetType(
                            preset.code(),
                            preset.name(),
                            toFieldConfigs(templateContext.fieldsForIssue(comId, preset.code(), "header")),
                            toFieldConfigs(templateContext.fieldsForIssue(comId, preset.code(), "line"))
                    );
                })
                .toList());
        result.setStatuses(Arrays.stream(MaterialSheetStatus.values())
                .map(status -> new MaterialSheetEnums.Option(status.name(), status.getName()))
                .toList());
        result.setUnits(Arrays.stream(Unit.values())
                .map(unit -> new MaterialSheetEnums.Option(unit.name(), unit.getName()))
                .toList());
        return result;
    }

    private List<MaterialSheetEnums.FieldConfig> toFieldConfigs(
            List<MaterialSheetTemplateContext.FieldRequirement> fields) {
        List<MaterialSheetEnums.FieldConfig> result = new ArrayList<>(fields.size());
        for (MaterialSheetTemplateContext.FieldRequirement field : fields) {
            result.add(new MaterialSheetEnums.FieldConfig(
                    field.key(), field.label(), field.visible(), field.required(), field.order()));
        }
        return result;
    }
}
