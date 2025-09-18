package com.UGcris.ALG;
import com.UGcris.ALG.ratelimite.SlidingWindowLogLimiter;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SlidingWindowLogLimiterTest {
    private SlidingWindowLogLimiter limiter;
    private final long WINDOW_SIZE_MILLIS = 1000; // 1秒窗口（便于测试）
    private final int LIMIT = 5; // 最多3次

    @Before
    public void setUp() {
        // 窗口大小：1000ms（1秒），最多允许 5 次
        limiter = new SlidingWindowLogLimiter(WINDOW_SIZE_MILLIS, LIMIT);
    }

    @Test
    public void testWithinLimit_Allowed() {
        // 在 1 秒内发送 5 次，应该全部允许
        for (int i = 0; i < LIMIT; i++) {
            assertTrue("第 " + (i + 1) + " 次请求应被允许", limiter.tryLog("test-ip"));
        }
        assertEquals(LIMIT, limiter.getRequestCount("test-ip"));
    }

    @Test
    public void testExceedLimit_Blocked() {
        // 前 5 次允许
        for (int i = 0; i < LIMIT; i++) {
            assertTrue(limiter.tryLog("test-ip"));
        }

        // 第 6 次应被拒绝
        assertFalse("超过限制的请求应被拒绝", limiter.tryLog("test-ip"));
    }

    @Test
    public void testSlidingWindow_TimePasses_AllowedAgain() throws InterruptedException {
        // 发送 5 次
        for (int i = 0; i < LIMIT; i++) {
            assertTrue(limiter.tryLog("user1"));
        }

        // 此时第 6 次会被拒绝
        assertFalse(limiter.tryLog("user1"));

        // 等待 1.1 秒，窗口滑动
        Thread.sleep(1100);

        // 再次尝试，应该允许（旧请求已过期）
        assertTrue("窗口滑动后应允许新请求", limiter.tryLog("user1"));
    }

    @Test
    public void testMultipleKeys_Independent() {
        // 不同 key 应独立计数
        for (int i = 0; i < LIMIT; i++) {
            assertTrue(limiter.tryLog("ip1"));
            assertTrue(limiter.tryLog("ip2"));
        }

        // ip1 和 ip2 都达到上限
        assertFalse(limiter.tryLog("ip1"));
        assertFalse(limiter.tryLog("ip2"));
    }

    @Test
    public void testHighConcurrency_MultipleThreads() throws InterruptedException {
        int threadCount = 1000;
        int requestsPerThread = 10;
        String key = "concurrent-ip";

        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger allowedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 模拟高并发场景
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (limiter.tryLog(key)) {
                            allowedCount.incrementAndGet();
                        }
                        // 轻微延迟，模拟真实请求间隔
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await();

        // 关闭线程池
        executor.shutdown();

        // 由于窗口是 1 秒，最多允许 5 次
        // 即使并发 100 次请求（10线程×10次），也只应允许 5 次左右（取决于执行时间）
        // 但因为我们加了 10ms 延迟，总时间 > 1s，所以可能允许略多于 5 次
        // 保守判断：允许数应在 5~15 之间（防止误判）
        int count = limiter.getRequestCount(key);
        assertTrue("并发场景下请求数应在合理范围内", count >= LIMIT && count <= 15);
    }

    @Test
    public void testRapidRequestsInOneSecond() throws InterruptedException {
        String key = "rapid-ip";
        int allowed = 0;

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 950) { // 在 1 秒内快速请求
            if (limiter.tryLog(key)) {
                allowed++;
            }
            // 极小延迟，模拟高频请求
            Thread.sleep(1);
        }

        // 应接近 5 次（可能 4~6 次，因时间精度）
        assertTrue("1秒内应接近 maxRequests 次", allowed >= 4 && allowed <= 6);
    }
}
