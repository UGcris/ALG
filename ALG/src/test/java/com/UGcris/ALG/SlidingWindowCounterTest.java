package com.UGcris.ALG;

import com.UGcris.ALG.ratelimite.SlidingWindowCounter;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SlidingWindowCounterTest {
    private SlidingWindowCounter counter;
    private final long WINDOW_SIZE_MILLIS = 1000; // 1秒窗口（便于测试）
    private final int LIMIT = 3; // 最多3次

    @Before
    public void setUp() {
        counter = new SlidingWindowCounter(WINDOW_SIZE_MILLIS, LIMIT);
    }

    /**
     * 测试：在窗口内发送 LIMIT 次请求，应全部通过
     */
    @Test
    public void testWithinLimit_Allowed() {
        for (int i = 0; i < LIMIT; i++) {
            assertTrue("第 " + (i+1) + " 次请求应被允许", counter.tryAcquire());
        }
        assertEquals(LIMIT, counter.getCount());
    }

    /**
     * 测试：超过限流次数，后续请求应被拒绝
     */
    @Test
    public void testExceedLimit_Rejected() {
        // 发送 LIMIT + 2 次请求
        for (int i = 0; i < LIMIT + 2; i++) {
            boolean allowed = counter.tryAcquire();
            if (i < LIMIT) {
                assertTrue("前 " + LIMIT + " 次请求应被允许", allowed);
            } else {
                assertFalse("第 " + (i+1) + " 次请求应被拒绝", allowed);
            }
        }
        assertEquals(LIMIT, counter.getCount()); // 仍为3
    }

    /**
     * 测试：时间窗口滑动，旧请求应被清理
     * 先发3次，等待窗口过期，再发3次 → 应全部通过
     */
    @Test
    public void testSlidingWindow_OldRequestsEvicted() throws InterruptedException {
        // 第一波：3次请求（占满窗口）
        for (int i = 0; i < LIMIT; i++) {
            assertTrue(counter.tryAcquire());
        }
        assertEquals(LIMIT, counter.getCount());

        // 等待窗口过期（大于1秒）
        Thread.sleep(WINDOW_SIZE_MILLIS + 100);

        // 第二波：再发3次
        for (int i = 0; i < LIMIT; i++) {
            assertTrue("窗口已滑动，应允许新请求", counter.tryAcquire());
        }
        assertEquals(LIMIT, counter.getCount());
    }

    /**
     * 测试：窗口内部分请求过期，剩余容量可被使用
     * 发2次 → 等待500ms → 再发2次 → 应成功（总4次，但第1次已过期？不，窗口是1秒，要看时间）
     * 更精确：发3次 → 等500ms → 此时仍在窗口内 → 第4次应拒绝
     */
    @Test
    public void testPartialEviction() throws InterruptedException {
        // t0: 发3次
        for (int i = 0; i < LIMIT; i++) {
            assertTrue(counter.tryAcquire());
        }
        assertEquals(LIMIT, counter.getCount());

        // t+500ms: 窗口还剩500ms，此时第4次请求仍应拒绝
        Thread.sleep(500);
        assertFalse("窗口未滑出，第4次应被拒绝", counter.tryAcquire());

        // t+1100ms: 窗口已完全滑出，应可重新开始
        Thread.sleep(600); // 总共等待1100ms
        assertTrue("窗口已滑出，应允许新请求", counter.tryAcquire());
        assertEquals(1, counter.getCount());
    }

    /**
     * 测试：多线程并发请求，应线程安全
     */
    @Test
    public void testConcurrentRequests() throws InterruptedException {
        int size=1000;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger allowedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(size); // 20个并发请求

        for (int i = 0; i < size; i++) {
            executor.submit(() -> {
                try {
                    if (counter.tryAcquire()) {
                        allowedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 等待所有任务完成
        executor.shutdown();

        // 由于限流为3，最多允许3次
        assertTrue("并发请求最多允许 " + LIMIT + " 次", allowedCount.get() <= LIMIT);
        assertTrue("至少允许1次", allowedCount.get() >= 1);
        assertEquals("计数器应等于实际通过数", allowedCount.get(), counter.getCount());
    }

    /**
     * 测试：reset() 方法清空所有记录
     */
    @Test
    public void testReset() {
        for (int i = 0; i < 2; i++) {
            counter.tryAcquire();
        }
        assertEquals(2, counter.getCount());

        counter.reset();
        assertEquals(0, counter.getCount());
        assertTrue(counter.tryAcquire()); // 重置后应可重新请求
    }

    /**
     * 测试：getCount() 返回当前窗口内请求数
     */
    @Test
    public void testGetCount() {
        assertTrue(counter.tryAcquire());
        assertEquals(1, counter.getCount());

        assertTrue(counter.tryAcquire());
        assertEquals(2, counter.getCount());

        counter.reset();
        assertEquals(0, counter.getCount());
    }
}
