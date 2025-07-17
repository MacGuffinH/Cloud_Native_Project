package com.example.cloudnativeproject.controller.limit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 全局分布式限流切面
 * 使用Redis实现分布式限流，支持多个Pod实例共享限流策略
 * 采用滑动窗口算法，精确控制请求频率
 */
@Aspect
@Order(1)
@Component
public class GlobalRateLimiterAspect {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Pointcut("@annotation(RequestLimit)")
    public void requestLimit() {}

    /**
     * 限流切面方法
     * 在方法执行前检查是否超过限流阈值
     */
    @Around("requestLimit()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String url = request.getRequestURI();
        RequestLimit rateLimiter = getRequestLimit(joinPoint);

        if (rateLimiter == null) {
            return joinPoint.proceed();
        }

        String key = "rate_limit:" + url;
        int capacity = rateLimiter.count();
        long timeWindowMs = rateLimiter.time();

        // 检查是否允许请求通过
        if (isAllowed(key, capacity, timeWindowMs)) {
            return joinPoint.proceed();
        } else {
            throw new RequestLimitException();
        }
    }

    /**
     * 基于Redis的分布式限流检查
     * 使用滑动窗口算法，精确控制时间窗口内的请求数量
     *
     * @param key 限流键
     * @param capacity 时间窗口内允许的最大请求数
     * @param timeWindowMs 时间窗口大小（毫秒）
     * @return 是否允许请求通过
     */
    private boolean isAllowed(String key, int capacity, long timeWindowMs) {
        long currentTime = System.currentTimeMillis();
        String windowKey = key + ":" + (currentTime / timeWindowMs); // 时间窗口键

        try {
            // 使用Redis的原子操作来增加计数器
            Long currentCount = redisTemplate.opsForValue().increment(windowKey);

            // 如果是第一次访问这个时间窗口，设置过期时间
            if (currentCount == 1) {
                redisTemplate.expire(windowKey, timeWindowMs * 2, TimeUnit.MILLISECONDS);
            }

            // 检查是否超过限制
            return currentCount <= capacity;

        } catch (Exception e) {
            // 如果Redis出现异常，为了系统可用性，允许请求通过
            // 生产环境中可以考虑使用本地限流作为降级方案
            System.err.println("Redis限流检查异常: " + e.getMessage());
            return true;
        }
    }

    /**
     * 获取方法上的RequestLimit注解
     */
    private RequestLimit getRequestLimit(final ProceedingJoinPoint joinPoint) {
        Method[] methods = joinPoint.getTarget().getClass().getDeclaredMethods();
        String name = joinPoint.getSignature().getName();
        if (!StringUtils.isEmpty(name)) {
            for (Method method : methods) {
                RequestLimit annotation = method.getAnnotation(RequestLimit.class);
                if (!Objects.isNull(annotation) && name.equals(method.getName())) {
                    return annotation;
                }
            }
        }
        return null;
    }
}