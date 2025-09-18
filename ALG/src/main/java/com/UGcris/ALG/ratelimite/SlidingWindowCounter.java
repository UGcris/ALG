package com.UGcris.ALG.ratelimite;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 滑动窗口限流器
 */
public class SlidingWindowCounter {
    private final ConcurrentSkipListSet<Long> timestamps;
    private final long windowSizeMillis;  // 窗口大小，如 60_000ms（1分钟）
    private final int limit;              // 最大请求数
    private final AtomicInteger currentCount = new AtomicInteger(0);

    public SlidingWindowCounter(long windowSizeMillis, int limit) {
        this.timestamps = new ConcurrentSkipListSet<>();
        this.windowSizeMillis = windowSizeMillis*1_000_000;//转纳秒
        this.limit = limit;
    }

    /**
     * 尝试通过限流检查
     * @return true 表示允许，false 表示被限流
     */

    public boolean tryAcquire() {
        long now = System.nanoTime();
        long windowStart = now - windowSizeMillis;

        // 清理旧时间戳并同步计数器
        timestamps.headSet(windowStart).clear();
        currentCount.set(timestamps.size()); // 同步实际数量

        // 原子化检查并递增
        while (true) {
            int current = currentCount.get();
            if (current >= limit) {
                return false;
            }
            if (currentCount.compareAndSet(current, current + 1)) {
                timestamps.add(now);
                return true;
            }
        }
    }

//    public boolean tryAcquire() {
//
//        long now = System.nanoTime();
//        long windowStart = now - windowSizeMillis;
//        // 1. 移除窗口外的旧时间戳
//        timestamps.headSet(windowStart).clear();
//
//        // 2. 检查当前请求数是否超限
//        if (timestamps.size() >= limit) {
//            return false;
//        }
//
//        // 3. 添加当前请求时间戳
//        timestamps.add(now);
//        System.err.println("当前时间: " + now + ", 窗口请求数: " + timestamps.size());
////        System.err.println("当前窗口内请求数: " + timestamps.size());
//        return true;
//    }

    /**
     * 获取当前窗口内的请求数
     */
    public int getCount() {
        long windowStart = System.nanoTime() - windowSizeMillis;
        int count = (int) timestamps.tailSet(windowStart).size();
        return count;
    }

    /**
     * 清空所有记录（测试用）
     */
    public void reset() {
        timestamps.clear();
    }
}
