package com.th4.edge.tenant.user.context;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.SecurityAction;
import com.th4.edge.tenant.user.configure.SecurityResult;
import com.th4.edge.tenant.user.configure.UserStatus;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.UserSecurityLog;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.UserSecurityLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 平台用户识别上下文。
 *
 * <p>微信凭证使用 type=Wx、idKey=openId、idSecret=unionId；Web 密码凭证使用
 * type=Password、idKey=手机号，并在绑定微信时升级为 Wx。未加入企业时
 * employee 为空，加入企业后指向当前有效 Employee。</p>
 */
@Component
public class UserIdentityContext {

    private final CredentialsRepository credentialsRepository;
    private final EmployeeRepository employeeRepository;
    private final UserSecurityLogRepository securityLogRepository;
    private final ModelIdentityContext modelIdentityContext;

    public UserIdentityContext(CredentialsRepository credentialsRepository,
                               EmployeeRepository employeeRepository,
                               UserSecurityLogRepository securityLogRepository,
                               ModelIdentityContext modelIdentityContext) {
        this.credentialsRepository = credentialsRepository;
        this.employeeRepository = employeeRepository;
        this.securityLogRepository = securityLogRepository;
        this.modelIdentityContext = modelIdentityContext;
    }

    @Transactional
    public Credentials identify(String openId, String unionId, String phone, String nickname, String avatarUrl) {
        String normalizedOpenId = required(openId, "openId");
        String normalizedPhone = required(phone, "手机号");
        Credentials credential = credentialsRepository.findByTypeAndIdKey(CredType.Wx, normalizedOpenId);
        if (credential != null) {
            if (credential.getStatus() != UserStatus.Active) {
                throw new DefinedException(new DefaultErrorCode("403", "当前微信用户已停用"));
            }
            if (credential.getPhone() != null && !normalizedPhone.equals(credential.getPhone())) {
                log(credential, SecurityAction.PhoneBind, SecurityResult.Failure, null, null,
                        "openId 已绑定其他手机号");
                throw new DefinedException(new DefaultErrorCode("400", "当前微信已绑定其他手机号"));
            }
            credential.setPhone(normalizedPhone);
            if (unionId != null && !unionId.isBlank()) credential.setIdSecret(unionId);
            return bindEmployee(credential, avatarUrl);
        }

        Credentials byPhone = credentialsRepository.findFirstByPhoneOrderByCreIdDesc(normalizedPhone);
        if (byPhone == null) {
            // 兼容旧仓储测试/旧数据索引；生产新查询会先命中任意凭证类型。
            byPhone = credentialsRepository.findByTypeAndPhone(CredType.Wx, normalizedPhone);
        }
        if (byPhone != null) {
            if (byPhone.getStatus() != UserStatus.Active) {
                throw new DefinedException(new DefaultErrorCode("403", "当前用户已停用"));
            }
            if (byPhone.getType() == CredType.Wx) {
                log(byPhone, SecurityAction.PhoneBind, SecurityResult.Blocked, null, null,
                        "手机号已绑定其他微信");
                throw new DefinedException(new DefaultErrorCode("400", "该手机号已绑定其他微信，请联系客服"));
            }
            byPhone.setType(CredType.Wx);
            byPhone.setIdKey(normalizedOpenId);
            byPhone.setIdSecret(unionId);
            byPhone.setPhone(normalizedPhone);
            return bindEmployee(byPhone, avatarUrl);
        }

        credential = new Credentials();
        credential.setType(CredType.Wx);
        credential.setIdKey(normalizedOpenId);
        credential.setIdSecret(unionId);
        credential.setPhone(normalizedPhone);
        credential.setStatus(UserStatus.Active);
        credential = bindEmployee(credential, avatarUrl);
        log(credential, SecurityAction.Register, SecurityResult.Success,
                credential.getEmployee(), null, "微信用户注册");
        return credential;
    }

    @Transactional
    public Credentials requireUser(Long userId) {
        Credentials credential = credentialsRepository.findByCreId(userId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("401", "当前用户不存在")));
        if (credential.getStatus() != UserStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("403", "当前用户已停用"));
        }
        return credential;
    }

    @Transactional
    public Credentials lock(Long userId) {
        return credentialsRepository.findLockedByCreId(userId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("401", "当前用户不存在")));
    }

    public Employee activeEmployee(Credentials credential) {
        if (credential == null) return null;
        if (credential.getEmployee() != null && credential.getEmployee().getStatus() == EmployeeStatus.Active) {
            return credential.getEmployee();
        }
        return null;
    }

    public Employee requireActiveEmployee(Credentials credential) {
        Employee employee = activeEmployee(credential);
        if (employee == null) {
            throw new DefinedException(new DefaultErrorCode("409", "当前用户尚未加入企业"));
        }
        return employee;
    }

    public Credentials requireWx(Employee employee) {
        if (employee == null) return null;
        return credentialsRepository
                .findFirstByEmployee_EmpIdOrderByCreIdDesc(employee.getEmpId());
    }

    public Credentials findWxByPhone(String phone) {
        return phone == null ? null : credentialsRepository.findFirstByPhoneOrderByCreIdDesc(phone);
    }

    /** 微信 code 已换取 openId 后，优先查询已有凭证，避免重复要求手机号授权。 */
    public Credentials findWxByOpenId(String openId) {
        return openId == null || openId.isBlank()
                ? null : credentialsRepository.findByTypeAndIdKey(CredType.Wx, openId);
    }

    public Credentials credentialFor(Employee employee) {
        return requireWx(employee);
    }

    @Transactional
    public Credentials linkEmployee(Credentials credential, Employee employee) {
        credential.setEmployee(employee);
        return credentialsRepository.save(credential);
    }

    @Transactional
    public void touchLogin(Credentials credential) {
        credential.setLastTime(LocalDateTime.now());
        credentialsRepository.save(credential);
        log(credential, SecurityAction.Login, SecurityResult.Success,
                activeEmployee(credential), null, "登录成功");
    }

    @Transactional
    public UserSecurityLog log(Credentials credential, SecurityAction action, SecurityResult result,
                               Employee employee, String remark, String detail) {
        UserSecurityLog log = new UserSecurityLog();
        if (credential != null) {
            log.setUserId(credential.getCreId());
            log.setOpenId(credential.getOpenId());
            log.setPhone(credential.getPhone());
        }
        if (employee != null) {
            log.setCompanyId(employee.getCompany() == null ? null : employee.getCompany().getComId());
            log.setEmpId(employee.getEmpId());
        }
        log.setAction(action);
        log.setResult(result);
        log.setRemark(remark == null ? detail : remark + (detail == null ? "" : "：" + detail));
        return securityLogRepository.save(modelIdentityContext.prepare(log));
    }

    @Transactional
    public UserSecurityLog logUnknown(String phone, SecurityAction action,
                                      SecurityResult result, String detail) {
        UserSecurityLog log = new UserSecurityLog();
        log.setPhone(phone);
        log.setAction(action);
        log.setResult(result);
        log.setRemark(detail);
        return securityLogRepository.save(modelIdentityContext.prepare(log));
    }

    private Credentials bindEmployee(Credentials credential, String avatarUrl) {
        Employee employee = activeEmployee(credential);
        if (employee != null) {
            credential.setEmployee(employee);
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                employee.setAvatarUrl(avatarUrl);
                employeeRepository.save(employee);
            }
        }
        return credentialsRepository.save(modelIdentityContext.prepare(credential));
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", label + "不能为空"));
        }
        return value.trim();
    }
}
