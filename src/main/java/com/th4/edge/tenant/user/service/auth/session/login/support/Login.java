package com.th4.edge.tenant.user.service.auth.session.login.support;
import com.th4.edge.components.wechat.service.WechatMiniProgramClient;
import com.th4.edge.components.wechat.service.WechatPhone;
import com.th4.edge.components.wechat.service.WechatSession;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.configure.UserStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Note("微信小程序登录")
@Properties(allow = {
        @AllowProperties(model = Credentials.class, properties = {"creId", "openId", "unionId", "phone", "status", "lastTime", "employee"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "createTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "addr", "setting"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"})
})
@Service("auth/session/login")
@Scope(ScopeTypes.PROTOTYPE)
public class Login implements com.th4.edge.tenant.user.service.auth.session.login.Login {
    @AutoInject @NotEmpty @Length(max = 128) private String code;
    @AutoInject @Length(max = 128) private String phoneCode;
    @AutoInject @Length(max = 64) private String nickname;
    @AutoInject @Length(max = 512) private String avatarUrl;
    private final WechatMiniProgramClient wechatClient;
    private final UserIdentityContext userContext;
    public Login(WechatMiniProgramClient wechatClient, UserIdentityContext userContext) {
        this.wechatClient = wechatClient; this.userContext = userContext;
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Login.class);

    /** openid 指纹：SHA-256 前 8 位十六进制，用于日志比对而不暴露标识本身。 */
    private static String fingerprint(String value) {
        if (value == null || value.isBlank()) return "(空)";
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        } catch (Exception e) {
            return "(不可计算)";
        }
    }

    @Override @Transactional
    public com.th4.edge.framework.provider.security.Token call(Credentials request) {
        WechatSession session = wechatClient.session(code);
        Credentials existing = userContext.findWxByOpenId(session.openId());
        // 诊断口径（XA-20260924-007）：只记录 openid 指纹（SHA-256 前 8 位）与命中结果，
        // 不写 openid 原文/手机号；用于区分"换码得到的 openid 不同/未命中凭证"与"首次登录"。
        log.info("auth/session/login 结果: openid指纹={} 命中凭证={} 带phoneCode={}",
                fingerprint(session.openId()), existing != null, phoneCode != null && !phoneCode.isBlank());
        if (existing != null) {
            if (existing.getStatus() != UserStatus.Active) {
                throw new DefinedException(new DefaultErrorCode("403", "当前微信用户已停用"));
            }
            userContext.touchLogin(existing);
            Employee employee = userContext.activeEmployee(existing);
            return employee == null ? existing : employee;
        }
        if (phoneCode == null || phoneCode.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("PHONE_REQUIRED", "首次登录请授权手机号"));
        }
        WechatPhone phone = wechatClient.phone(phoneCode);
        Credentials credential = userContext.identify(session.openId(), session.unionId(), phone.phone(), nickname, avatarUrl);
        userContext.touchLogin(credential);
        Employee employee = userContext.activeEmployee(credential);
        return employee == null ? credential : employee;
    }
}
