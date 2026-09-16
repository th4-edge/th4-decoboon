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
    @Override @Transactional
    public com.th4.edge.framework.provider.security.Token call(Credentials request) {
        WechatSession session = wechatClient.session(code);
        Credentials existing = userContext.findWxByOpenId(session.openId());
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
