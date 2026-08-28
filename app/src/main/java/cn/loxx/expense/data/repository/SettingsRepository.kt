package cn.loxx.expense.data.repository

import android.content.Context

/**
 * Persists user profile and WebDAV configuration in plain SharedPreferences.
 *
 * Note: the WebDAV password is stored unencrypted on-device. A personal,
 * single-user expense app has no stronger secret to derive an encryption key
 * from, so this is a deliberate MVP tradeoff; encrypting would require a
 * passphrase prompt on every launch.
 */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("expense_settings", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString("userName", "") ?: ""
        set(value) = prefs.edit().putString("userName", value).apply()

    var department: String
        get() = prefs.getString("department", "") ?: ""
        set(value) = prefs.edit().putString("department", value).apply()

    var webdavUrl: String
        get() = prefs.getString("webdavUrl", "") ?: ""
        set(value) = prefs.edit().putString("webdavUrl", value).apply()

    var webdavUser: String
        get() = prefs.getString("webdavUser", "") ?: ""
        set(value) = prefs.edit().putString("webdavUser", value).apply()

    var webdavPass: String
        get() = prefs.getString("webdavPass", "") ?: ""
        set(value) = prefs.edit().putString("webdavPass", value).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean("appLockEnabled", false)
        set(value) = prefs.edit().putBoolean("appLockEnabled", value).apply()

    var autoBackupEnabled: Boolean
        get() = prefs.getBoolean("autoBackupEnabled", false)
        set(value) = prefs.edit().putBoolean("autoBackupEnabled", value).apply()

    var lastAutoBackupAt: Long
        get() = prefs.getLong("lastAutoBackupAt", 0L)
        set(value) = prefs.edit().putLong("lastAutoBackupAt", value).apply()

    var lastAutoBackupResult: String
        get() = prefs.getString("lastAutoBackupResult", "") ?: ""
        set(value) = prefs.edit().putString("lastAutoBackupResult", value).apply()
}
