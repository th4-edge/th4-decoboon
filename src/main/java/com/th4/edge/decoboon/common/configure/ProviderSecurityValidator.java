package com.th4.edge.decoboon.common.configure;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 启动时校验业务 Provider 必须继承 Security，公开路由只能来自显式白名单。 */
@Component
public class ProviderSecurityValidator implements ApplicationRunner {

    /** 仅允许无需登录或使用独立签名/渠道验签的公开入口。 */
    public static final Set<String> PUBLIC_ROUTES = Set.of(
            "auth/session/login",
            "auth/session/password-login",
            "auth/session/password-setup",
            "auth/session/register",
            "identity/customer-invite/accept",
            "identity/customer-invite/info",
            "invite/invitation/accept",
            "invite/invitation/info",
            "invite/invitation/reject",
            "order/callback",
            "file/private/download"
    );

    private final ApplicationContext applicationContext;

    public ProviderSecurityValidator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> violations = new ArrayList<>();
        for (Provider<?, ?> provider : applicationContext.getBeansOfType(Provider.class).values()) {
            Class<?> target = AopUtils.getTargetClass(provider);
            if (!target.getName().startsWith("com.th4.edge.decoboon")
                    && !target.getName().startsWith("com.th4.edge.tenant")) {
                continue;
            }
            Service service = AnnotationUtils.findAnnotation(target, Service.class);
            if (service == null || service.value().isBlank() || service.value().contains("{")) {
                continue;
            }
            String route = service.value();
            if (PUBLIC_ROUTES.contains(route)) {
                continue;
            }
            if (!(provider instanceof Security)) {
                violations.add(route + " -> " + target.getName());
            }
        }
        if (!violations.isEmpty()) {
            throw new IllegalStateException("发现未显式声明 Security 的业务路由：\n" + String.join("\n", violations));
        }
    }
}
