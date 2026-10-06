package com.th4.edge.components.lock;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/** @DistributedLocked 切面：锁在事务外层获取，finally 释放。 */
@Note("分布式锁切面")
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DistributedLockAspect {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final DistributedLock distributedLock;

    public DistributedLockAspect(DistributedLock distributedLock) {
        this.distributedLock = distributedLock;
    }

    @Around("@annotation(com.th4.edge.components.lock.DistributedLocked)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        DistributedLocked locked = method.getAnnotation(DistributedLocked.class);
        if (locked == null) {
            return joinPoint.proceed();
        }
        String key = resolveKey(joinPoint, locked.key());
        if (!distributedLock.tryLock(key, locked.waitMillis(), locked.leaseMillis())) {
            throw new DefinedException(new DefaultErrorCode("409", locked.message()));
        }
        try {
            return joinPoint.proceed();
        } finally {
            distributedLock.unlock(key);
        }
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, String expression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        StandardEvaluationContext context = new StandardEvaluationContext(joinPoint.getTarget());
        String[] names = new DefaultParameterNameDiscoverer().getParameterNames(method);
        Object[] args = joinPoint.getArgs();
        if (names != null) {
            for (int i = 0; i < names.length && i < args.length; i++) {
                context.setVariable(names[i], args[i]);
            }
        }
        Object value = PARSER.parseExpression(expression).getValue(context);
        String key = value == null ? null : value.toString();
        if (key == null || key.isBlank()) {
            throw new LockException("分布式锁 key 为空: " + expression);
        }
        return key;
    }
}
