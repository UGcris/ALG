package com.UGcris.ALG.ratelimite;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
/**
 * 漏桶算法
 */
public class LeakyBucketAlgorithm {
    private final int capacity;           // 桶的容量（最多容纳多少请求）
    private final int leakRatePerMs;      // 每毫秒“漏水”（处理）的请求数
    private final AtomicReference<Bucket> bucketRef;// 桶的状态，包含当前水量和上次漏水时间

    private static class Bucket {
        final int water;           // 当前水量（请求数）
        final long lastLeakTime;   // 上次漏水时间（毫秒）

        Bucket(int water, long lastLeakTime) {
            this.water = water;
            this.lastLeakTime = lastLeakTime;
        }
    }

    public LeakyBucketAlgorithm(int capacity, int leakRatePerSecond) {
        this.capacity = capacity;
        this.leakRatePerMs = leakRatePerSecond / 1000;
        this.bucketRef = new AtomicReference<>(new Bucket(0, System.currentTimeMillis()));
    }

    /**
     * 尝试添加一个请求（加水）
     * @return true 表示成功加入桶中（允许），false 表示桶满被拒绝
     */
    public boolean allow() {
        long now = System.currentTimeMillis();
        while (true) {
            Bucket oldBucket = bucketRef.get();
            Bucket newBucket = leakAndAdd(oldBucket, now, 1);
            if (bucketRef.compareAndSet(oldBucket, newBucket)) {
                return newBucket.water <= capacity;
            }
        }
    }
    public boolean allow(int request) {
        long now = System.currentTimeMillis();
        while (true) {
            Bucket oldBucket = bucketRef.get();
            Bucket newBucket = leakAndAdd(oldBucket, now, request);
            if (bucketRef.compareAndSet(oldBucket, newBucket)) {
                return newBucket.water <= capacity;
            }
        }
    }

    private Bucket leakAndAdd(Bucket oldBucket, long now, int request) {
        // 计算从上次漏水到现在应该漏掉多少水
        long elapsedMs = now - oldBucket.lastLeakTime;
        int leakedWater = (int) (elapsedMs * leakRatePerMs);
        int currentWater = Math.max(0, oldBucket.water - leakedWater);

        // 更新时间
        long newTime = now;

        // 尝试加入新请求
        if (currentWater + request <= capacity) {
            return new Bucket(currentWater + request, newTime);
        } else {
            // 桶满，拒绝请求，但状态不变
            return oldBucket;
        }
    }

    // 获取当前水量（用于监控）
    public int getCurrentWater() {
        Bucket bucket = bucketRef.get();
        long now = System.currentTimeMillis();
        long elapsedMs = now - bucket.lastLeakTime;
        int leakedWater = (int) (elapsedMs * leakRatePerMs);
        return Math.max(0, bucket.water - leakedWater);
    }
}