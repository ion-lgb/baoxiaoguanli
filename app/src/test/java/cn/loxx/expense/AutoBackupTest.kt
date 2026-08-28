package cn.loxx.expense

import cn.loxx.expense.data.webdav.AutoBackup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupTest {
    private val day = AutoBackup.INTERVAL_MS

    @Test
    fun requiresEnabledAndConfigured() {
        val now = 100_000L
        assertFalse(AutoBackup.shouldRun(now, enabled = false, serverConfigured = true, lastRunAt = 0))
        assertFalse(AutoBackup.shouldRun(now, enabled = true, serverConfigured = false, lastRunAt = 0))
    }

    @Test
    fun firstRunHappensImmediately() {
        assertTrue(AutoBackup.shouldRun(100_000L, enabled = true, serverConfigured = true, lastRunAt = 0))
    }

    @Test
    fun skipsWithinInterval_runsAfterInterval() {
        val last = 1_000_000L
        assertFalse(
            AutoBackup.shouldRun(last + day - 1, enabled = true, serverConfigured = true, lastRunAt = last),
        )
        assertTrue(
            AutoBackup.shouldRun(last + day, enabled = true, serverConfigured = true, lastRunAt = last),
        )
    }
}
