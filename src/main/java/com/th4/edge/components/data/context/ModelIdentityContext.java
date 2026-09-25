package com.th4.edge.components.data.context;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import org.hibernate.Hibernate;
import org.springframework.util.ReflectionUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * 自定义领域写入前的模型标识准备。
 *
 * <p>框架通用 ModelService 会自动执行字段准备；注册、邀请等自定义 Provider
 * 直接调用 Repository 时，需要显式按 @SnowflakeId 契约生成 Long 主键。</p>
 */
@Note("模型标识上下文")
@Component
public class ModelIdentityContext {

    /**
     * 为尚未赋值的 @SnowflakeId 字段生成主键。
     */
    public <T> T prepare(T model) {
        Class<?> type = Hibernate.getClass(model);
        for (Field field : type.getDeclaredFields()) {
            SnowflakeId annotation = field.getAnnotation(SnowflakeId.class);
            if (annotation != null) {
                ReflectionUtils.makeAccessible(field);
                if (ReflectionUtils.getField(field, model) == null) {
                    ReflectionUtils.setField(field, model, SnowflakeIdGenerator.nextId());
                }
            }
        }
        return model;
    }
}
