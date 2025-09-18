package com.UGcris.ALG;

import com.UGcris.ALG.ratelimite.LeakyBucketAlgorithm;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;


public class LeakyBucketAlgorithmTest {
    private LeakyBucketAlgorithm leakyBucket;
    private final int CAPACITY = 10;
    private final  int LEAK_RATE_PER_MS = 1; // 每秒漏100个

    @Before
    void setUp() {
        leakyBucket = new LeakyBucketAlgorithm(CAPACITY, LEAK_RATE_PER_MS);
    }

    @Test
    void testAllow_whenBucketNotFull_shouldReturnTrue() {
        assertTrue(leakyBucket.allow());
    }

    @Test
    void testAllow_whenBucketFull_shouldReturnFalse() {
        // Fill the bucket to capacity
        for (int i = 0; i < CAPACITY; i++) {
            leakyBucket.allow();
        }
        assertFalse(leakyBucket.allow());
    }

    @Test
    void testAllow_afterLeakage_shouldAllowMoreRequests() throws InterruptedException {
        // Fill the bucket
        for (int i = 0; i < CAPACITY; i++) {
            leakyBucket.allow();
        }

        // Wait for some water to leak
        Thread.sleep(100); // Should leak 10 units (0.1 * 100ms)

        // Should now allow at least one more request
        assertTrue(leakyBucket.allow());
    }

    @Test
    void testAllowRequestWhenTokensAvailable() {
        assertTrue(leakyBucket.allow(5)); // 初始有10个令牌，消耗5个
    }

    @Test
    void testDenyRequestWhenTokensInsufficient() {
        assertFalse(leakyBucket.allow(15)); // 请求超过容量
    }

    @Test
    void testTokenRefillOverTime() throws InterruptedException {
        leakyBucket.allow(10); // 清空令牌桶
        Thread.sleep(10000); // 等待2秒
        assertTrue(leakyBucket.allow(2)); // 应补充2个令牌
    }

    @Test
    void testEdgeCaseZeroTokens() {
        assertTrue(leakyBucket.allow(10)); // 刚好用完
        assertFalse(leakyBucket.allow(1)); // 立即请求应失败
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {

        // 使用CountDownLatch确保并发执行
        CountDownLatch latch = new CountDownLatch(2);

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 提交两个并发任务
        executor.submit(() -> {
            assertTrue(leakyBucket.allow(5));
            latch.countDown();
        });

        executor.submit(() -> {
            assertTrue(leakyBucket.allow(5));
            latch.countDown();
        });

        // 等待所有线程完成
        latch.await(1, TimeUnit.SECONDS);

        // 关闭线程池
        executor.shutdown();
    }

    @Test
    void testConcurrentAccessExceedingCapacity() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // 三个线程同时请求，总请求量15超过容量10
        executor.submit(() -> {
            assertTrue(leakyBucket.allow(5));
            latch.countDown();
        });

        executor.submit(() -> {
            assertTrue(leakyBucket.allow(5));
            latch.countDown();
        });

        executor.submit(() -> {
            assertFalse(leakyBucket.allow(5));  // 应该有一个请求失败
            latch.countDown();
        });

        latch.await(1, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Test
    void testConcurrentAccessWithRefill() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        ExecutorService executor = Executors.newFixedThreadPool(4);

        // 初始两个请求
        executor.submit(() -> {
            assertTrue(leakyBucket.allow(5));
            latch.countDown();
        });

        executor.submit(() -> {
            assertTrue(leakyBucket.allow(5));
            latch.countDown();
        });

        // 等待补充令牌
        Thread.sleep(1000);

        // 补充后两个请求
        executor.submit(() -> {
            assertTrue(leakyBucket.allow(5));
            latch.countDown();
        });

        executor.submit(() -> {
            assertTrue(leakyBucket.allow(5));
            latch.countDown();
        });

        latch.await(2, TimeUnit.SECONDS);
        executor.shutdown();
    }
}