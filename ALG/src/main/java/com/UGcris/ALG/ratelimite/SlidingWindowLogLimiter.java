package com.UGcris.ALG.ratelimite;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 滑动窗口日志限流器
 * 用于限制每个 key（如 ip、email、userId）在指定时间窗口内的最大请求数
 */
public class SlidingWindowLogLimiter {
    private final long windowSizeMillis;   // 窗口大小，如 60_000 ms（1分钟）
    private final int maxRequests;         // 窗口内最大请求数
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestWindows;

    public SlidingWindowLogLimiter(long windowSizeMillis, int maxRequests) {
        this.windowSizeMillis = windowSizeMillis*1_000_000;//转纳秒
        this.maxRequests = maxRequests;
        this.requestWindows = new ConcurrentHashMap<>();
    }

    /**
     * 尝试记录一次日志操作
     * @param key 限流键（如：ip、email、userId）
     * @return 是否允许（true = 允许，false = 被限流）
     */
    public boolean tryLog(String key) {
        long now = System.nanoTime();
        Queue<Long> window = requestWindows.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        // 1. 清理过期请求
        while (!window.isEmpty() && window.peek() < now - windowSizeMillis) {
            window.poll();
        }

        // 2. 检查当前窗口内请求数是否超限
        if (window.size() >= maxRequests) {
            return false;
        }

        // 3. 添加当前请求时间戳
        window.offer(now);
        return true;
    }

    /**
     * 获取当前 key 的请求数（用于监控）
     */
    public int getRequestCount(String key) {
        Queue<Long> window = requestWindows.get(key);
        if (window == null) return 0;

        long now = System.nanoTime();
        // 临时清理过期数据并计数
        window.removeIf(timestamp -> timestamp < now - windowSizeMillis);
        return window.size();
    }

    /**
     * 手动重置某个 key（测试用）
     */
    public void reset(String key) {
        requestWindows.remove(key);
    }
}
