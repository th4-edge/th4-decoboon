package com.th4.edge.tenant.user.context;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.components.lock.DistributedLocked;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.SecurityAction;
import com.th4.edge.tenant.user.configure.SecurityResult;
import com.th4.edge.tenant.user.configure.UserStatus;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 员工密码凭证的创建、锁定、校验和审计边界。 */
@Component
public class PasswordAuthContext {

    private final CredentialsRepository credentialsRepository;
    private final EmployeeRepository employeeRepository;
    private final UserIdentityContext userContext;
    private final PasswordContext passwordContext;
    private final ModelIdentityContext modelIdentityContext;
    private final int maxFailedAttempts;
    private final int lockMinutes;

    public PasswordAuthContext(CredentialsRepository credentialsRepository,
                               EmployeeRepository employeeRepository,
                               UserIdentityContext userContext,
                               PasswordContext passwordContext,
                               ModelIdentityContext modelIdentityContext,
                               @Value("${th4.edge.framework.security.password.max-failed-attempts:5}")
                               int maxFailedAttempts,
                               @Value("${th4.edge.framework.security.password.lock-minutes:15}")
                               int lockMinutes) {
        this.credentialsRepository = credentialsRepository;
        this.employeeRepository = employeeRepository;
        this.userContext = userContext;
        this.passwordContext = passwordContext;
        this.modelIdentityContext = modelIdentityContext;
        this.maxFailedAttempts = Math.max(1, maxFailedAttempts);
        this.lockMinutes = Math.max(1, lockMinutes);
    }

    /** 首设只允许存在员工账号且从未设置密码的凭证；没有凭证时创建 Password 行。 */
    @DistributedLocked(key = "#root.phoneLockKey(#phone)")
    @Transactional
    public Credentials requireSetupCredential(String phone) {
        String normalizedPhone = requiredPhone(phone);
        Credentials credential = credentialsRepository.findByPhone(normalizedPhone).orElse(null);
        Employee employee = credential == null ? null : userContext.activeEmployee(credential);
        if (employee == null) {
            employee = employeeRepository.findFirstByTelNoAndStatusOrderByEmpIdDesc(
                    normalizedPhone, EmployeeStatus.Active);
        }
        if (employee == null) {
            throw new DefinedException(new DefaultErrorCode("404", "员工账号不存在或已停用"));
        }
        if (credential == null) {
            credential = credentialsRepository.findFirstByEmployee_EmpIdOrderByCreIdDesc(employee.getEmpId());
        }
        if (credential == null) {
            credential = new Credentials();
            credential.setType(CredType.Password);
            credential.setIdKey(normalizedPhone);
            credential.setPhone(normalizedPhone);
            credential.setStatus(UserStatus.Active);
            credential.setEmployee(employee);
            return credentialsRepository.save(modelIdentityContext.prepare(credential));
        }
        if (credential.getStatus() != UserStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号已停用"));
        }
        if (credential.getPhone() != null && !credential.getPhone().equals(normalizedPhone)) {
            throw new DefinedException(new DefaultErrorCode("409", "账号手机号不一致，请由管理员核验"));
        }
        credential.setPhone(normalizedPhone);
        if (credential.getEmployee() == null) {
            credential.setEmployee(employee);
        }
        return credential;
    }

    /** 登录按手机号串行，避免并发失败计数丢更新（原行级悲观锁迁移为分布式锁）。 */
    @DistributedLocked(key = "#root.phoneLockKey(#phone)")
    @Transactional
    public Credentials lockLoginCredential(String phone) {
        Credentials credential = credentialsRepository.findByPhone(requiredPhone(phone)).orElse(null);
        if (credential == null) {
            return null;
        }
        if (credential.getStatus() != UserStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号已停用"));
        }
        if (credential.getPasswordHash() == null || credential.getPasswordHash().isBlank()) {
            return null;
        }
        if (credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(LocalDateTime.now())) {
            userContext.log(credential, SecurityAction.Login, SecurityResult.Blocked,
                    userContext.activeEmployee(credential), "账号锁定中", null);
            throw new DefinedException(new DefaultErrorCode("ACCOUNT_LOCKED", "账号暂时锁定，请稍后重试"));
        }
        if (credential.getLockedUntil() != null) {
            credential.setLockedUntil(null);
            credential.setFailedAttempts(0);
        }
        return credential;
    }

    public boolean matches(String rawPassword, Credentials credential) {
        return passwordContext.matches(rawPassword, credential.getPasswordHash());
    }

    @Transactional
    public void loginFailed(Credentials credential, String reason) {
        int attempts = credential.getFailedAttempts() == null ? 0 : credential.getFailedAttempts();
        attempts++;
        credential.setFailedAttempts(attempts);
        if (attempts >= maxFailedAttempts) {
            credential.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
        }
        credentialsRepository.save(credential);
        Employee employee = userContext.activeEmployee(credential);
        if (attempts >= maxFailedAttempts) {
            userContext.log(credential, SecurityAction.Login, SecurityResult.Blocked, employee, reason, null);
            userContext.log(credential, SecurityAction.AccountLock, SecurityResult.Success, employee,
                    "连续登录失败达到阈值", null);
        } else {
            userContext.log(credential, SecurityAction.Login, SecurityResult.Failure, employee, reason, null);
        }
    }

    @Transactional
    public void loginSucceeded(Credentials credential) {
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setLastTime(LocalDateTime.now());
        credentialsRepository.save(credential);
        userContext.log(credential, SecurityAction.Login, SecurityResult.Success,
                userContext.activeEmployee(credential), "密码登录成功", null);
    }

    @Transactional
    public Credentials setInitialPassword(Credentials credential, String rawPassword) {
        if (credential.getPasswordHash() != null && !credential.getPasswordHash().isBlank()) {
            throw new DefinedException(new DefaultErrorCode("PASSWORD_ALREADY_SET", "当前账号已设置密码"));
        }
        credential.setPasswordHash(passwordContext.encode(rawPassword));
        credential.setPasswordUpdatedAt(LocalDateTime.now());
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setMustChangePassword(false);
        Credentials saved = credentialsRepository.save(credential);
        userContext.log(saved, SecurityAction.PasswordSet, SecurityResult.Success,
                userContext.activeEmployee(saved), "首次设置密码", null);
        return saved;
    }

    @Transactional
    public Credentials changePassword(Credentials credential, String rawPassword) {
        credential.setPasswordHash(passwordContext.encode(rawPassword));
        credential.setPasswordUpdatedAt(LocalDateTime.now());
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setMustChangePassword(false);
        Credentials saved = credentialsRepository.save(credential);
        userContext.log(saved, SecurityAction.PasswordChange, SecurityResult.Success,
                userContext.activeEmployee(saved), "修改密码", null);
        return saved;
    }

    @Transactional
    public void logout(Credentials credential) {
        if (credential != null) {
            userContext.log(credential, SecurityAction.Logout, SecurityResult.Success,
                    userContext.activeEmployee(credential), "退出登录", null);
        }
    }

    private String requiredPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "手机号不能为空"));
        }
        return phone.trim();
    }

    /**
     * 凭证手机号锁键：与 {@link #requiredPhone(String)} 同口径 trim 后取键，
     * 保证「首设凭证」与「登录失败计数」对同一手机号串行；手机号为空时退化为不争用键。
     */
    public String phoneLockKey(String phone) {
        String normalized = phone == null ? "" : phone.trim();
        return "credential-phone:" + (normalized.isBlank() ? "blank" : normalized);
    }
}
