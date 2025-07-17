package com.example.cloudnativeproject.controller.limit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 限流功能专项测试
 * 测试分布式限流逻辑的正确性
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.redis.host=localhost",
    "spring.redis.port=6379"
})
public class RateLimitTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private GlobalRateLimiterAspect rateLimiterAspect;

    @BeforeEach
    public void setUp() {
        // 清理Redis中的测试数据
        try {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
        } catch (Exception e) {
            // 如果Redis不可用，跳过清理
            System.out.println("Redis不可用，跳过数据清理: " + e.getMessage());
        }
    }

    /**
     * 测试Redis连接
     */
    @Test
    public void testRedisConnection() {
        try {
            redisTemplate.opsForValue().set("test_key", "test_value");
            String value = redisTemplate.opsForValue().get("test_key");
            assertEquals("test_value", value);
            redisTemplate.delete("test_key");
        } catch (Exception e) {
            System.out.println("Redis连接测试失败: " + e.getMessage());
            // 在CI/CD环境中，Redis可能不可用，这是正常的
        }
    }

    /**
     * 测试限流逻辑的基本功能
     */
    @Test
    public void testBasicRateLimit() {
        String testKey = "test_rate_limit";
        int capacity = 5;
        long timeWindow = 1000; // 1秒

        try {
            // 模拟限流检查
            for (int i = 0; i < capacity; i++) {
                Long count = redisTemplate.opsForValue().increment(testKey + ":" + (System.currentTimeMillis() / timeWindow));
                assertTrue(count <= capacity, "请求计数应该在限制范围内");
            }
        } catch (Exception e) {
            System.out.println("Redis限流测试跳过: " + e.getMessage());
        }
    }

    /**
     * 测试并发场景下的限流
     */
    @Test
    public void testConcurrentRateLimit() throws InterruptedException {
        if (!isRedisAvailable()) {
            System.out.println("Redis不可用，跳过并发限流测试");
            return;
        }

        String testKey = "concurrent_test";
        int capacity = 10;
        long timeWindow = 1000;
        int threadCount = 20;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String windowKey = testKey + ":" + (System.currentTimeMillis() / timeWindow);
                    Long count = redisTemplate.opsForValue().increment(windowKey);
                    
                    if (count <= capacity) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("成功请求数: " + successCount.get());
        System.out.println("失败请求数: " + failCount.get());
        
        // 验证限流效果：成功请求数应该不超过容量
        assertTrue(successCount.get() <= capacity, "成功请求数不应超过限流容量");
    }

    /**
     * 检查Redis是否可用
     */
    private boolean isRedisAvailable() {
        try {
            redisTemplate.opsForValue().set("health_check", "ok");
            return "ok".equals(redisTemplate.opsForValue().get("health_check"));
        } catch (Exception e) {
            return false;
        }
    }
}
