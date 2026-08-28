package cn.loxx.expense.security

/**
 * App-lock policy, pure and unit-testable.
 *
 * Session semantics: a fresh process always starts locked; after unlocking,
 * a short background (incoming message, brief app switch) does not re-lock,
 * but being away longer than [RELOCK_AFTER_BACKGROUND_MS] does.
 */
object AppLock {
    const val RELOCK_AFTER_BACKGROUND_MS: Long = 2 * 60 * 1000

    fun shouldLock(
        enabled: Boolean,
        unlockedThisSession: Boolean,
        lastBackgroundedAt: Long,
        now: Long,
    ): Boolean = when {
        !enabled -> false
        !unlockedThisSession -> true
        lastBackgroundedAt <= 0L -> false
        else -> now - lastBackgroundedAt >= RELOCK_AFTER_BACKGROUND_MS
    }
}

/** In-process lock state; process death intentionally resets it (cold start re-locks). */
object AppLockSession {
    @Volatile var unlocked: Boolean = false
    @Volatile var lastBackgroundedAt: Long = 0L
}
