package com.th4.edge.tenant.user.access.context;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeCriteria;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 框架通用 ModelService CRUD 的数据权限执行器。
 */
@Note("数据权限 CRUD 上下文")
@Component
public class DataPermissionCrudContext {

    private static final Set<String> NON_COPY_PROPERTIES = Set.of(
            "class", "createTime", "updateTime");

    private final EntityManager entityManager;

    public DataPermissionCrudContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 创建时强制由服务端组装企业和个人归属。
     */
    public Object prepareCreate(Object request, Employee current,
                                DataScope scope, DataPermission permission) {
        Company company = current.getCompany();
        if (company == null || company.getComId() == null
                || !company.getComId().equals(scope.comId())) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属有效企业"));
        }
        Employee owner = resolveOwner(request, permission, current, scope, current);
        write(request, permission.company(), company);
        writeOwner(request, permission, owner);
        return request;
    }

    /**
     * 更新前加载当前范围内实体，合并允许修改的字段并保留强制归属字段。
     */
    public Object prepareUpdate(Object request, Employee current,
                                DataScope scope, DataPermission permission) {
        Object existing = requireVisible(request, permission, scope);
        Employee owner = resolveOwner(request, permission, current, scope,
                read(existing, permission.owner(), Employee.class));
        copyMutableProperties(request, existing, permission);
        write(existing, permission.company(), read(existing, permission.company(), Company.class));
        writeOwner(existing, permission, owner);
        return existing;
    }

    /**
     * 详情和删除前按当前数据范围加载实体。
     */
    public Object requireVisible(Object request, DataPermission permission, DataScope scope) {
        Class<?> modelClass = request.getClass();
        Object id = readId(request);
        if (id == null) {
            throw new DefinedException(new DefaultErrorCode("400", "业务主键不能为空"));
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        @SuppressWarnings("unchecked")
        CriteriaQuery<Object> query = (CriteriaQuery<Object>) builder.createQuery(modelClass);
        @SuppressWarnings("unchecked")
        Root<Object> root = query.from((Class<Object>) modelClass);
        Predicate idPredicate = builder.equal(root.get(idProperty(modelClass)), id);
        Predicate permissionPredicate = DataScopeCriteria.build(
                builder, root, modelClass, scope);
        query.where(builder.and(idPredicate, permissionPredicate));

        return entityManager.createQuery(query)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "数据不存在或无权访问")));
    }

    private Employee resolveOwner(Object request, DataPermission permission,
                                  Employee current, DataScope scope, Employee fallback) {
        if (current.getRole() != EmployeeRole.Boss) {
            return fallback;
        }
        Object requested = read(request, permission.owner(), Object.class, true);
        if (requested == null) {
            return fallback;
        }
        Object ownerId = readId(requested);
        if (ownerId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "负责人主键不能为空"));
        }
        Employee owner = entityManager.find(Employee.class, ownerId);
        if (owner == null || owner.getCompany() == null
                || !owner.getCompany().getComId().equals(scope.comId())) {
            throw new DefinedException(new DefaultErrorCode("400", "负责人不属于当前企业"));
        }
        return owner;
    }

    private void writeOwner(Object target, DataPermission permission, Employee owner) {
        write(target, permission.owner(), owner);
        write(target, permission.store(), owner == null ? null : owner.getStore());
        write(target, permission.team(), owner == null ? null : owner.getTeam());
    }

    private void copyMutableProperties(Object source, Object target, DataPermission permission) {
        Set<String> excluded = new HashSet<>(NON_COPY_PROPERTIES);
        excluded.addAll(Arrays.asList(
                permission.company(), permission.store(), permission.team(), permission.owner()));
        excluded.add(idProperty(source.getClass()));

        for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(source.getClass())) {
            String name = descriptor.getName();
            Method readMethod = descriptor.getReadMethod();
            Method writeMethod = descriptor.getWriteMethod();
            if (readMethod == null || writeMethod == null || excluded.contains(name)) {
                continue;
            }
            Object value = invoke(readMethod, source);
            if (value != null) {
                invoke(writeMethod, target, value);
            }
        }
    }

    private Object readId(Object target) {
        return read(target, idProperty(target.getClass()), Object.class);
    }

    private String idProperty(Class<?> modelClass) {
        Field idField = findIdField(modelClass);
        if (idField == null) {
            throw new DefinedException(new DefaultErrorCode("400", "业务实体缺少主键声明"));
        }
        return idField.getName();
    }

    private Field findIdField(Class<?> modelClass) {
        Class<?> current = modelClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (AnnotationUtils.findAnnotation(field, jakarta.persistence.Id.class) != null) {
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Object read(Object target, String property) {
        return read(target, property, Object.class);
    }

    private <T> T read(Object target, String property, Class<T> type) {
        return read(target, property, type, false);
    }

    private <T> T read(Object target, String property, Class<T> type, boolean optional) {
        PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(target.getClass(), property);
        if (descriptor == null || descriptor.getReadMethod() == null) {
            if (optional) {
                return null;
            }
            throw new DefinedException(new DefaultErrorCode("403", "数据权限字段不存在: " + property));
        }
        return type.cast(invoke(descriptor.getReadMethod(), target));
    }

    private void write(Object target, String property, Object value) {
        PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(target.getClass(), property);
        if (descriptor == null || descriptor.getWriteMethod() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "数据权限字段不可写: " + property));
        }
        invoke(descriptor.getWriteMethod(), target, value);
    }

    private Object invoke(Method method, Object target, Object... arguments) {
        ReflectionUtils.makeAccessible(method);
        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation
                    ? invocation.getTargetException()
                    : exception;
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new DefinedException(new DefaultErrorCode("500", "数据权限字段处理失败"), cause);
        }
    }
}
