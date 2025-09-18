package com.UGcris.ALG;
import com.UGcris.ALG.ratelimite.FixedWindowRateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class FixedWindowRateLimiterTest {
    private FixedWindowRateLimiter rateLimiter;
    private static final String TEST_KEY = "testKey";
    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SIZE_MS = 1000; // 1 second

    @BeforeEach
    void setUp() {
        rateLimiter = new FixedWindowRateLimiter(MAX_REQUESTS, WINDOW_SIZE_MS);
    }

    @AfterEach
    void tearDown() {
        rateLimiter.shutdown();
    }

    @Test
    void allowRequest_withNullKey_shouldReturnFalse() {
        assertFalse(rateLimiter.allowRequest(null));
    }

    @Test
    void allowRequest_withEmptyKey_shouldReturnFalse() {
        assertFalse(rateLimiter.allowRequest(""));
    }

    @Test
    void allowRequest_withValidKey_shouldAllowRequestsWithinLimit() {
        for (int i = 0; i < MAX_REQUESTS; i++) {
            assertTrue(rateLimiter.allowRequest(TEST_KEY));
        }
    }

    @Test
    void allowRequest_withValidKey_shouldDenyRequestsBeyondLimit() {
        // Exhaust the limit
        for (int i = 0; i < MAX_REQUESTS; i++) {
            rateLimiter.allowRequest(TEST_KEY);
        }

        // Next request should be denied
        assertFalse(rateLimiter.allowRequest(TEST_KEY));
    }

    @Test
    void allowRequest_afterWindowReset_shouldAllowRequestsAgain() throws InterruptedException {
        // Exhaust the limit
        for (int i = 0; i < MAX_REQUESTS; i++) {
            rateLimiter.allowRequest(TEST_KEY);
        }

        // Wait for window reset
        TimeUnit.MILLISECONDS.sleep(WINDOW_SIZE_MS + 100);

        // Should allow requests again
        assertTrue(rateLimiter.allowRequest(TEST_KEY));
    }

    @Test
    void allowRequest_withDifferentKeys_shouldTrackSeparately() {
        String key1 = "key1";
        String key2 = "key2";

        // Exhaust limit for key1
        for (int i = 0; i < MAX_REQUESTS; i++) {
            assertTrue(rateLimiter.allowRequest(key1));
        }

        // Key2 should still have full quota
        assertTrue(rateLimiter.allowRequest(key2));
    }
}
