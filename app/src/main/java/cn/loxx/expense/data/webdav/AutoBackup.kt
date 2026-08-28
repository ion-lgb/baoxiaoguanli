package cn.loxx.expense.data.webdav

/**
 * Daily auto-backup decision, pure and unit-testable. Runs once per process
 * launch when enabled and configured, and at most once per interval.
 */
object AutoBackup {
    const val INTERVAL_MS: Long = 24 * 60 * 60 * 1000

    fun shouldRun(
        now: Long,
        enabled: Boolean,
        serverConfigured: Boolean,
        lastRunAt: Long,
    ): Boolean =
        enabled &&
            serverConfigured &&
            (lastRunAt <= 0L || now - lastRunAt >= INTERVAL_MS)
}
