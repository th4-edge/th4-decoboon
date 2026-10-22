package com.th4.edge.tenant.billing.filter;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.RequestContext;
import com.th4.edge.framework.provider.filter.RequestFilter;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.provider.security.SecurityManager;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.billing.configure.PlanModule;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 企业订阅权益统一拦截器。
 *
 * <p>订阅到期、冻结或关闭后只放行查询类能力；其余写操作直接返回续费提醒。
 * 套餐未开通对应功能模块时，无论查询还是写入均返回升级提醒。所有租户员工请求先经过
 * {@link TenantContext} 的应用角色门禁；续费与文件路由也不能绕过 TeamLeader 冻结（DEC-082）。</p>
 */
@Note("订阅权益请求拦截器")
@Component
public class SubscriptionAccessRequestFilter implements RequestFilter<Object> {

    private static final Set<String> QUERY_CAPABILITIES = Set.of(
            "page", "list", "info", "count", "enums", "tree", "report", "unique");

    private final SecurityManager securityManager;
    private final TenantContext tenantContext;
    private final SubscriptionContext subscriptionContext;

    public SubscriptionAccessRequestFilter(SecurityManager securityManager,
                                           TenantContext tenantContext,
                                           SubscriptionContext subscriptionContext) {
        this.securityManager = securityManager;
        this.tenantContext = tenantContext;
        this.subscriptionContext = subscriptionContext;
    }

    @Override
    public Object prepare(Provider<Object, ?> provider, Object request,
                          RequestContext requestContext) {
        String route = route(provider);
        if (route == null) {
            return request;
        }

        User<?> principal = securityManager.authenticate(requestContext);
        if (!(principal instanceof Employee employee)) {
            return request;
        }
        Employee current = tenantContext.requireEmployee(employee);
        if (isSubscriptionRoute(route)) {
            return request;
        }

        if (!isQuery(route)) {
            subscriptionContext.requireWritable(current.getCompany());
        }
        subscriptionContext.requireModule(current.getCompany(), moduleOf(route));
        return request;
    }

    @Override
    public boolean support(Provider<Object, ?> provider, Object request) {
        return provider instanceof Security && route(provider) != null;
    }

    @Override
    public int getPriority() {
        // TokenValidateRequestFilter=190，本过滤器紧随其后，先于业务 Provider 执行。
        return 180;
    }

    private boolean isQuery(String route) {
        int index = route.lastIndexOf('/');
        return index >= 0 && QUERY_CAPABILITIES.contains(route.substring(index + 1));
    }

    private boolean isSubscriptionRoute(String route) {
        return route.startsWith("subscription/")
                || route.startsWith("plan/")
                || route.startsWith("order/")
                || route.startsWith("billing/")
                || route.startsWith("file/private/");
    }

    private PlanModule moduleOf(String route) {
        String domain = route.substring(0, route.indexOf('/'));
        if (route.startsWith("project/purchase")
                || route.startsWith("project/goods-receipt")
                || route.startsWith("project/labor")) {
            return PlanModule.Supply;
        }
        return switch (domain) {
            case "customer", "property", "identity" -> PlanModule.Crm;
            case "design" -> PlanModule.Design;
            case "catalog", "pack", "quote" -> PlanModule.Quote;
            case "project", "collab" -> PlanModule.Project;
            case "supply", "inventory", "stock", "partner", "partner-price" -> PlanModule.Supply;
            case "finance" -> PlanModule.Finance;
            case "statistic" -> PlanModule.Analytics;
            case "ai" -> PlanModule.Ai;
            case "api" -> PlanModule.Api;
            default -> null;
        };
    }

    private String route(Provider<?, ?> provider) {
        Service annotation = AnnotationUtils.findAnnotation(
                AopUtils.getTargetClass(provider), Service.class);
        if (annotation == null || annotation.value().isBlank()) {
            return null;
        }
        return annotation.value();
    }
}
