package com.th4.edge.tenant.user.access.filter;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.RequestContext;
import com.th4.edge.framework.provider.filter.RequestFilter;
import com.th4.edge.framework.provider.security.SecurityManager;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.access.context.DataPermissionCrudContext;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

/**
 * 框架通用 ModelUpdate Provider 的数据权限修改过滤器。
 *
 * <p>在 {@code ProviderFieldInjectRequestFilter}（优先级 190）完成
 * {@code @ModelUpdateInject} 注值后执行，重新按当前数据范围加载旧实体并保护
 * company/store/team/owner 归属字段，防止前端通过模型注值修改数据归属。</p>
 */
@Note("数据权限修改过滤器")
@Component
public class AccessDataPermissionUpdateRequestFilter implements RequestFilter<Object> {

    private final SecurityManager securityManager;
    private final DataScopeContext dataScopeContext;
    private final DataPermissionCrudContext crudContext;

    public AccessDataPermissionUpdateRequestFilter(SecurityManager securityManager,
                                                   DataScopeContext dataScopeContext,
                                                   DataPermissionCrudContext crudContext) {
        this.securityManager = securityManager;
        this.dataScopeContext = dataScopeContext;
        this.crudContext = crudContext;
    }

    @Override
    public Object prepare(Provider<Object, ?> provider, Object request,
                          RequestContext requestContext) {
        DataPermission permission = AnnotationUtils.findAnnotation(
                request.getClass(), DataPermission.class);
        if (permission == null) {
            return request;
        }

        User<?> principal = securityManager.authenticate(requestContext);
        if (!(principal instanceof Employee employee)) {
            throw new DefinedException(new DefaultErrorCode("401", "登录状态已失效"));
        }
        Employee current = dataScopeContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);
        return crudContext.prepareUpdate(request, current, scope, permission);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean support(Provider<Object, ?> provider, Object request) {
        return provider instanceof com.th4.edge.framework.service.update.Update
                && request != null
                && AnnotationUtils.findAnnotation(request.getClass(), DataPermission.class) != null;
    }

    @Override
    public int getPriority() {
        // ProviderFieldInjectRequestFilter=190，本过滤器必须在其后保护归属字段不被模型注值覆盖。
        return 185;
    }
}
