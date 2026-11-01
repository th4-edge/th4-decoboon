package com.th4.edge.tenant.user.service.company.company.info.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
@Note("企业信息")
@Properties(allow = {
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "size", "addr", "setting", "createTime"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Setting.class, properties = {"enableStore", "gradeConfig", "brandPrimary", "custodianCredit", "serviceArea", "serviceTel", "afterSaleTel", "uscc", "qualification", "intro"})
})
@Service("company/company/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.tenant.user.service.company.company.info.Info {
    @User private Employee employee;
    private final TenantContext tenantContext;
    public Info(TenantContext tenantContext) { this.tenantContext = tenantContext; }
    @Override public Company call(Employee request) { return tenantContext.requireEmployee(employee).getCompany(); }
}
