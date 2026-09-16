package com.th4.edge.tenant.user.access;

import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Criteria 数据范围条件构造器。
 */
public final class DataScopeCriteria {

    private static final String COM_ID = "comId";
    private static final String STORE_ID = "storeId";
    private static final String TEAM_ID = "teamId";
    private static final String EMP_ID = "empId";

    private DataScopeCriteria() {
    }

    /**
     * 向 CriteriaQuery 追加当前数据范围。
     */
    public static void apply(CriteriaBuilder builder, CriteriaQuery<?> query,
                             Class<?> modelClass, DataScope scope) {
        if (query == null) {
            throw new DefinedException(new DefaultErrorCode("403", "数据范围查询上下文缺失"));
        }
        Root<?> root = query.getRoots().stream()
                .filter(candidate -> candidate.getJavaType().equals(modelClass))
                .findFirst()
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("403", "数据范围查询根对象缺失")));
        Predicate permission = build(builder, root, modelClass, scope);
        Predicate restriction = query.getRestriction();
        query.where(restriction == null ? permission : builder.and(restriction, permission));
    }

    /**
     * 构造当前数据范围对应的 Predicate。
     */
    public static Predicate build(CriteriaBuilder builder, Root<?> root,
                                  Class<?> modelClass, DataScope scope) {
        if (scope == null || scope.type() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未配置数据范围"));
        }
        DataPermission permission = modelClass.getAnnotation(DataPermission.class);
        if (permission == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前数据模型未配置权限字段"));
        }
        Predicate company = builder.equal(
                root.get(permission.company()).get(COM_ID), scope.comId());
        return switch (scope.type()) {
            case Company -> company;
            case Store -> builder.and(company, builder.equal(
                    root.get(permission.store()).get(STORE_ID), require(scope.storeId(), "当前账号未归属门店")));
            case Team -> builder.and(company, builder.equal(
                    root.get(permission.team()).get(TEAM_ID), require(scope.teamId(), "当前账号未归属团队")));
            case Self -> builder.and(company, builder.equal(
                    root.get(permission.owner()).get(EMP_ID), require(scope.empId(), "当前账号标识缺失")));
        };
    }

    private static Long require(Long value, String message) {
        if (value == null) {
            throw new DefinedException(new DefaultErrorCode("403", message));
        }
        return value;
    }
}
