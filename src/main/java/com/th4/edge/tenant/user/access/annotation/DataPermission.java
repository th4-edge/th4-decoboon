package com.th4.edge.tenant.user.access.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明业务实体的数据权限归属字段。
 *
 * <p>字段名必须对应实体属性名；未声明的业务实体不会启用统一数据范围拦截。</p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {

    /** 企业归属字段。 */
    String company() default "company";

    /** 门店归属字段。 */
    String store() default "store";

    /** 团队归属字段。 */
    String team() default "team";

    /** 个人归属字段。 */
    String owner() default "owner";
}
