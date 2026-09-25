package com.th4.edge.components.data.listener;

import com.th4.edge.components.data.context.SnowflakeIdGenerator;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import jakarta.persistence.PrePersist;
import org.hibernate.Hibernate;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/** 为直接调用 Repository 保存的业务实体自动分配 Snowflake 主键。 */
public class SnowflakeEntityListener {

    @PrePersist
    public void assignId(Object entity) {
        if (entity == null) return;
        Class<?> type = Hibernate.getClass(entity);
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getAnnotation(SnowflakeId.class) == null) continue;
                ReflectionUtils.makeAccessible(field);
                if (ReflectionUtils.getField(field, entity) == null) {
                    ReflectionUtils.setField(field, entity, SnowflakeIdGenerator.nextId());
                }
            }
            type = type.getSuperclass();
        }
    }
}
