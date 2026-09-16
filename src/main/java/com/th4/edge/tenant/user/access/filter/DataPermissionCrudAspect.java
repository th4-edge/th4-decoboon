package com.th4.edge.tenant.user.access.filter;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.RequestContext;
import com.th4.edge.framework.provider.RequestContextHolder;
import com.th4.edge.framework.provider.security.SecurityManager;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.access.context.DataPermissionCrudContext;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * th4 通用 ModelService CRUD 的数据权限切面。
 *
 * <p>该切面只处理框架通用 service/save、service/update、service/delete、
 * service/info；page/list/count 由 Criteria 拦截器处理。</p>
 */
@Note("数据权限 CRUD 切面")
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataPermissionCrudAspect {

    private final SecurityManager securityManager;
    private final DataScopeContext dataScopeContext;
    private final DataPermissionCrudContext crudContext;
    private final TransactionTemplate transactionTemplate;

    public DataPermissionCrudAspect(SecurityManager securityManager,
                                    DataScopeContext dataScopeContext,
                                    DataPermissionCrudContext crudContext,
                                    PlatformTransactionManager transactionManager) {
        this.securityManager = securityManager;
        this.dataScopeContext = dataScopeContext;
        this.crudContext = crudContext;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Around("execution(* com.th4.edge.framework.service..*.call(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object target = joinPoint.getTarget();
        Object[] arguments = joinPoint.getArgs();
        if (arguments.length == 0 || arguments[0] == null
                || !isCrudService(target)) {
            return joinPoint.proceed();
        }

        Object request = arguments[0];
        if (!request.getClass().isAnnotationPresent(DataPermission.class)) {
            return joinPoint.proceed();
        }

        RequestContext requestContext = RequestContextHolder.getRequestContext();
        if (requestContext == null) {
            throw new DefinedException(new DefaultErrorCode("401", "登录状态已失效"));
        }

        try {
            return transactionTemplate.execute(status -> {
                try {
                    Object prepared = prepare(target, request, requestContext);
                    return joinPoint.proceed(new Object[]{prepared});
                } catch (Throwable throwable) {
                    throw new DataPermissionInvocationException(throwable);
                }
            });
        } catch (DataPermissionInvocationException exception) {
            throw exception.getCause();
        }
    }

    private Object prepare(Object provider, Object request,
                           RequestContext requestContext) {
        DataPermission permission = request.getClass().getAnnotation(DataPermission.class);
        User<?> principal = securityManager.authenticate(requestContext);
        if (!(principal instanceof Employee employee)) {
            throw new DefinedException(new DefaultErrorCode("401", "登录状态已失效"));
        }
        Employee current = dataScopeContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);

        if (provider instanceof com.th4.edge.framework.service.save.Save<?>) {
            return crudContext.prepareCreate(request, current, scope, permission);
        }
        if (provider instanceof com.th4.edge.framework.service.update.Update<?>) {
            return crudContext.prepareUpdate(request, current, scope, permission);
        }
        if (provider instanceof com.th4.edge.framework.service.delete.Delete<?>
                || provider instanceof com.th4.edge.framework.service.info.Info<?>) {
            return crudContext.requireVisible(request, permission, scope);
        }
        return request;
    }

    private boolean isCrudService(Object target) {
        return target instanceof com.th4.edge.framework.service.save.Save<?>
                || target instanceof com.th4.edge.framework.service.update.Update<?>
                || target instanceof com.th4.edge.framework.service.delete.Delete<?>
                || target instanceof com.th4.edge.framework.service.info.Info<?>;
    }

    private static final class DataPermissionInvocationException extends RuntimeException {

        private DataPermissionInvocationException(Throwable cause) {
            super(cause);
        }

        @Override
        public synchronized Throwable getCause() {
            return super.getCause();
        }
    }
}
