package com.th4.edge.tenant.user.service.auth.session.devownerlogin.support;

import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.repository.CustomerAccessRepository;
import com.th4.edge.decoboon.customer.repository.CustomerInviteRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.configure.UserStatus;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 本地开发业主登录实现；见接口注释的安全边界。 */
@Note("本地开发业主登录（仅 dev）")
@Description("为已授权的微信业主凭证签发本地会话，用于业主侧链路端到端验证；默认关闭且仅 dev 生效")
@Properties(allow = {
        @AllowProperties(model = Credentials.class,
                properties = {"creId", "phone", "status", "lastTime", "mustChangePassword"})
})
@Service("auth/session/dev-owner-login")
@Scope(ScopeTypes.PROTOTYPE)
public class DevOwnerLogin implements com.th4.edge.tenant.user.service.auth.session.devownerlogin.DevOwnerLogin {

    @Value("${decoboon.dev-owner-login.enabled:false}")
    private boolean enabled;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @AutoInject
    @NotEmpty
    private String phone;

    private final CredentialsRepository credentialsRepository;
    private final CustomerAccessRepository accessRepository;
    private final CustomerInviteRepository inviteRepository;

    public DevOwnerLogin(CredentialsRepository credentialsRepository,
                         CustomerAccessRepository accessRepository,
                         CustomerInviteRepository inviteRepository) {
        this.credentialsRepository = credentialsRepository;
        this.accessRepository = accessRepository;
        this.inviteRepository = inviteRepository;
    }

    @Override
    @Transactional
    public Credentials call(Credentials request) {
        if (!enabled || activeProfiles == null || !activeProfiles.contains("dev")) {
            // 非 dev 或未显式开启时与不存在的接口一致，不泄露该入口存在。
            throw new DefinedException(new DefaultErrorCode("404", "接口不存在"));
        }
        String normalized = phone == null ? "" : phone.trim();
        if (normalized.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "手机号不能为空"));
        }
        Credentials credential = credentialsRepository.findByTypeAndPhone(CredType.Wx, normalized);
        if (credential == null || credential.getStatus() != UserStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("403", "业主身份不存在或已停用"));
        }
        if (credential.getEmployee() != null) {
            throw new DefinedException(new DefaultErrorCode("403", "该手机号已绑定员工账号，不能作为业主登录"));
        }
        boolean granted = !accessRepository
                .findByCredential_CreIdAndStatusOrderByAccessIdAsc(
                        credential.getCreId(), CustomerAccessStatus.Active)
                .isEmpty();
        if (!granted) {
            // 还没有授权时，只允许「存在本人待接受邀请」的业主登录，用于走完真实接受邀请链路。
            granted = inviteRepository.findByTelNoOrderByInviteIdDesc(normalized).stream()
                    .anyMatch(invite -> invite.getStatus() == InviteStatus.Pending);
        }
        if (!granted) {
            throw new DefinedException(new DefaultErrorCode("403", "该业主尚未授权任何项目"));
        }
        return credential;
    }
}
