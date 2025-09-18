package com.UGcris.ALG.ratelimite;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/*
 * 令牌桶算法
 */
public class TokenBucketAlgorithm {
    // 修改日志配置
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%3$s] %1$tH:%1$tM:%1$tS %5$s%6$s%n");
    }
    Logger logger = Logger.getLogger(TokenBucketAlgorithm.class.getName());

    private final double refillTokensPerSecond; // 每秒补充的令牌数（速率）
    private final int capacity;                 // 桶的最大容量
    private final AtomicReference<Bucket> bucketRef;// 桶的状态引用

    // 内部类：表示桶的状态
    private static class Bucket {
        final int tokens;           // 当前令牌数量
        final long lastRefillTime;  // 上次补充令牌的时间（纳秒）

        Bucket(int tokens, long lastRefillTime) {
            this.tokens = tokens;
            this.lastRefillTime = lastRefillTime;
        }
    }

    public TokenBucketAlgorithm(double refillTokensPerSecond, int capacity) {
        this.refillTokensPerSecond = refillTokensPerSecond;
        this.capacity = capacity;
        this.bucketRef = new AtomicReference<>(
                new Bucket(capacity, System.nanoTime()) // 初始时桶是满的
        );
    }

    /**
     * 尝试获取一个令牌
     * @return true 表示获取成功（允许请求），false 表示被限流
     */
    public boolean allow() {
        return tryConsume(1);
    }

    /**
     * 尝试获取指定数量的令牌
     * @param numTokens 要获取的令牌数
     * @return true 表示成功，false 表示令牌不足
     */
    public boolean allow(int numTokens) {
        if (numTokens <= 0) return true;
        return tryConsume(numTokens);
    }

    // 尝试消耗令牌
    private boolean tryConsume(int numTokens) {
        while (true) {
            Bucket oldBucket = bucketRef.get();
            Bucket newBucket = refillAndConsume(oldBucket, numTokens);
            logger.info("numTokens: " + numTokens);
            logger.info("oldBucket.tokens: " + oldBucket.tokens);
            logger.info("newBucket.tokens: " + newBucket.tokens);
            if (bucketRef.compareAndSet(oldBucket, newBucket)) {
                return newBucket.tokens >= 0;
            }
        }
    }

    // 补充令牌并尝试消耗
    private Bucket refillAndConsume(Bucket oldBucket, int numTokens) {
        long now = System.nanoTime();
        long elapsedTimeNanos = now - oldBucket.lastRefillTime;// 计算自上次补充令牌以来的时间（纳秒）
        double elapsedTimeSecs = elapsedTimeNanos / 1_000_000_000.0;// 转换为秒
//        logger.info("elapsedTimeSecs: " + elapsedTimeSecs);
        // 计算应补充的令牌数
        int refillTokens = (int) (elapsedTimeSecs * refillTokensPerSecond);
        int updatedTokens = Math.min(oldBucket.tokens + refillTokens, capacity);
//        logger.info("oldBucket.tokens: " + oldBucket.tokens);
//        logger.info("refillTokens: " + refillTokens);
//        logger.info("updatedTokens: " + updatedTokens);
        // 尝试消费 numTokens 个令牌
        if (updatedTokens >= numTokens) {
            return new Bucket(updatedTokens - numTokens, now);
        } else {
            // 令牌不足，返回当前状态（不更新时间，避免空转影响下次计算）
            return new Bucket(oldBucket.tokens, oldBucket.lastRefillTime);
        }
    }

    // 可选：获取当前剩余令牌数（用于监控）
    public int getAvailableTokens() {
        Bucket bucket = bucketRef.get();
        long now = System.nanoTime();
        long elapsedTimeNanos = now - bucket.lastRefillTime;
        double elapsedTimeSecs = elapsedTimeNanos / 1_000_000_000.0;
        int refillTokens = (int) (elapsedTimeSecs * refillTokensPerSecond);
        return Math.min(bucket.tokens + refillTokens, capacity);
    }
}
