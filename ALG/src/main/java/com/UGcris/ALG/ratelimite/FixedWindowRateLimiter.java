package com.UGcris.ALG.ratelimite;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * 固定窗口限流器
 */
public class FixedWindowRateLimiter {

    // 存储每个 key 的计数器
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    // 窗口大小（毫秒），例如 60_000 = 1分钟
    private final long windowSizeMs;
    private final int maxRequests;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public FixedWindowRateLimiter(int maxRequests, long windowSizeMs) {
        this.maxRequests = maxRequests;
        this.windowSizeMs = windowSizeMs;

        // 每隔 windowSizeMs 时间重置一次计数器
        this.scheduler.scheduleAtFixedRate(
                this::resetCounters,
                windowSizeMs, windowSizeMs, TimeUnit.MILLISECONDS
        );
    }

    /**
     * 检查是否允许请求
     * @param key 限流的标识（如 IP、邮箱）
     * @return 是否允许
     */
    public synchronized boolean allowRequest(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        // 获取当前 key 的计数器，不存在则创建
        AtomicInteger counter = counters.computeIfAbsent(key, k -> new AtomicInteger(0));
        int count = counter.get();

        if (count >= maxRequests) {
            return false; // 超过限制
        }

        // 原子递增
        counter.incrementAndGet();
        return true;
    }

    /**
     * 重置所有计数器（每窗口周期执行一次）
     */
    private void resetCounters() {
        System.out.println("🔄 重置固定窗口计数器...");
        counters.clear();
    }

    /**
     * 关闭调度器（应用关闭时调用）
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}