package com.th4.edge.tenant.user.service.auth.session.register.support;
import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Setting;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
@Note("微信注册企业")
@Properties(allow = {
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "createTime"}),
        @AllowProperties(model = Credentials.class, properties = {"creId", "openId", "unionId", "phone", "status", "lastTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "size", "addr", "setting"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore", "color", "addr", "company", "createTime"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color", "store", "company", "createTime"})
})
@Service("auth/session/register")
@Scope(ScopeTypes.PROTOTYPE)
public class Register implements com.th4.edge.tenant.user.service.auth.session.register.Register {
    @com.th4.edge.framework.provider.security.annotation.User
    private com.th4.edge.framework.provider.security.User<?> principal;
    @AutoInject @NotEmpty @Length(max = 15) private String bossName;
    @AutoInject @NotEmpty @Length(max = 128) private String companyTitle;
    @AutoInject @NotEmpty @Length(max = 12) private String companyName;
    @AutoInject private Size size;
    @AutoInject @Length(max = 256) private String companyAddr;
    @AutoInject @Length(max = 16) private String storeName;
    @AutoInject @Length(max = 256) private String storeAddr;
    @AutoInject @Length(max = 512) private String avatarUrl;
    private final UserIdentityContext userContext;
    private final MembershipContext membershipContext;
    private final CompanyRepository companyRepository;
    private final StoreRepository storeRepository;
    private final EmployeeRepository employeeRepository;
    private final SubscriptionContext subscriptionContext;
    private final ModelIdentityContext modelIdentityContext;
    public Register(UserIdentityContext userContext, MembershipContext membershipContext,
                    CompanyRepository companyRepository, StoreRepository storeRepository,
                    EmployeeRepository employeeRepository, SubscriptionContext subscriptionContext,
                    ModelIdentityContext modelIdentityContext) {
        this.userContext = userContext; this.membershipContext = membershipContext;
        this.companyRepository = companyRepository; this.storeRepository = storeRepository;
        this.employeeRepository = employeeRepository; this.subscriptionContext = subscriptionContext;
        this.modelIdentityContext = modelIdentityContext;
    }
    @Override @Transactional
    public com.th4.edge.framework.provider.security.Token call(Credentials request) {
        Credentials credential;
        if (principal instanceof Credentials current) {
            credential = userContext.requireUser(current.getCreId());
        } else if (principal instanceof Employee employee) {
            credential = userContext.credentialFor(employee);
        } else {
            credential = null;
        }
        if (credential == null || credential.getPhone() == null) {
            throw new DefinedException(new DefaultErrorCode("401", "微信身份未完成手机号授权"));
        }
        if (userContext.activeEmployee(credential) != null) {
            throw new DefinedException(new DefaultErrorCode("400", "当前用户已有在职企业"));
        }
        membershipContext.validateJoin(credential);

        Company company = new Company();
        company.setTitle(companyTitle);
        company.setName(companyName);
        company.setSize(size == null ? Size.A : size);
        company.setAddr(companyAddr);
        Setting setting = new Setting();
        setting.setEnableStore(false);
        company.setSetting(setting);
        company = companyRepository.save(modelIdentityContext.prepare(company));

        Store store = new Store();
        store.setCompany(company);
        store.setName(storeName == null || storeName.isBlank() ? "总店" : storeName);
        store.setAddr(storeAddr == null ? companyAddr : storeAddr);
        store.setDefaultStore(true);
        store = storeRepository.save(modelIdentityContext.prepare(store));

        Employee boss = new Employee();
        boss.setName(bossName);
        boss.setTelNo(credential.getPhone());
        boss.setAvatarUrl(avatarUrl);
        boss.setStore(store);
        boss.setCompany(company);
        boss.setRole(EmployeeRole.Boss);
        boss.setStatus(EmployeeStatus.Active);
        boss.setJoinedAt(LocalDateTime.now());
        boss = employeeRepository.save(modelIdentityContext.prepare(boss));
        userContext.linkEmployee(credential, boss);
        membershipContext.recordJoin(credential, credential.getPhone());
        subscriptionContext.grantTrial(company);
        return boss;
    }
}
