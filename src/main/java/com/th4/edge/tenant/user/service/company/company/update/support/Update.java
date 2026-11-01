package com.th4.edge.tenant.user.service.company.company.update.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.io.annotation.AutoFile;
import com.th4.edge.framework.io.StreamFile;
import com.th4.edge.tenant.user.context.CompanyBrandContext;
import com.th4.edge.tenant.user.model.Setting;
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
@Properties(allow = {
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "size", "addr", "setting", "createTime"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Setting.class, properties = {"enableStore", "gradeConfig", "brandPrimary", "custodianCredit", "serviceArea", "serviceTel", "afterSaleTel", "uscc", "qualification", "intro"})
})
@Service("company/company/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.tenant.user.service.company.company.update.Update {
    @User private Employee employee;
    @AutoInject @Length(max = 128) private String title;
    @AutoInject @Length(max = 12) private String name;
    @AutoInject @Length(max = 256) private String logo;
    /** Logo 文件只能通过该字段上传；服务端负责保存并回填长期地址。 */
    @AutoFile private StreamFile logoFile;
    @AutoInject @Length(max = 12) private String color;
    @AutoInject @Length(max = 256) private String addr;
    /** 品牌与署名配置（字段级合并，不整体覆盖既有 enableStore / gradeConfig）。 */
    @AutoInject private Setting setting;
    private final TenantContext tenantContext;
    private final CompanyRepository companyRepository;
    private final CompanyBrandContext companyBrandContext;
    public Update(TenantContext tenantContext, CompanyRepository companyRepository,
                  CompanyBrandContext companyBrandContext) {
        this.tenantContext = tenantContext;
        this.companyRepository = companyRepository;
        this.companyBrandContext = companyBrandContext;
    }
    @Override @Transactional
    public Company call(Employee request) {
        Employee boss = tenantContext.requireBoss(employee);
        Company company = boss.getCompany();
        if (title != null && !title.isBlank()) company.setTitle(title.trim());
        if (name != null && !name.isBlank()) company.setName(name.trim());
        if (logoFile != null && !logoFile.isEmpty()) {
            CompanyBrandContext.StoredLogo stored = companyBrandContext.save(company.getComId(), logoFile);
            company.setLogo(stored.url());
        } else if (logo != null) {
            if (!logo.isBlank()) {
                throw new com.th4.edge.framework.exception.DefinedException(
                        new com.th4.edge.framework.exception.DefaultErrorCode("400", "企业Logo请通过文件上传"));
            }
            company.setLogo(null);
        }
        if (color != null) company.setColor(color);
        if (addr != null) company.setAddr(addr);
        if (setting != null) {
            Setting target = company.getSetting() == null ? new Setting() : company.getSetting();
            if (setting.getBrandPrimary() != null) {
                String brandPrimary = setting.getBrandPrimary().trim();
                if (!"Company".equals(brandPrimary) && !"Platform".equals(brandPrimary)) {
                    throw new com.th4.edge.framework.exception.DefinedException(
                            new com.th4.edge.framework.exception.DefaultErrorCode("400", "brandPrimary 只允许 Company 或 Platform"));
                }
                target.setBrandPrimary(brandPrimary);
            }
            if (setting.getCustodianCredit() != null) target.setCustodianCredit(setting.getCustodianCredit());
            if (setting.getServiceArea() != null) target.setServiceArea(setting.getServiceArea());
            if (setting.getServiceTel() != null) target.setServiceTel(setting.getServiceTel());
            if (setting.getAfterSaleTel() != null) target.setAfterSaleTel(setting.getAfterSaleTel());
            if (setting.getUscc() != null) target.setUscc(setting.getUscc());
            if (setting.getQualification() != null) target.setQualification(setting.getQualification());
            if (setting.getIntro() != null) target.setIntro(setting.getIntro());
            company.setSetting(target);
        }
        return companyRepository.save(company);
    }
}
