package com.th4.edge.tenant.user.access.filter;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.RequestContext;
import com.th4.edge.framework.provider.criteria.CriteriaQueryAware;
import com.th4.edge.framework.provider.filter.RequestFilter;
import com.th4.edge.framework.provider.security.SecurityManager;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeCriteria;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

/**
 * Provider 数据范围统一拦截器。
 *
 * <p>在框架完成 CriteriaQuery 构建后、业务扩展条件之前加入企业及角色数据范围条件。</p>
 */
@Note("数据范围权限拦截器")
@Component
public class DataScopeRequestFilter implements RequestFilter<Object> {

    private final SecurityManager securityManager;
    private final DataScopeContext dataScopeContext;
    private final EntityManager entityManager;

    public DataScopeRequestFilter(SecurityManager securityManager,
                                  DataScopeContext dataScopeContext,
                                  EntityManager entityManager) {
        this.securityManager = securityManager;
        this.dataScopeContext = dataScopeContext;
        this.entityManager = entityManager;
    }

    @Override
    public Object prepare(Provider<Object, ?> provider, Object request,
                          RequestContext requestContext) {
        if (!(provider instanceof CriteriaQueryAware criteriaQueryAware)) {
            return request;
        }
        User<?> user = securityManager.authenticate(requestContext);
        if (!(user instanceof Employee employee)) {
            throw new DefinedException(new DefaultErrorCode("401", "登录状态已失效"));
        }
        DataScope scope = dataScopeContext.requireScope(employee);
        DataScopeCriteria.apply(entityManager.getCriteriaBuilder(),
                criteriaQueryAware.getCriteriaQuery(), request.getClass(), scope);
        return request;
    }

    @Override
    public boolean support(Provider<Object, ?> provider, Object request) {
        return provider instanceof CriteriaQueryAware
                && request != null
                && request.getClass().isAnnotationPresent(DataPermission.class);
    }

    @Override
    public int getPriority() {
        // CriteriaQueryAwareRequestFilter=160，业务查询条件=150。
        return 155;
    }
}
