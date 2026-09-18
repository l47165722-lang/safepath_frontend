package com.example.safepath_test1.wear.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearStatusTest {
    @Test
    fun missingTimestamp_isStale() {
        assertTrue(isStatusStale(updatedAt = 0L, now = 1_000L))
    }

    @Test
    fun recentTimestamp_isNotStale() {
        assertFalse(isStatusStale(updatedAt = 1_000L, now = 1_000L + STALE_AFTER_MS))
    }

    @Test
    fun oldTimestamp_isStale() {
        assertTrue(isStatusStale(updatedAt = 1_000L, now = 1_001L + STALE_AFTER_MS))
    }
}
