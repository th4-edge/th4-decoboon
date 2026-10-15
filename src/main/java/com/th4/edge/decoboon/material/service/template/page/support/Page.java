package com.th4.edge.decoboon.material.service.template.page.support;

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
import com.th4.edge.framework.provider.security.annotation.User;

/** material/template/page 实现。 */
@Note("主材下单模板列表")
@Properties(allow = {
        @AllowProperties(model = MaterialSheetTemplate.class,
                properties = {"sheetTemplateId", "name", "sheetType", "scope", "baselineVersion", "version",
                        "publishedVersion", "fieldConfig", "rowTemplateConfig", "ruleConfig", "signatureConfig",
                        "numberRule", "status", "printTemplateId", "createTime", "updateTime"}),
        @AllowProperties(model = MaterialCategory.class, properties = {"materialCategoryId", "name", "defaultLossRate"}),
        @AllowProperties(model = Company.class, properties = {"comId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("material/template/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.material.service.template.page.Page {

    @User private Employee employee;

    private final MaterialSheetTemplateContext context;

    public Page(MaterialSheetTemplateContext context) {
        this.context = context;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<MaterialSheetTemplate> call(Employee request) {
        return context.page(employee);
    }
}
