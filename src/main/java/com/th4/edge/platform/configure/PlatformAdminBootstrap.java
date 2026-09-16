package com.th4.edge.platform.configure;

import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.configure.UserStatus;
import com.th4.edge.tenant.user.context.PasswordContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 显式启用的平台运营账号初始化器。 */
@Note("平台运营账号初始化")
@Description("仅在提供完整环境变量时创建平台运营企业、默认门店、Boss 员工和 BCrypt 密码凭证")
@Component
public class PlatformAdminBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformAdminBootstrap.class);

    @Value("${decoboon.platform.bootstrap.enabled:false}")
    private boolean enabled;
    @Value("${decoboon.platform.bootstrap.company-title:装得清平台运营中心}")
    private String companyTitle;
    @Value("${decoboon.platform.bootstrap.company-name:平台运营}")
    private String companyName;
    @Value("${decoboon.platform.bootstrap.phone:}")
    private String phone;
    @Value("${decoboon.platform.bootstrap.password:}")
    private String password;

    private final CompanyRepository companyRepository;
    private final StoreRepository storeRepository;
    private final EmployeeRepository employeeRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordContext passwordContext;

    public PlatformAdminBootstrap(CompanyRepository companyRepository,
                                  StoreRepository storeRepository,
                                  EmployeeRepository employeeRepository,
                                  CredentialsRepository credentialsRepository,
                                  PasswordContext passwordContext) {
        this.companyRepository = companyRepository;
        this.storeRepository = storeRepository;
        this.employeeRepository = employeeRepository;
        this.credentialsRepository = credentialsRepository;
        this.passwordContext = passwordContext;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (phone == null || phone.isBlank() || password == null || password.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("PLATFORM_BOOTSTRAP_INVALID",
                    "平台运营初始化必须提供手机号和密码"));
        }
        if (companyTitle == null || companyTitle.isBlank() || companyTitle.length() > 128) {
            throw new DefinedException(new DefaultErrorCode("PLATFORM_BOOTSTRAP_INVALID",
                    "平台运营企业名称不合法"));
        }
        passwordContext.validate(password);

        Company company = companyRepository.findFirstByTitle(companyTitle.trim());
        if (company == null) {
            company = new Company();
            company.setTitle(companyTitle.trim());
            company.setName(companyName == null || companyName.isBlank() ? "平台运营" : companyName.trim());
            company.setSize(Size.A);
            company.setPlatform(true);
            company = companyRepository.save(company);
        } else {
            if (!Boolean.TRUE.equals(company.getPlatform())) {
                company.setPlatform(true);
                company = companyRepository.save(company);
            }
        }

        Store store = storeRepository.findByCompany_ComIdAndDefaultStoreTrue(company.getComId());
        if (store == null) {
            store = new Store();
            store.setCompany(company);
            store.setName("平台运营总店");
            store.setDefaultStore(true);
            store = storeRepository.save(store);
        }

        Employee employee = employeeRepository.findFirstByTelNoOrderByEmpIdDesc(phone.trim());
        if (employee != null && (employee.getCompany() == null
                || !company.getComId().equals(employee.getCompany().getComId()))) {
            throw new DefinedException(new DefaultErrorCode("PLATFORM_BOOTSTRAP_CONFLICT",
                    "该手机号已属于其他企业，禁止自动提升为平台运营账号"));
        }
        if (employee == null) {
            employee = new Employee();
            employee.setCompany(company);
            employee.setStore(store);
            employee.setRole(EmployeeRole.Boss);
            employee.setName("平台运营负责人");
            employee.setTelNo(phone.trim());
            employee.setStatus(EmployeeStatus.Active);
            employee.setJoinedAt(LocalDateTime.now());
            employee = employeeRepository.save(employee);
        } else {
            if (employee.getRole() != EmployeeRole.Boss) {
                throw new DefinedException(new DefaultErrorCode("PLATFORM_BOOTSTRAP_CONFLICT",
                        "已有账号不是平台运营负责人，禁止自动覆盖角色"));
            }
            if (employee.getStatus() != EmployeeStatus.Active) {
                employee.setStatus(EmployeeStatus.Active);
                employee.setResignedAt(null);
                employee.setResignReason(null);
                employee = employeeRepository.save(employee);
            }
        }

        Credentials credential = credentialsRepository.findFirstByPhoneOrderByCreIdDesc(phone.trim());
        if (credential == null) {
            credential = new Credentials();
            credential.setType(CredType.Password);
            credential.setIdKey(phone.trim());
            credential.setIdSecret("password");
            credential.setPhone(phone.trim());
            credential.setEmployee(employee);
        }
        credential.setStatus(UserStatus.Active);
        credential.setPasswordHash(passwordContext.encode(password));
        credential.setPasswordUpdatedAt(LocalDateTime.now());
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setMustChangePassword(false);
        credentialsRepository.save(credential);
        LOGGER.info("平台运营账号初始化完成，企业={}，手机号后四位={}", company.getTitle(), phone.substring(Math.max(0, phone.length() - 4)));
    }
}
