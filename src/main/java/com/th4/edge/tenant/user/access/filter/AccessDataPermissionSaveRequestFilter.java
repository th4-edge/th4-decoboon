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
 * 框架通用 ModelSave Provider 的数据权限保存过滤器。
 *
 * <p>该过滤器在 {@code ProviderFieldInjectRequestFilter}（优先级 190）完成
 * {@code @ModelSaveInject} 注值后执行，重新使用当前登录员工的真实企业、门店、团队和
 * 负责人覆盖请求中的归属字段，防止前端模型注值覆盖服务端数据权限。</p>
 */
@Note("数据权限保存过滤器")
@Component
public class AccessDataPermissionSaveRequestFilter implements RequestFilter<Object> {

    private final SecurityManager securityManager;
    private final DataScopeContext dataScopeContext;
    private final DataPermissionCrudContext crudContext;

    public AccessDataPermissionSaveRequestFilter(SecurityManager securityManager,
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
        return crudContext.prepareCreate(request, current, scope, permission);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean support(Provider<Object, ?> provider, Object request) {
        return provider instanceof com.th4.edge.framework.service.save.Save
                && request != null
                && AnnotationUtils.findAnnotation(request.getClass(), DataPermission.class) != null;
    }

    @Override
    public int getPriority() {
        // ProviderFieldInjectRequestFilter=190，本过滤器必须在其后覆盖 @ModelSaveInject 的归属注值。
        return 185;
    }
}
