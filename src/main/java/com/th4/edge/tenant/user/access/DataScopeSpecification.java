package com.th4.edge.tenant.user.access;

import org.springframework.data.jpa.domain.Specification;

/**
 * Repository 数据范围 Specification。
 */
public final class DataScopeSpecification {

    private DataScopeSpecification() {
    }

    public static <T> Specification<T> visible(Class<T> modelClass, DataScope scope) {
        return (root, query, builder) -> DataScopeCriteria.build(builder, root, modelClass, scope);
    }
}
