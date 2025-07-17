package com.example.cloudnativeproject.controller.limit;

import java.lang.annotation.*;

/**
 * 请求限流注解
 * 用于标记需要进行限流控制的方法
 * 支持分布式限流，多个Pod实例共享限流策略
 *
 * 使用示例：
 * @RequestLimit(count=100, time=1000) // 每秒最多100次请求
 * public Object someMethod() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RequestLimit {
    /**
     * 时间窗口内允许的最大请求次数
     * 默认值为Integer.MAX_VALUE，表示不限制
     *
     * @return 允许的请求次数
     */
    int count() default Integer.MAX_VALUE;

    /**
     * 时间窗口大小，单位为毫秒
     * 默认值为1000毫秒（1秒）
     *
     * @return 时间窗口大小（毫秒）
     */
    long time() default 1000;
}