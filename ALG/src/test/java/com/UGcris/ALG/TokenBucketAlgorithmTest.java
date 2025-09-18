package com.UGcris.ALG;

import com.UGcris.ALG.ratelimite.TokenBucketAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketAlgorithmTest {
    private TokenBucketAlgorithm tokenBucket;

    @BeforeEach
    void setUp() {
        // 假设构造函数接收容量和填充速率参数
        tokenBucket = new TokenBucketAlgorithm(5, 10); // 容量=10, 每秒填充1个令牌
    }

    @Test
    void testAllowRequestWhenTokensAvailable() {
        assertTrue(tokenBucket.allow(5)); // 初始有10个令牌，消耗5个
    }

    @Test
    void testDenyRequestWhenTokensInsufficient() {
        assertFalse(tokenBucket.allow(15)); // 请求超过容量
    }

    @Test
    void testTokenRefillOverTime() throws InterruptedException {
        tokenBucket.allow(10); // 清空令牌桶
        Thread.sleep(10000); // 等待2秒
        assertTrue(tokenBucket.allow(2)); // 应补充2个令牌
    }

    @Test
    void testEdgeCaseZeroTokens() {
        assertTrue(tokenBucket.allow(10)); // 刚好用完
        assertFalse(tokenBucket.allow(1)); // 立即请求应失败
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {

        // 使用CountDownLatch确保并发执行
        CountDownLatch latch = new CountDownLatch(2);

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 提交两个并发任务
        executor.submit(() -> {
            assertTrue(tokenBucket.allow(5));
            latch.countDown();
        });

        executor.submit(() -> {
            assertTrue(tokenBucket.allow(5));
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
            assertTrue(tokenBucket.allow(5));
            latch.countDown();
        });

        executor.submit(() -> {
            assertTrue(tokenBucket.allow(5));
            latch.countDown();
        });

        executor.submit(() -> {
            assertFalse(tokenBucket.allow(5));  // 应该有一个请求失败
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
            assertTrue(tokenBucket.allow(5));
            latch.countDown();
        });

        executor.submit(() -> {
            assertTrue(tokenBucket.allow(5));
            latch.countDown();
        });

        // 等待补充令牌
        Thread.sleep(1000);

        // 补充后两个请求
        executor.submit(() -> {
            assertTrue(tokenBucket.allow(5));
            latch.countDown();
        });

        executor.submit(() -> {
            assertTrue(tokenBucket.allow(5));
            latch.countDown();
        });

        latch.await(2, TimeUnit.SECONDS);
        executor.shutdown();
    }
}