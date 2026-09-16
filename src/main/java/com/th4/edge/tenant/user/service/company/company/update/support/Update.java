package com.th4.edge.tenant.user.service.company.company.update.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Note("企业信息更新")
@Properties(allow = @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "size", "addr", "setting", "createTime"}))
@Service("company/company/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.tenant.user.service.company.company.update.Update {
    @User private Employee employee;
    @AutoInject @Length(max = 128) private String title;
    @AutoInject @Length(max = 12) private String name;
    @AutoInject @Length(max = 256) private String logo;
    @AutoInject @Length(max = 12) private String color;
    @AutoInject @Length(max = 256) private String addr;
    private final TenantContext tenantContext;
    private final CompanyRepository companyRepository;
    public Update(TenantContext tenantContext, CompanyRepository companyRepository) { this.tenantContext = tenantContext; this.companyRepository = companyRepository; }
    @Override @Transactional
    public Company call(Employee request) {
        Employee boss = tenantContext.requireBoss(employee);
        Company company = boss.getCompany();
        if (title != null && !title.isBlank()) company.setTitle(title.trim());
        if (name != null && !name.isBlank()) company.setName(name.trim());
        if (logo != null) company.setLogo(logo);
        if (color != null) company.setColor(color);
        if (addr != null) company.setAddr(addr);
        return companyRepository.save(company);
    }
}
