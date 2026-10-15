package com.th4.edge.decoboon.material.service.template.update.support;

import com.th4.edge.decoboon.material.context.MaterialSheetTemplateContext;
import com.th4.edge.decoboon.material.model.MaterialCategory;
import com.th4.edge.decoboon.material.model.MaterialSheetTemplate;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;

/** material/template/update 实现。 */
@Note("主材下单模板更新")
@Properties(allow = {
        @AllowProperties(model = MaterialSheetTemplate.class,
                properties = {"sheetTemplateId", "name", "sheetType", "scope", "baselineVersion", "version",
                        "publishedVersion", "fieldConfig", "rowTemplateConfig", "ruleConfig", "signatureConfig",
                        "numberRule", "status", "printTemplateId", "createTime", "updateTime"}),
        @AllowProperties(model = MaterialCategory.class, properties = {"materialCategoryId", "name", "defaultLossRate"}),
        @AllowProperties(model = Company.class, properties = {"comId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("material/template/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.material.service.template.update.Update {

    @User private Employee employee;
    @AutoInject @NotNull private Long templateId;
    @AutoInject private String name;
    @AutoInject private String fieldConfig;
    @AutoInject private String rowTemplateConfig;
    @AutoInject private String ruleConfig;
    @AutoInject private String signatureConfig;
    @AutoInject private String numberRule;

    private final MaterialSheetTemplateContext context;

    public Update(MaterialSheetTemplateContext context) {
        this.context = context;
    }

    @Override
    @Transactional
    public MaterialSheetTemplate call(MaterialSheetTemplate request) {
        return context.update(employee, templateId, name, fieldConfig, rowTemplateConfig, ruleConfig,
                signatureConfig, numberRule);
    }
}
