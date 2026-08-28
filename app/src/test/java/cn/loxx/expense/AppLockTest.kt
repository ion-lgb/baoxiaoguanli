package cn.loxx.expense

import cn.loxx.expense.security.AppLock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {
    @Test
    fun disabled_neverLocks() {
        assertFalse(
            AppLock.shouldLock(
                enabled = false,
                unlockedThisSession = false,
                lastBackgroundedAt = 0L,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun enabled_coldStartLocks() {
        assertTrue(
            AppLock.shouldLock(
                enabled = true,
                unlockedThisSession = false,
                lastBackgroundedAt = 0L,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun unlocked_shortBackgroundDoesNotRelock() {
        val now = 10 * 60 * 1000L
        assertFalse(
            AppLock.shouldLock(
                enabled = true,
                unlockedThisSession = true,
                lastBackgroundedAt = now - 30_000L,
                now = now,
            ),
        )
    }

    @Test
    fun unlocked_longBackgroundRelocks() {
        val now = 60 * 60 * 1000L
        assertTrue(
            AppLock.shouldLock(
                enabled = true,
                unlockedThisSession = true,
                lastBackgroundedAt = now - AppLock.RELOCK_AFTER_BACKGROUND_MS - 1,
                now = now,
            ),
        )
    }
}
